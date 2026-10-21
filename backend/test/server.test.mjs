import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { AuthError } from '../src/auth.mjs'
import { createHandler } from '../src/server.mjs'
import { RateLimiter } from '../src/limits.mjs'
import { UpstreamError } from '../src/upstream.mjs'

async function withServer(overrides, run) {
  const logs = []
  const handler = createHandler({
    verifyToken: async (t) => { if (t !== 'good') throw new AuthError('bad'); return { uid: 'u1' } },
    transcribeAudio: async () => ({ text: 'hello there', language: 'en' }),
    generateAudience: async (req) => ({ comments: [{ text: `echo ${req.recent_speech.at(-1)}`, tone: 'neutral' }] }),
    transcribeLimiter: new RateLimiter({ perMinute: 100, perDay: 100 }),
    audienceLimiter: new RateLimiter({ perMinute: 100, perDay: 100 }),
    log: (line) => logs.push(line),
    ...overrides,
  })
  const server = createServer(handler)
  await new Promise((r) => server.listen(0, '127.0.0.1', r))
  const base = `http://127.0.0.1:${server.address().port}`
  try {
    await run(base, logs)
  } finally {
    server.close()
  }
}

const wav = () => { const b = Buffer.alloc(100); b.write('RIFF', 0, 'ascii'); return b }
const audienceBody = JSON.stringify({ protocol_version: 1, recent_speech: ['my secret plan'], max_comments: 2 })

test('requires a valid Firebase token', async () => {
  await withServer({}, async (base) => {
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: audienceBody })).status, 401)
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: audienceBody, headers: { authorization: 'Bearer bad' } })).status, 401)
  })
})

test('transcribes WAV and generates comments; never logs speech', async () => {
  await withServer({}, async (base, logs) => {
    const t = await fetch(`${base}/v1/transcribe`, { method: 'POST', body: wav(), headers: { authorization: 'Bearer good', 'content-type': 'audio/wav' } })
    assert.deepEqual(await t.json(), { text: 'hello there', language: 'en' })
    const a = await fetch(`${base}/v1/audience`, { method: 'POST', body: audienceBody, headers: { authorization: 'Bearer good' } })
    assert.equal((await a.json()).comments[0].text, 'echo my secret plan')
    assert.ok(logs.every((l) => !l.includes('secret') && !l.includes('u1')))
  })
})

test('rejects non-WAV, oversized and malformed bodies', async () => {
  await withServer({}, async (base) => {
    const auth = { authorization: 'Bearer good' }
    assert.equal((await fetch(`${base}/v1/transcribe`, { method: 'POST', body: 'nope', headers: auth })).status, 400)
    assert.equal((await fetch(`${base}/v1/transcribe`, { method: 'POST', body: Buffer.alloc(1_100_000), headers: auth })).status, 413)
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: '{', headers: auth })).status, 400)
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: '{"protocol_version":9}', headers: auth })).status, 400)
  })
})

test('limits and provider failures map to clear statuses', async () => {
  await withServer({
    audienceLimiter: new RateLimiter({ perMinute: 1, perDay: 10 }),
    transcribeAudio: async () => { throw new UpstreamError('groq', 429, 'rate limited') },
  }, async (base, logs) => {
    const auth = { authorization: 'Bearer good' }
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: audienceBody, headers: auth })).status, 200)
    assert.equal((await fetch(`${base}/v1/audience`, { method: 'POST', body: audienceBody, headers: auth })).status, 429)
    const t = await fetch(`${base}/v1/transcribe`, { method: 'POST', body: wav(), headers: auth })
    assert.equal(t.status, 503)
    assert.deepEqual(await t.json(), { error: 'upstream unavailable' })
    assert.ok(logs.some((l) => l.includes('groq returned 429')))
  })
})
