import { test } from 'node:test'
import assert from 'node:assert/strict'
import { generateKeyPairSync, createSign } from 'node:crypto'
import { createServer } from 'node:http'
import { AuthError } from '../src/auth.mjs'
import { createSsvVerifier, InvalidCallback, isAllowedAdUnit } from '../src/admob.mjs'
import {
  createEconomy, DEFAULT_CONFIG, localDay, MIN_CLAIM_GAP_MS, newWallet, parseConfig, planAdCredit, planCheckIn, planSpend,
} from '../src/economy.mjs'
import { ConflictError, decodeFields, encodeFields } from '../src/firestore.mjs'
import { RateLimiter } from '../src/limits.mjs'
import { createHandler } from '../src/server.mjs'

const DAY = 86_400_000
const config = parseConfig({})

/** In-memory Firestore with the same precondition semantics as the REST commit. */
function fakeFirestore(seed = {}) {
  const docs = new Map(Object.entries(seed).map(([k, v]) => [k, { data: v, updateTime: 't0' }]))
  let clock = 0
  return {
    docs,
    name: (path) => path,
    async get(path) {
      const doc = docs.get(path)
      return doc ? { data: structuredClone(doc.data), updateTime: doc.updateTime } : null
    },
    async commit(writes) {
      for (const w of writes) {
        const doc = docs.get(w.update.name)
        if (w.currentDocument.exists === false && doc) throw new ConflictError('exists')
        if (w.currentDocument.updateTime && doc?.updateTime !== w.currentDocument.updateTime) throw new ConflictError('changed')
      }
      for (const w of writes) docs.set(w.update.name, { data: decodeFields(w.update.fields), updateTime: `t${++clock}` })
    },
  }
}

test('config falls back field by field and rejects bad values', () => {
  const parsed = parseConfig({ streakRewards: [5, 'x'], adReward: 40, liveCosts: { aurora: 10, 'Bad Id': 3, noir: -1 }, adsPerDay: 1000 })
  assert.deepEqual(parsed.streakRewards, DEFAULT_CONFIG.streakRewards)
  assert.equal(parsed.adReward, 40)
  assert.deepEqual(parsed.liveCosts, { aurora: 10 })
  assert.equal(parsed.adsPerDay, DEFAULT_CONFIG.adsPerDay)
})

test('the streak grows on consecutive days, cycles through the rewards and resets after a missed day', () => {
  let wallet = newWallet(config, 0)
  const start = 100 * DAY + 10 * 3_600_000
  const rewards = []
  for (let d = 0; d < config.streakRewards.length + 1; d++) {
    const r = planCheckIn(wallet, config, start + d * DAY, 0)
    assert.ok(r.claimed)
    rewards.push(r.reward)
    wallet = r.wallet
  }
  assert.deepEqual(rewards, [...config.streakRewards, config.streakRewards[0]])
  assert.equal(planCheckIn(wallet, config, start + config.streakRewards.length * DAY + 3_600_000, 0).claimed, false)
  const afterGap = planCheckIn(wallet, config, start + (config.streakRewards.length + 2) * DAY, 0)
  assert.equal(afterGap.wallet.streak, 1)
})

test('a time-zone change cannot claim twice within the minimum gap', () => {
  const at = 100 * DAY + 23 * 3_600_000
  const first = planCheckIn(newWallet(config, 0), config, at, 0)
  // Same moment, a zone where it's already tomorrow.
  assert.equal(planCheckIn(first.wallet, config, at + 60_000, 120).claimed, false)
  assert.equal(planCheckIn(first.wallet, config, at + MIN_CLAIM_GAP_MS, 0).claimed, true)
  assert.equal(localDay(0, 99_999), localDay(0, 840))
})

