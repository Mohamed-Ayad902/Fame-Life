import { test } from 'node:test'
import assert from 'node:assert/strict'
import { cleanTranscript } from '../src/transcribe.mjs'
import { buildGeminiBody, createAudienceGenerator, sanitizeRequest, shapeResponse } from '../src/audience.mjs'
import { RateLimiter } from '../src/limits.mjs'

const request = {
  protocol_version: 1, session_id: 's', request_seq: 1, trigger: 'utterance', language: 'mixed',
  vibes: ['supportive', 'funny'], haters_mode: false, stream_title: 'chatting', recent_speech: ['ازيكم يا جماعة النهارده هنتكلم عن الكورة'],
  context_summary: '', topic: 'just chatting', recent_comments: [], elapsed_seconds: 12, viewer_count: 140, max_comments: 4,
}

test('transcript keeps speech, reports the detected language', () => {
  const out = cleanTranscript({ language: 'arabic', text: 'x', segments: [{ text: ' ازيكم يا جماعة ', no_speech_prob: 0.01, avg_logprob: -0.2 }] })
  assert.deepEqual(out, { text: 'ازيكم يا جماعة', language: 'ar' })
})

test('transcript drops Whisper silence hallucinations', () => {
  assert.equal(cleanTranscript({ language: 'arabic', segments: [{ text: 'ترجمة نانسي قنقر', no_speech_prob: 0.1, avg_logprob: -0.3 }] }).text, '')
  assert.equal(cleanTranscript({ language: 'english', segments: [{ text: ' Thank you.', no_speech_prob: 0.5, avg_logprob: -0.3 }] }).text, '')
  assert.equal(cleanTranscript({ language: 'english', segments: [{ text: 'hello', no_speech_prob: 0.9, avg_logprob: -1.2 }] }).text, '')
  // A confident "thank you" is real speech.
  assert.equal(cleanTranscript({ language: 'english', segments: [{ text: 'Thank you.', no_speech_prob: 0.01, avg_logprob: -0.1 }] }).text, 'Thank you.')
})

test('request is bounded and unknown fields are dropped', () => {
  const out = sanitizeRequest({ ...request, recent_speech: Array(9).fill('a'.repeat(900)), max_comments: 50, language: 'klingon', evil: 1 })
  assert.equal(out.recent_speech.length, 5)
  assert.equal(out.recent_speech[0].length, 400)
  assert.equal(out.max_comments, 4)
  assert.equal(out.language, 'mixed')
  assert.equal(out.evil, undefined)
  assert.throws(() => sanitizeRequest({ ...request, protocol_version: 2 }), /protocol/)
})

test('model output is shaped to the app protocol', () => {
  const out = shapeResponse({
    comments: [{ text: 'الكورة؟ مين بيشجع الأهلي هنا 🔥', tone: 'curious' }, { text: 'x', tone: 'funny' }, { text: 'ok', tone: 'weird' }],
    reactions: ['fire', 'bomb'], suggested_event: 'none', engagement_intensity: 4, updated_topic: 'الكورة', context_summary: 'بيتكلم عن الكورة',
  }, 4)
  assert.deepEqual(out.comments, [{ text: 'الكورة؟ مين بيشجع الأهلي هنا 🔥', tone: 'curious' }, { text: 'ok', tone: 'neutral' }])
  assert.deepEqual(out.reactions, ['fire'])
  assert.equal(out.suggested_event, null)
  assert.equal(out.engagement_intensity, 1)
})

