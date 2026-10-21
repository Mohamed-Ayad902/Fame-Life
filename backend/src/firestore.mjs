import { UpstreamError } from './upstream.mjs'

/** A commit lost a race (the document changed, or a "must not exist" document exists). The caller re-reads and retries. */
export class ConflictError extends Error {}

/** Plain JS value → Firestore REST `Value`. Integers stay integers; only finite numbers are accepted. */
export function encodeValue(v) {
  if (v === null || v === undefined) return { nullValue: null }
  if (typeof v === 'boolean') return { booleanValue: v }
  if (typeof v === 'string') return { stringValue: v }
  if (typeof v === 'number') {
    if (!Number.isFinite(v)) throw new Error('non-finite number')
    return Number.isInteger(v) ? { integerValue: String(v) } : { doubleValue: v }
  }
  if (Array.isArray(v)) return { arrayValue: { values: v.map(encodeValue) } }
  if (typeof v === 'object') return { mapValue: { fields: encodeFields(v) } }
  throw new Error(`cannot encode ${typeof v}`)
}

export function encodeFields(obj) {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined).map(([k, v]) => [k, encodeValue(v)]))
}

/** Firestore REST `Value` → plain JS. Timestamps become epoch milliseconds. */
export function decodeValue(v) {
  if ('nullValue' in v) return null
  if ('booleanValue' in v) return v.booleanValue
  if ('stringValue' in v) return v.stringValue
  if ('integerValue' in v) return Number(v.integerValue)
  if ('doubleValue' in v) return v.doubleValue
  if ('timestampValue' in v) return Date.parse(v.timestampValue)
  if ('arrayValue' in v) return (v.arrayValue.values ?? []).map(decodeValue)
  if ('mapValue' in v) return decodeFields(v.mapValue.fields ?? {})
  return null
}

export function decodeFields(fields) {
  return Object.fromEntries(Object.entries(fields ?? {}).map(([k, v]) => [k, decodeValue(v)]))
}

/**
 * The few Firestore REST calls the wallet needs: read a document, and commit several writes atomically with
 * preconditions (optimistic concurrency instead of transactions). The service account bypasses security rules, which
 * is why wallets, ledgers and config are server-write-only in firestore.rules.
 */
export function createFirestore({ projectId, accessToken, fetchImpl = fetch }) {
  const root = `projects/${projectId}/databases/(default)/documents`
  const base = `https://firestore.googleapis.com/v1/${root}`

  async function call(url, init) {
    const res = await fetchImpl(url, {
      ...init,
      headers: { authorization: `Bearer ${await accessToken()}`, 'content-type': 'application/json' },
      signal: AbortSignal.timeout(5000),
    })
    return res
  }

  return {
    name: (path) => `${root}/${path}`,

    /** Returns `{ data, updateTime }`, or null when the document doesn't exist. */
    async get(path) {
      const res = await call(`${base}/${path}`, { method: 'GET' })
      if (res.status === 404) return null
      if (!res.ok) throw new UpstreamError('firestore', res.status, await res.text())
      const doc = await res.json()
      return { data: decodeFields(doc.fields), updateTime: doc.updateTime }
    },

    /** Applies every write or none. Throws [ConflictError] when a precondition fails. */
    async commit(writes) {
      const res = await call(`${base}:commit`, { method: 'POST', body: JSON.stringify({ writes }) })
      if (res.ok) return
      const text = await res.text()
      if (res.status === 409 || (res.status === 400 && text.includes('FAILED_PRECONDITION'))) throw new ConflictError('precondition failed')
      if (res.status === 404 && text.includes('NOT_FOUND')) throw new ConflictError('document vanished')
      throw new UpstreamError('firestore', res.status, text)
    },
  }
}
