import { ConflictError, encodeFields } from './firestore.mjs'

/**
 * The points economy. Every grant and spend happens here, on the server; the app only reads its wallet. Admins tune
 * the numbers in the Firestore document `config/economy` (Firebase console); anything missing or invalid there falls
 * back to [DEFAULT_CONFIG]. Mirrored in the app by `EconomyConfig.Default`.
 */
export const DEFAULT_CONFIG = Object.freeze({
  enabled: true,
  startingBalance: 60,
  streakRewards: [20, 25, 30, 40, 50, 60, 100],
  defaultLiveCost: 30,
  liveCosts: { aurora: 30, noir: 50 },
  adReward: 25,
  adsPerDay: 5,
  // AdMob rewarded ad unit ids allowed to credit points (`ca-app-pub-…/123`); empty accepts any of this app's units.
  adUnits: [],
})

const MAX_POINTS = 100_000
const MAX_STREAK_DAYS = 31
/** A second claim needs a new local day and at least this gap, so changing the phone's time zone gains little. */
export const MIN_CLAIM_GAP_MS = 6 * 3_600_000
const DAY_MS = 86_400_000

const points = (v, fallback) => (Number.isInteger(v) && v >= 0 && v <= MAX_POINTS ? v : fallback)

/** Validates the admin document field by field: one bad value never disables the rest. */
export function parseConfig(raw) {
  const d = DEFAULT_CONFIG
  const r = raw && typeof raw === 'object' ? raw : {}
  const rewards = Array.isArray(r.streakRewards) && r.streakRewards.length >= 1 && r.streakRewards.length <= MAX_STREAK_DAYS
    && r.streakRewards.every((v) => points(v, -1) >= 0) ? r.streakRewards : d.streakRewards
  const costs = {}
  for (const [id, v] of Object.entries(r.liveCosts && typeof r.liveCosts === 'object' ? r.liveCosts : d.liveCosts)) {
    if (/^[a-z0-9_-]{1,32}$/.test(id) && points(v, -1) >= 0) costs[id] = v
  }
  return {
    enabled: typeof r.enabled === 'boolean' ? r.enabled : d.enabled,
    startingBalance: points(r.startingBalance, d.startingBalance),
    streakRewards: [...rewards],
    defaultLiveCost: points(r.defaultLiveCost, d.defaultLiveCost),
    liveCosts: costs,
    adReward: points(r.adReward, d.adReward),
    adsPerDay: Number.isInteger(r.adsPerDay) && r.adsPerDay >= 0 && r.adsPerDay <= 100 ? r.adsPerDay : d.adsPerDay,
    adUnits: Array.isArray(r.adUnits) ? r.adUnits.filter((u) => typeof u === 'string' && u.length <= 80) : [...d.adUnits],
  }
}

export const liveCost = (config, styleId) => config.liveCosts[styleId] ?? config.defaultLiveCost

/** The user's calendar day, from the UTC offset the phone reports (clamped to real time zones). */
export function localDay(nowMs, offsetMinutes) {
  const offset = Number.isFinite(offsetMinutes) ? Math.min(Math.max(Math.round(offsetMinutes), -720), 840) : 0
  return Math.floor((nowMs + offset * 60_000) / DAY_MS)
}

export function newWallet(config, nowMs) {
  return { balance: config.startingBalance, streak: 0, lastClaimDay: -1, lastClaimAtMs: 0, adsDay: -1, adsToday: 0, createdAtMs: nowMs, updatedAtMs: nowMs }
}

/** Fills fields an older or hand-edited wallet document lacks. */
export function normalizeWallet(data, config, nowMs) {
  const fresh = newWallet(config, nowMs)
  const out = { ...fresh }
  for (const key of Object.keys(fresh)) if (Number.isInteger(data?.[key])) out[key] = data[key]
  out.balance = Math.max(0, out.balance)
  return out
}

/** The daily check-in. Returns `{ claimed: false }` when today was already claimed. */
export function planCheckIn(wallet, config, nowMs, offsetMinutes) {
  const today = localDay(nowMs, offsetMinutes)
  if (today <= wallet.lastClaimDay || nowMs - wallet.lastClaimAtMs < MIN_CLAIM_GAP_MS) return { claimed: false, wallet }
  const streak = wallet.lastClaimDay === today - 1 ? wallet.streak + 1 : 1
  const reward = config.streakRewards[(streak - 1) % config.streakRewards.length]
  return {
    claimed: true,
    reward,
    day: today,
    wallet: { ...wallet, balance: Math.min(wallet.balance + reward, MAX_POINTS), streak, lastClaimDay: today, lastClaimAtMs: nowMs, updatedAtMs: nowMs },
  }
}

/** Entering a live. Fails without changing anything when the balance is short. */
export function planSpend(wallet, cost, nowMs) {
  if (wallet.balance < cost) return { ok: false, wallet }
  return { ok: true, wallet: { ...wallet, balance: wallet.balance - cost, updatedAtMs: nowMs } }
}