test('gemini call sends the schema and retries without thinking when the model rejects it', async () => {
  const bodies = []
  const fetchImpl = async (url, init) => {
    bodies.push(JSON.parse(init.body))
    if (bodies.length === 1) return new Response('{"error":{"message":"thinking_budget is not supported"}}', { status: 400 })
    const text = JSON.stringify({ comments: [{ text: 'مين بيشجع مين؟', tone: 'curious' }], reactions: [], suggested_event: 'none', engagement_intensity: 0.4, updated_topic: 'الكورة', context_summary: '' })
    return Response.json({ candidates: [{ content: { parts: [{ text }] } }] })
  }
  const generate = createAudienceGenerator({ apiKey: 'k', model: 'm', fetchImpl })
  const out = await generate(sanitizeRequest(request))
  assert.equal(out.comments[0].text, 'مين بيشجع مين؟')
  assert.ok(bodies[0].generationConfig.thinkingConfig)
  assert.equal(bodies[1].generationConfig.thinkingConfig, undefined)
  assert.equal(bodies[0].generationConfig.responseMimeType, 'application/json')
  assert.match(buildGeminiBody(sanitizeRequest(request)).contents[0].parts[0].text, /الكورة/)
})

test('a busy or slow model falls through to the next one; other errors do not', async () => {
  const text = JSON.stringify({ comments: [{ text: 'ahlan!', tone: 'hype' }], reactions: [], suggested_event: 'none', engagement_intensity: 0.5 })
  const calls = []
  const busyThenOk = async (url) => {
    calls.push(url.match(/models\/([^:]+)/)[1])
    if (calls.length === 1) return new Response('{"error":{"status":"UNAVAILABLE"}}', { status: 503 })
    return Response.json({ candidates: [{ content: { parts: [{ text }] } }] })
  }
  const out = await createAudienceGenerator({ apiKey: 'k', model: 'lite, full', fetchImpl: busyThenOk })(sanitizeRequest(request))
  assert.equal(out.comments[0].text, 'ahlan!')
  assert.deepEqual(calls, ['lite', 'full'])

  const timeouts = []
  const slowThenOk = async (url, init) => {
    timeouts.push(init.signal)
    if (timeouts.length === 1) throw new DOMException('timed out', 'TimeoutError')
    return Response.json({ candidates: [{ content: { parts: [{ text }] } }] })
  }
  assert.equal((await createAudienceGenerator({ apiKey: 'k', model: 'a,b', fetchImpl: slowThenOk })(sanitizeRequest(request))).comments.length, 1)

  const seen = []
  const refusedThenOk = async (url, init) => {
    seen.push([url.match(/models\/([^:]+)/)[1], Boolean(JSON.parse(init.body).generationConfig.thinkingConfig)])
    if (seen.length <= 2) return new Response('{"error":{"message":"Request contains an invalid argument."}}', { status: 400 })
    return Response.json({ candidates: [{ content: { parts: [{ text }] } }] })
  }
  assert.equal((await createAudienceGenerator({ apiKey: 'k', model: 'lite,full', fetchImpl: refusedThenOk })(sanitizeRequest(request))).comments.length, 1)
  assert.deepEqual(seen, [['lite', true], ['lite', false], ['full', true]], 'retries once without thinking, then moves on')

  let badCalls = 0
  const badKey = async () => { badCalls++; return new Response('{"error":"API key not valid"}', { status: 403 }) }
  await assert.rejects(createAudienceGenerator({ apiKey: 'k', model: 'a,b', fetchImpl: badKey })(sanitizeRequest(request)), (e) => e.status === 403)
  assert.equal(badCalls, 1)

  let t = 0
  const allBusy = async () => { t += 3_000; return new Response('busy', { status: 503 }) }
  await assert.rejects(createAudienceGenerator({ apiKey: 'k', model: 'a,b,c', fetchImpl: allBusy, budgetMs: 5_000, now: () => t })(sanitizeRequest(request)), (e) => e.status === 503)
  assert.equal(t, 6_000, 'stops once the budget is spent')
})

test('rate limiter enforces per-minute and per-day limits', () => {
  let now = 0
  const limiter = new RateLimiter({ perMinute: 2, perDay: 3, now: () => now })
  assert.ok(limiter.take('u'))
  assert.ok(limiter.take('u'))
  assert.ok(!limiter.take('u'))
  assert.ok(limiter.take('other'))
  now += 61_000
  assert.ok(limiter.take('u'))
  assert.ok(!limiter.take('u'))
  now += 86_400_000
  assert.ok(limiter.take('u'))
})
