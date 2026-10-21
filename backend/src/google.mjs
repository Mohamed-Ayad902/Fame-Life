import { createSign } from 'node:crypto'
import { readFileSync } from 'node:fs'

const TOKEN_URL = 'https://oauth2.googleapis.com/token'
const DATASTORE_SCOPE = 'https://www.googleapis.com/auth/datastore'

/** Reads a Firebase service-account key file (JSON from the Firebase console). Kept root-only on the server. */
export function loadServiceAccount(path) {
  const sa = JSON.parse(readFileSync(path, 'utf8'))
  if (!sa.client_email || !sa.private_key || !sa.project_id) throw new Error(`${path} is not a service-account key`)
  return { clientEmail: sa.client_email, privateKey: sa.private_key, projectId: sa.project_id }
}

/**
 * OAuth access tokens for Google APIs from a service account (the JWT-bearer flow the Admin SDK uses), cached until a
 * minute before they expire. No SDK needed.
 */
export function createAccessTokenSource({ clientEmail, privateKey, scope = DATASTORE_SCOPE, fetchImpl = fetch, now = () => Date.now() }) {
  let cached = { token: null, expiresAt: 0 }
  return async function accessToken() {
    if (cached.token && now() < cached.expiresAt - 60_000) return cached.token
    const iat = Math.floor(now() / 1000)
    const encode = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url')
    const unsigned = `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode({ iss: clientEmail, scope, aud: TOKEN_URL, iat, exp: iat + 3600 })}`
    const signature = createSign('RSA-SHA256').update(unsigned).sign(privateKey).toString('base64url')
    const res = await fetchImpl(TOKEN_URL, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: `${unsigned}.${signature}` }),
      signal: AbortSignal.timeout(5000),
    })
    if (!res.ok) throw new Error(`token exchange failed: ${res.status}`)
    const body = await res.json()
    cached = { token: body.access_token, expiresAt: now() + Number(body.expires_in ?? 3600) * 1000 }
    return cached.token
  }
}
