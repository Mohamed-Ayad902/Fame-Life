import { test } from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createSign } from 'node:crypto'
import { createTokenVerifier } from '../src/auth.mjs'

// A throwaway key and self-signed certificate stand in for Google's signing certificates.
const dir = mkdtempSync(join(tmpdir(), 'pulse-auth-'))
execFileSync('openssl', ['req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', join(dir, 'key.pem'),
  '-out', join(dir, 'cert.pem'), '-days', '1', '-subj', '/CN=test'], { stdio: 'ignore' })
const privateKey = readFileSync(join(dir, 'key.pem'), 'utf8')
const cert = readFileSync(join(dir, 'cert.pem'), 'utf8')

const NOW = 1_800_000_000_000
const project = 'live-fame'

function token(claims = {}, header = {}) {
  const h = Buffer.from(JSON.stringify({ alg: 'RS256', kid: 'k1', ...header })).toString('base64url')
  const p = Buffer.from(JSON.stringify({
    aud: project, iss: `https://securetoken.google.com/${project}`, sub: 'user-1',
    iat: NOW / 1000 - 10, exp: NOW / 1000 + 3600, ...claims,
  })).toString('base64url')
  const sig = createSign('RSA-SHA256').update(`${h}.${p}`).sign(privateKey).toString('base64url')
  return `${h}.${p}.${sig}`
}

const verifier = () => createTokenVerifier({
  projectId: project,
  now: () => NOW,
  fetchCerts: async () => ({ certs: { k1: cert }, maxAgeSeconds: 3600 }),
})

test('accepts a valid token', async () => {
  assert.deepEqual(await verifier()(token()), { uid: 'user-1' })
})

test('rejects wrong audience, issuer, expiry, key and tampering', async () => {
  const verify = verifier()
  await assert.rejects(verify(token({ aud: 'other' })), /audience/)
  await assert.rejects(verify(token({ iss: 'https://evil' })), /issuer/)
  await assert.rejects(verify(token({ exp: NOW / 1000 - 1000 })), /expired/)
  await assert.rejects(verify(token({}, { kid: 'nope' })), /unknown key/)
  await assert.rejects(verify(token({}, { alg: 'none' })), /algorithm/)
  const [h, , s] = token().split('.')
  const forged = Buffer.from(JSON.stringify({ aud: project, iss: `https://securetoken.google.com/${project}`, sub: 'admin', iat: 0, exp: NOW })).toString('base64url')
  await assert.rejects(verify(`${h}.${forged}.${s}`), /signature/)
  await assert.rejects(verify('garbage'), /malformed/)
})

test('caches certificates until they expire', async () => {
  let fetches = 0
  let now = NOW
  const verify = createTokenVerifier({
    projectId: project,
    now: () => now,
    fetchCerts: async () => { fetches++; return { certs: { k1: cert }, maxAgeSeconds: 60 } },
  })
  await verify(token())
  await verify(token())
  assert.equal(fetches, 1)
  now += 61_000
  await verify(token())
  assert.equal(fetches, 2)
})