/** A verified rewarded ad. Over the daily cap it is recorded but credits nothing. */
export function planAdCredit(wallet, config, nowMs) {
  const day = localDay(nowMs, 0)
  const adsToday = wallet.adsDay === day ? wallet.adsToday : 0
  if (adsToday >= config.adsPerDay) return { credited: false, wallet }
  return {
    credited: true,
    reward: config.adReward,
    wallet: { ...wallet, balance: Math.min(wallet.balance + config.adReward, MAX_POINTS), adsDay: day, adsToday: adsToday + 1, updatedAtMs: nowMs },
  }
}

export class InsufficientPoints extends Error {
  constructor(wallet, cost) {
    super('not enough points')
    this.wallet = wallet
    this.cost = cost
  }
}

/**
 * Wallets in Firestore: `wallets/{uid}` plus one ledger entry per grant or spend in `wallets/{uid}/ledger/{id}`. The
 * ledger id makes each operation idempotent (one check-in per day, one charge per live, one credit per ad
 * transaction), and every commit is conditional on the wallet not having changed since it was read.
 */
export function createEconomy({ firestore, now = () => Date.now(), configTtlMs = 60_000 }) {
  let configCache = { value: null, expiresAt: 0 }
  // Lives already paid for, so /v1/audience doesn't read Firestore on every request.
  const paidLives = new Map()

  async function config() {
    if (configCache.value && now() < configCache.expiresAt) return configCache.value
    const doc = await firestore.get('config/economy')
    configCache = { value: parseConfig(doc?.data), expiresAt: now() + configTtlMs }
    return configCache.value
  }

  const walletPath = (uid) => `wallets/${uid}`
  const ledgerPath = (uid, id) => `wallets/${uid}/ledger/${id}`

  /**
   * Reads the wallet, lets [plan] decide, and commits the new wallet with the ledger entry. When the ledger entry
   * already exists the operation happened before: [plan] is not run and `duplicate` is returned.
   */
  async function apply(uid, ledgerId, plan) {
    const cfg = await config()
    for (let attempt = 0; attempt < 5; attempt++) {
      const [doc, existing] = await Promise.all([firestore.get(walletPath(uid)), firestore.get(ledgerPath(uid, ledgerId))])
      const at = now()
      const wallet = doc ? normalizeWallet(doc.data, cfg, at) : newWallet(cfg, at)
      if (existing) return { duplicate: true, wallet, entry: existing.data, config: cfg }
      const result = plan(wallet, cfg, at)
      if (!result.entry) return { ...result, config: cfg }
      try {
        await firestore.commit([
          { update: { name: firestore.name(walletPath(uid)), fields: encodeFields(result.wallet) }, currentDocument: doc ? { updateTime: doc.updateTime } : { exists: false } },
          { update: { name: firestore.name(ledgerPath(uid, ledgerId)), fields: encodeFields({ ...result.entry, balanceAfter: result.wallet.balance, atMs: at }) }, currentDocument: { exists: false } },
        ])
        return { ...result, config: cfg }
      } catch (e) {
        if (!(e instanceof ConflictError)) throw e
      }
    }
    throw new ConflictError('wallet busy')
  }

  return {
    config,

    async checkIn(uid, offsetMinutes) {
      const cfg = await config()
      const day = localDay(now(), offsetMinutes)
      const result = await apply(uid, `checkin-${day}`, (wallet, c, at) => {
        const r = planCheckIn(wallet, c, at, offsetMinutes)
        return r.claimed ? { ...r, entry: { kind: 'checkin', amount: r.reward, streak: r.wallet.streak } } : r
      })
      return { claimed: result.claimed === true, reward: result.reward ?? 0, wallet: result.wallet, enabled: cfg.enabled }
    },

    async spendForLive(uid, sessionId, styleId) {
      // Switched off by an admin: lives are free and nothing is recorded.
      if (!(await config()).enabled) return { wallet: null, cost: 0, duplicate: false }
      const result = await apply(uid, `live-${sessionId}`, (wallet, c, at) => {
        const cost = liveCost(c, styleId)
        const r = planSpend(wallet, cost, at)
        if (!r.ok) throw new InsufficientPoints(wallet, cost)
        return { ...r, cost, entry: { kind: 'live', amount: -cost, styleId } }
      })
      paidLives.set(`${uid}/${sessionId}`, now())
      if (paidLives.size > 20_000) paidLives.clear()
      return { wallet: result.wallet, cost: result.cost ?? -(result.entry?.amount ?? 0), duplicate: result.duplicate === true }
    },

    /** True when this live was paid for (or the economy is switched off). */
    async isLivePaid(uid, sessionId) {
      if (!(await config()).enabled) return true
      if (typeof sessionId !== 'string' || !/^[A-Za-z0-9_-]{1,64}$/.test(sessionId)) return false
      if (paidLives.has(`${uid}/${sessionId}`)) return true
      const paid = (await firestore.get(ledgerPath(uid, `live-${sessionId}`))) !== null
      if (paid) paidLives.set(`${uid}/${sessionId}`, now())
      return paid
    },

    async creditAd(uid, transactionId) {
      const result = await apply(uid, `ad-${transactionId}`, (wallet, c, at) => {
        const r = planAdCredit(wallet, c, at)
        // Recorded even when capped, so an AdMob retry of the same transaction stays a no-op.
        return { ...r, entry: { kind: 'ad', amount: r.credited ? r.reward : 0 } }
      })
      return { credited: result.credited === true, duplicate: result.duplicate === true, wallet: result.wallet }
    },
  }
}
