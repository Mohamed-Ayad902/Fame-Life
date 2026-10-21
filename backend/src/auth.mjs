import { X509Certificate, createVerify } from 'node:crypto'

const CERTS_URL = 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com'

export class AuthError extends Error {}

/** Google's current Firebase signing certificates, with how long they may be cached. */
export async function fetchFirebaseCerts() {
  const res = await fetch(CERTS_URL, { signal: AbortSignal.timeout(5000) })
  if (!res.ok) throw new Error(`certificate fetch failed: ${res.status}`)
  const maxAge = Number(/max-age=(\d+)/.exec(res.headers.get('cache-control') ?? '')?.[1] ?? 3600)
  return { certs: await res.json(), maxAgeSeconds: maxAge }
}

/**
 * Verifies a Firebase Auth ID token the way the Admin SDK does (RS256 signature, audience, issuer, expiry) without
 * the SDK. Returns the user's uid.
 */
export function createTokenVerifier({ projectId, fetchCerts = fetchFirebaseCerts, now = () => Date.now() }) {
  let cache = { keys: null, expiresAt: 0 }

  async function keys() {
    if (cache.keys && now() < cache.expiresAt) return cache.keys
    const { certs, maxAgeSeconds } = await fetchCerts()
    const parsed = Object.fromEntries(Object.entries(certs).map(([kid, pem]) => [kid, new X509Certificate(pem).publicKey]))
    cache = { keys: parsed, expiresAt: now() + maxAgeSeconds * 1000 }
    return parsed
  }

  return async function verify(token) {
    const parts = typeof token === 'string' ? token.split('.') : []
    if (parts.length !== 3) throw new AuthError('malformed token')
    let header, payload
    try {
      header = JSON.parse(Buffer.from(parts[0], 'base64url').toString())
      payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString())
    } catch {
      throw new AuthError('malformed token')
    }
    if (header.alg !== 'RS256') throw new AuthError('wrong algorithm')
    const key = (await keys())[header.kid]
    if (!key) throw new AuthError('unknown key')
    const signed = createVerify('RSA-SHA256').update(`${parts[0]}.${parts[1]}`)
    if (!signed.verify(key, Buffer.from(parts[2], 'base64url'))) throw new AuthError('bad signature')

    const nowSeconds = now() / 1000
    const skew = 300
    if (payload.aud !== projectId) throw new AuthError('wrong audience')
    if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new AuthError('wrong issuer')
    if (typeof payload.sub !== 'string' || payload.sub.length === 0 || payload.sub.length > 128) throw new AuthError('no subject')
    if (!(payload.exp > nowSeconds - skew)) throw new AuthError('expired')
    if (!(payload.iat <= nowSeconds + skew)) throw new AuthError('issued in the future')
    return { uid: payload.sub }
  }
}