test('spending needs enough points; ads stop crediting at the daily cap', () => {
  const wallet = { ...newWallet(config, 0), balance: 10 }
  assert.equal(planSpend(wallet, 30, 1).ok, false)
  assert.equal(planSpend(wallet, 10, 1).wallet.balance, 0)
  let w = wallet
  for (let i = 0; i < config.adsPerDay; i++) w = planAdCredit(w, config, 5 * DAY).wallet
  assert.equal(w.balance, 10 + config.adsPerDay * config.adReward)
  assert.equal(planAdCredit(w, config, 5 * DAY).credited, false)
  assert.equal(planAdCredit(w, config, 6 * DAY).credited, true)
})

test('wallet operations are idempotent and survive a concurrent write', async () => {
  const db = fakeFirestore({ 'config/economy': { liveCosts: { noir: 50 }, defaultLiveCost: 30, startingBalance: 60 } })
  let now = 200 * DAY
  const economy = createEconomy({ firestore: db, now: () => now })

  const first = await economy.checkIn('u1', 0)
  assert.equal(first.claimed, true)
  assert.equal(first.wallet.balance, 60 + config.streakRewards[0])
  assert.equal((await economy.checkIn('u1', 0)).claimed, false)

  const spend = await economy.spendForLive('u1', 's1', 'noir')
  assert.equal(spend.cost, 50)
  const again = await economy.spendForLive('u1', 's1', 'noir')
  assert.equal(again.duplicate, true)
  assert.equal(again.wallet.balance, spend.wallet.balance)
  assert.equal(await economy.isLivePaid('u1', 's1'), true)
  assert.equal(await economy.isLivePaid('u1', 's2'), false)
  await assert.rejects(economy.spendForLive('u1', 's3', 'noir'), { message: 'not enough points' })

  // Another write lands between the read and the commit: the commit fails its precondition and is retried.
  const realCommit = db.commit
  let raced = false
  db.commit = async (writes) => {
    if (!raced) {
      raced = true
      const w = db.docs.get('wallets/u1')
      db.docs.set('wallets/u1', { data: { ...w.data, balance: w.data.balance + 1 }, updateTime: 'other' })
    }
    return realCommit(writes)
  }
  const before = (await db.get('wallets/u1')).data.balance
  const ad = await economy.creditAd('u1', 'tx1')
  assert.equal(ad.credited, true)
  assert.equal(ad.wallet.balance, before + 1 + config.adReward)
  assert.equal((await economy.creditAd('u1', 'tx1')).duplicate, true)
  assert.equal(db.docs.get('wallets/u1/ledger/ad-tx1').data.amount, config.adReward)
})

test('field encoding round-trips', () => {
  const value = { a: 1, b: 1.5, c: 'x', d: true, e: [1, 'y'], f: { g: null } }
  const encoded = encodeFields(value)
  assert.equal(encoded.a.integerValue, '1')
  assert.deepEqual(decodeFields(encoded), value)
})

