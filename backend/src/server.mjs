import { createServer } from 'node:http'
import { createHash } from 'node:crypto'
import { AuthError, createTokenVerifier } from './auth.mjs'
import { BadRequest, createAudienceGenerator, DEFAULT_GEMINI_MODELS, sanitizeRequest } from './audience.mjs'
import { createSsvVerifier, InvalidCallback, isAllowedAdUnit } from './admob.mjs'
import { createEconomy, InsufficientPoints } from './economy.mjs'
import { createFirestore } from './firestore.mjs'
import { createAccessTokenSource, loadServiceAccount } from './google.mjs'
import { RateLimiter } from './limits.mjs'
import { transcribe } from './transcribe.mjs'
import { UpstreamError } from './upstream.mjs'

const MAX_AUDIO_BYTES = 1_000_000 // ~30 s of 16 kHz mono 16-bit WAV; the app sends at most 15 s.
const MAX_JSON_BYTES = 16_000
const POST_ROUTES = new Set(['/v1/transcribe', '/v1/audience', '/v1/points/checkin', '/v1/points/spend'])
const ID = /^[A-Za-z0-9_-]{1,64}$/

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

async function readJson(req) {
  try {
    const parsed = JSON.parse((await readBody(req, MAX_JSON_BYTES)).toString('utf8'))
    if (!parsed || typeof parsed !== 'object') throw new HttpError(400, 'body must be a JSON object')
    return parsed
  } catch (e) {
    if (e instanceof HttpError) throw e
    throw new HttpError(400, 'invalid JSON')
  }
}

/** The wallet fields the app shows; the same shape it reads from Firestore. */
const walletBody = (w) => (w ? { balance: w.balance, streak: w.streak, lastClaimDay: w.lastClaimDay, lastClaimAtMs: w.lastClaimAtMs, adsDay: w.adsDay, adsToday: w.adsToday } : null)

/**
 * Builds the request handler. Every dependency is injectable so tests run without network or keys. [economy] and
 * [verifySsv] are null when the server has no service account: the points routes then answer 503 and lives are
 * not charged.
 */
export function createHandler({ verifyToken, transcribeAudio, generateAudience, transcribeLimiter, audienceLimiter, pointsLimiter, economy = null, verifySsv = null, log = console.log }) {
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
      // Called by AdMob, not the app: authenticated by AdMob's signature instead of a Firebase token. Always 200 once
      // the signature checks out, so AdMob doesn't retry a callback that was handled (or deliberately ignored).
      if (req.method === 'GET' && path === '/v1/ads/admob-ssv') {
        if (!economy || !verifySsv) throw new HttpError(503, 'points unavailable')
        let callback
        try {
          callback = await verifySsv(new URL(req.url, 'http://local').search)
        } catch (e) {
          if (e instanceof InvalidCallback) throw new HttpError(400, e.message)
          throw e
        }
        const config = await economy.config()
        // AdMob's console sends a test callback without a user; it is acknowledged and credits nothing.
        if (ID.test(callback.userId) && callback.userId.length <= 128 && isAllowedAdUnit(callback.adUnit, config.adUnits)) {
          uid = callback.userId
          await economy.creditAd(uid, callback.transactionId)
        }
        status = 200
        return send(res, 200, { ok: true })
      }
      if (req.method !== 'POST' || !POST_ROUTES.has(path)) throw new HttpError(404, 'not found')

      ;({ uid } = await authenticate(req))
      if (path.startsWith('/v1/points/')) {
        if (!economy) throw new HttpError(503, 'points unavailable')
        if (!pointsLimiter.take(uid)) throw new HttpError(429, 'too many requests')
        const body = await readJson(req)
        if (path === '/v1/points/checkin') {
          const result = await economy.checkIn(uid, Number(body.tz_offset_minutes))
          status = 200
          return send(res, 200, { claimed: result.claimed, reward: result.reward, wallet: walletBody(result.wallet) })
        }
        if (typeof body.session_id !== 'string' || !ID.test(body.session_id)) throw new HttpError(400, 'invalid session_id')
        const styleId = typeof body.style_id === 'string' ? body.style_id.slice(0, 32) : ''
        try {
          const result = await economy.spendForLive(uid, body.session_id, styleId)
          status = 200
          return send(res, 200, { cost: result.cost, wallet: walletBody(result.wallet) })
        } catch (e) {
          if (!(e instanceof InsufficientPoints)) throw e
          status = 402
          return send(res, 402, { error: e.message, cost: e.cost, wallet: walletBody(e.wallet) })
        }
      }
      if (path === '/v1/transcribe') {
        if (!transcribeLimiter.take(uid)) throw new HttpError(429, 'too many requests')
        const audio = await readBody(req, MAX_AUDIO_BYTES)
        if (audio.length < 44 || audio.toString('ascii', 0, 4) !== 'RIFF') throw new HttpError(400, 'expected WAV audio')
        const result = await transcribeAudio(audio)
        status = 200
        return send(res, 200, result)
      }
      if (!audienceLimiter.take(uid)) throw new HttpError(429, 'too many requests')
      const parsed = await readJson(req)
      // The AI is what costs money, so it only answers lives that were paid for with points.
      if (economy && !(await economy.isLivePaid(uid, parsed.session_id))) throw new HttpError(402, 'live not paid')
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
      // Never log transcripts, request bodies or query strings (AdMob's carries the uid): only the route, outcome, timing and a short hash of the uid.
      const who = uid ? createHash('sha256').update(uid).digest('hex').slice(0, 8) : '-'
      log(`${req.method} ${req.url.split('?')[0]} ${status} ${Date.now() - started}ms user=${who}`)
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

/** The wallet needs a Firebase service account (GOOGLE_APPLICATION_CREDENTIALS); without one the economy is off. */
function pointsServices() {
  const path = process.env.GOOGLE_APPLICATION_CREDENTIALS
  if (!path) {
    console.log('points economy off: GOOGLE_APPLICATION_CREDENTIALS is not set')
    return {}
  }
  const account = loadServiceAccount(path)
  const firestore = createFirestore({ projectId: account.projectId, accessToken: createAccessTokenSource(account) })
  return { economy: createEconomy({ firestore }), verifySsv: createSsvVerifier({}) }
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
    pointsLimiter: new RateLimiter({ perMinute: 20, perDay: 500 }),
    ...pointsServices(),
  })
  const port = Number(process.env.PORT || 8787)
  const host = process.env.HOST || '127.0.0.1'
  createServer(handler).listen(port, host, () => console.log(`pulse backend listening on ${host}:${port}`))
}
