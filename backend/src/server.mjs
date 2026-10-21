import { createServer } from 'node:http'
import { createHash } from 'node:crypto'
import { AuthError, createTokenVerifier } from './auth.mjs'
import { BadRequest, createAudienceGenerator, DEFAULT_GEMINI_MODELS, sanitizeRequest } from './audience.mjs'
import { RateLimiter } from './limits.mjs'
import { transcribe } from './transcribe.mjs'
import { UpstreamError } from './upstream.mjs'

const MAX_AUDIO_BYTES = 1_000_000 // ~30 s of 16 kHz mono 16-bit WAV; the app sends at most 15 s.
const MAX_JSON_BYTES = 16_000

class HttpError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const declared = Number(req.headers['content-length'])
    if (declared > limit) return reject(new HttpError(413, 'body too large'))
    const chunks = []
    let size = 0
    req.on('data', (chunk) => {
      size += chunk.length
      if (size > limit) {
        reject(new HttpError(413, 'body too large'))
        req.destroy()
      } else chunks.push(chunk)
    })
    req.on('end', () => resolve(Buffer.concat(chunks)))
    req.on('error', reject)
  })
}

function send(res, status, body) {
  const json = JSON.stringify(body)
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'content-length': Buffer.byteLength(json) })
  res.end(json)
}

/** Builds the request handler. Every dependency is injectable so tests run without network or keys. */
export function createHandler({ verifyToken, transcribeAudio, generateAudience, transcribeLimiter, audienceLimiter, log = console.log }) {
  async function authenticate(req) {
    const header = req.headers.authorization ?? ''
    if (!header.startsWith('Bearer ')) throw new HttpError(401, 'missing token')
    try {
      return await verifyToken(header.slice(7))
    } catch (e) {
      if (e instanceof AuthError) throw new HttpError(401, 'invalid token')
      throw e
    }
  }

  return async function handle(req, res) {
    const started = Date.now()
    let uid = null
    let status = 500
    try {
      const path = new URL(req.url, 'http://local').pathname
      if (req.method === 'GET' && path === '/healthz') {
        status = 200
        return send(res, 200, { ok: true })
      }
      if (req.method !== 'POST' || (path !== '/v1/transcribe' && path !== '/v1/audience')) throw new HttpError(404, 'not found')

      ;({ uid } = await authenticate(req))
      if (path === '/v1/transcribe') {
        if (!transcribeLimiter.take(uid)) throw new HttpError(429, 'too many requests')
        const audio = await readBody(req, MAX_AUDIO_BYTES)
        if (audio.length < 44 || audio.toString('ascii', 0, 4) !== 'RIFF') throw new HttpError(400, 'expected WAV audio')
        const result = await transcribeAudio(audio)
        status = 200
        return send(res, 200, result)
      }
      if (!audienceLimiter.take(uid)) throw new HttpError(429, 'too many requests')
      let parsed
      try {
        parsed = JSON.parse((await readBody(req, MAX_JSON_BYTES)).toString('utf8'))
      } catch (e) {
        if (e instanceof HttpError) throw e
        throw new HttpError(400, 'invalid JSON')
      }
      const result = await generateAudience(sanitizeRequest(parsed))
      status = 200
      return send(res, 200, result)
    } catch (e) {
      if (e instanceof HttpError) status = e.status
      else if (e instanceof BadRequest) status = 400
      else if (e instanceof UpstreamError) status = e.status === 429 ? 503 : 502
      else if (e?.name === 'TimeoutError') status = 504
      else status = 500
      if (status >= 500) log(`error ${e instanceof UpstreamError ? `${e.message}: ${e.body}` : e?.stack ?? e}`)
      if (!res.headersSent) send(res, status, { error: status >= 500 ? 'upstream unavailable' : e.message })
    } finally {
      // Never log transcripts or request bodies: only the route, outcome, timing and a short hash of the uid.
      const who = uid ? createHash('sha256').update(uid).digest('hex').slice(0, 8) : '-'
      log(`${req.method} ${req.url} ${status} ${Date.now() - started}ms user=${who}`)
    }
  }
}

function requireEnv(name) {
  const value = process.env[name]
  if (!value) {
    console.error(`Missing ${name}. Put it in /etc/pulse/env (see backend/README.md).`)
    process.exit(1)
  }
  return value
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const groqKey = requireEnv('GROQ_API_KEY')
  const geminiKey = requireEnv('GEMINI_API_KEY')
  const groqModel = process.env.GROQ_MODEL || 'whisper-large-v3'
  const handler = createHandler({
    verifyToken: createTokenVerifier({ projectId: process.env.FIREBASE_PROJECT_ID || 'live-fame' }),
    transcribeAudio: (wav) => transcribe(wav, { apiKey: groqKey, model: groqModel }),
    generateAudience: createAudienceGenerator({ apiKey: geminiKey, model: process.env.GEMINI_MODEL || DEFAULT_GEMINI_MODELS }),
    transcribeLimiter: new RateLimiter({ perMinute: Number(process.env.TRANSCRIBE_PER_MINUTE || 30), perDay: Number(process.env.TRANSCRIBE_PER_DAY || 1500) }),
    audienceLimiter: new RateLimiter({ perMinute: Number(process.env.AUDIENCE_PER_MINUTE || 20), perDay: Number(process.env.AUDIENCE_PER_DAY || 1000) }),
  })
  const port = Number(process.env.PORT || 8787)
  const host = process.env.HOST || '127.0.0.1'
  createServer(handler).listen(port, host, () => console.log(`pulse backend listening on ${host}:${port}`))
}