function adMobFixture() {
  const { privateKey, publicKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' })
  const pem = publicKey.export({ type: 'spki', format: 'pem' })
  const sign = (query) => {
    const signature = createSign('SHA256').update(query).sign(privateKey).toString('base64url')
    return `${query}&signature=${signature}&key_id=7`
  }
  return { pem, sign }
}

test('AdMob callbacks need a valid, fresh signature', async () => {
  const { pem, sign } = adMobFixture()
  const now = 1_700_000_000_000
  const verify = createSsvVerifier({ fetchKeys: async () => ({ 7: pem }), now: () => now })
  const query = `ad_network=1&ad_unit=123&custom_data=&reward_amount=1&reward_item=points&timestamp=${now}&transaction_id=tx9&user_id=uid1`
  const ok = await verify(`?${sign(query)}`)
  assert.deepEqual(ok, { transactionId: 'tx9', userId: 'uid1', adUnit: '123' })
  const tampered = sign(query).replace('user_id=uid1', 'user_id=uid2')
  await assert.rejects(verify(tampered), InvalidCallback)
  await assert.rejects(verify(query), InvalidCallback)
  const old = sign(query.replace(`timestamp=${now}`, `timestamp=${now - 2 * DAY}`))
  await assert.rejects(verify(old), InvalidCallback)
  assert.equal(isAllowedAdUnit('123', ['ca-app-pub-1/123']), true)
  assert.equal(isAllowedAdUnit('124', ['ca-app-pub-1/123']), false)
  assert.equal(isAllowedAdUnit('124', []), true)
})

async function withServer(overrides, run) {
  const logs = []
  const server = createServer(createHandler({
    verifyToken: async (t) => { if (t !== 'good') throw new AuthError('bad'); return { uid: 'u1' } },
    transcribeAudio: async () => ({ text: '', language: 'en' }),
    generateAudience: async () => ({ comments: [] }),
    transcribeLimiter: new RateLimiter({ perMinute: 100, perDay: 100 }),
    audienceLimiter: new RateLimiter({ perMinute: 100, perDay: 100 }),
    pointsLimiter: new RateLimiter({ perMinute: 100, perDay: 100 }),
    log: (line) => logs.push(line),
    ...overrides,
  }))
  await new Promise((r) => server.listen(0, '127.0.0.1', r))
  try {
    await run(`http://127.0.0.1:${server.address().port}`, logs)
  } finally {
    server.close()
  }
}

const post = (base, path, body) =>
  fetch(`${base}${path}`, { method: 'POST', body: JSON.stringify(body), headers: { authorization: 'Bearer good' } })

test('points routes: check-in, spend, 402 when short, and the audience only for paid lives', async () => {
  const db = fakeFirestore({ 'config/economy': { startingBalance: 10, liveCosts: { aurora: 30 } } })
  const economy = createEconomy({ firestore: db })
  await withServer({ economy }, async (base) => {
    const checkin = await (await post(base, '/v1/points/checkin', { tz_offset_minutes: 120 })).json()
    assert.equal(checkin.claimed, true)
    assert.equal(checkin.wallet.balance, 10 + config.streakRewards[0])

    assert.equal((await post(base, '/v1/audience', { protocol_version: 1, session_id: 'live1' })).status, 402)
    const spend = await post(base, '/v1/points/spend', { session_id: 'live1', style_id: 'aurora' })
    assert.equal(spend.status, 200)
    assert.equal((await spend.json()).cost, 30)
    assert.equal((await post(base, '/v1/audience', { protocol_version: 1, session_id: 'live1' })).status, 200)

    assert.equal((await post(base, '/v1/points/spend', { session_id: '../x' })).status, 400)
    const short = await post(base, '/v1/points/spend', { session_id: 'live2', style_id: 'aurora' })
    assert.equal(short.status, 402)
    assert.equal((await short.json()).cost, 30)
  })
})

test('without a service account the points routes say so and lives are not charged', async () => {
  await withServer({}, async (base) => {
    assert.equal((await post(base, '/v1/points/checkin', {})).status, 503)
    assert.equal((await post(base, '/v1/audience', { protocol_version: 1, session_id: 'x' })).status, 200)
  })
})

test('the AdMob callback credits the signed user once and never logs the query', async () => {
  const { pem, sign } = adMobFixture()
  const db = fakeFirestore()
  const economy = createEconomy({ firestore: db })
  const verifySsv = createSsvVerifier({ fetchKeys: async () => ({ 7: pem }) })
  await withServer({ economy, verifySsv }, async (base, logs) => {
    const query = sign(`ad_network=1&ad_unit=123&reward_amount=1&reward_item=p&timestamp=${Date.now()}&transaction_id=t1&user_id=secretuid`)
    assert.equal((await fetch(`${base}/v1/ads/admob-ssv?${query}`)).status, 200)
    assert.equal((await fetch(`${base}/v1/ads/admob-ssv?${query}`)).status, 200)
    assert.equal(db.docs.get('wallets/secretuid').data.balance, config.startingBalance + config.adReward)
    assert.equal((await fetch(`${base}/v1/ads/admob-ssv?${query.replace('t1', 't2')}`)).status, 400)
    assert.ok(logs.every((l) => !l.includes('secretuid')))
  })
})
