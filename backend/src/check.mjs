// Confirms the keys in the environment work: checks the Groq model and makes one real Gemini comment request.
import { createAudienceGenerator, DEFAULT_GEMINI_MODELS, sanitizeRequest } from './audience.mjs'

const groqKey = process.env.GROQ_API_KEY
const geminiKey = process.env.GEMINI_API_KEY
const groqModel = process.env.GROQ_MODEL || 'whisper-large-v3'
const geminiModel = process.env.GEMINI_MODEL || DEFAULT_GEMINI_MODELS
let failed = false

try {
  const res = await fetch(`https://api.groq.com/openai/v1/models/${groqModel}`, {
    headers: { Authorization: `Bearer ${groqKey}` },
    signal: AbortSignal.timeout(10_000),
  })
  if (res.ok) console.log(`ok    Groq key works, model ${groqModel} available`)
  else {
    failed = true
    console.log(`FAIL  Groq: HTTP ${res.status} ${(await res.text()).slice(0, 200)}`)
  }
} catch (e) {
  failed = true
  console.log(`FAIL  Groq: ${e.message}`)
}

// Each model on its own, so a broken name shows up even when the first one answers. A busy model is only a warning:
// the server falls through to the next one, and the app to its on-device comments.
let geminiAnswered = false
for (const m of geminiModel.split(',').map((x) => x.trim()).filter(Boolean)) try {
  const generate = createAudienceGenerator({ apiKey: geminiKey, model: m, budgetMs: 15_000 })
  const started = Date.now()
  const out = await generate(sanitizeRequest({
    protocol_version: 1, trigger: 'question', language: 'mixed', vibes: ['supportive', 'funny'], haters_mode: false,
    stream_title: 'first live', topic: 'just chatting', context_summary: '', recent_comments: [],
    recent_speech: ['ازيكم يا جماعة، النهارده أول لايف ليا', 'مين فيكم بيحب الكورة؟'], elapsed_seconds: 20, viewer_count: 120, max_comments: 3,
  }))
  geminiAnswered = true
  console.log(`ok    Gemini ${m} answered in ${Date.now() - started} ms:`)
  for (const c of out.comments) console.log(`        [${c.tone}] ${c.text}`)
} catch (e) {
  const busy = e?.name === 'TimeoutError' || e?.status === 429 || e?.status >= 500
  if (busy) console.log(`busy  Gemini ${m} is overloaded right now (${e.status ?? 'timeout'}); requests use the other models meanwhile`)
  else {
    failed = true
    console.log(`FAIL  Gemini ${m}: ${e.message} ${e.body ?? ''}`)
  }
}
if (!geminiAnswered) failed = true
process.exit(failed ? 1 : 0)
