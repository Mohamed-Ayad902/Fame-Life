import { createVerify } from 'node:crypto'

const KEYS_URL = 'https://www.gstatic.com/admob/reward/verifier-keys.json'

export class InvalidCallback extends Error {}

/** AdMob's current public keys for rewarded-ad server-side verification, by key id. */
export async function fetchAdMobKeys() {
  const res = await fetch(KEYS_URL, { signal: AbortSignal.timeout(5000) })
  if (!res.ok) throw new Error(`AdMob key fetch failed: ${res.status}`)
  const body = await res.json()
  return Object.fromEntries((body.keys ?? []).map((k) => [String(k.keyId), k.pem]))
}

/**
 * Verifies an AdMob rewarded-ad SSV callback (https://developers.google.com/admob/android/ssv). AdMob signs
 * everything in the query string before `&signature=` with ECDSA/SHA-256; the signature and key id come last.
 * Returns the parameters the server acts on. Keys are cached for a day and re-fetched for an unknown key id.
 */
export function createSsvVerifier({ fetchKeys = fetchAdMobKeys, now = () => Date.now(), maxAgeMs = 24 * 3_600_000 }) {
  let cache = { keys: null, expiresAt: 0 }

  async function key(id) {
    if (!cache.keys || now() >= cache.expiresAt || !cache.keys[id]) {
      cache = { keys: await fetchKeys(), expiresAt: now() + 24 * 3_600_000 }
    }
    return cache.keys[id]
  }

  return async function verify(rawQuery) {
    const query = rawQuery.startsWith('?') ? rawQuery.slice(1) : rawQuery
    const cut = query.indexOf('&signature=')
    if (cut <= 0) throw new InvalidCallback('unsigned')
    const params = new URLSearchParams(query)
    const signature = params.get('signature')
    const keyId = params.get('key_id')
    if (!signature || !keyId) throw new InvalidCallback('unsigned')
    const pem = await key(keyId)
    if (!pem) throw new InvalidCallback('unknown key')
    const ok = createVerify('SHA256').update(query.slice(0, cut)).verify(pem, Buffer.from(signature, 'base64url'))
    if (!ok) throw new InvalidCallback('bad signature')

    const timestamp = Number(params.get('timestamp'))
    if (!Number.isFinite(timestamp) || Math.abs(now() - timestamp) > maxAgeMs) throw new InvalidCallback('stale')
    const transactionId = params.get('transaction_id') ?? ''
    if (!/^[A-Za-z0-9_-]{1,128}$/.test(transactionId)) throw new InvalidCallback('bad transaction')
    return {
      transactionId,
      // Set by the app (ServerSideVerificationOptions.userId) to the Firebase uid; signed, so it can't be swapped later.
      userId: params.get('user_id') ?? '',
      adUnit: params.get('ad_unit') ?? '',
    }
  }
}

/** True when [adUnit] (the numeric id AdMob reports) is one of the configured `ca-app-pub-…/123` units, or none are set. */
export function isAllowedAdUnit(adUnit, allowed) {
  if (!allowed.length) return true
  return allowed.some((unit) => unit === adUnit || unit.split('/').pop() === adUnit)
}
