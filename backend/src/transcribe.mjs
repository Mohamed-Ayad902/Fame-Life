import { UpstreamError } from './upstream.mjs'

const GROQ_URL = 'https://api.groq.com/openai/v1/audio/transcriptions'

/** Sends one utterance (WAV bytes) to Groq Whisper, which detects the language itself. */
export async function transcribe(wav, { apiKey, model, fetchImpl = fetch }) {
  const form = new FormData()
  form.append('file', new Blob([wav], { type: 'audio/wav' }), 'speech.wav')
  form.append('model', model)
  form.append('response_format', 'verbose_json')
  form.append('temperature', '0')
  const res = await fetchImpl(GROQ_URL, {
    method: 'POST',
    headers: { Authorization: `Bearer ${apiKey}` },
    body: form,
    signal: AbortSignal.timeout(15_000),
  })
  if (!res.ok) throw new UpstreamError('groq', res.status, await res.text().catch(() => ''))
  return cleanTranscript(await res.json())
}

const LANGUAGE_CODES = { arabic: 'ar', english: 'en' }

// Whisper invents these on silence or noise, mostly from subtitle credits in its training data.
const ALWAYS_NOISE = [
  /ترجمة\s+نانسي/, /اشتركوا?\s+في\s+القناة/, /لا\s+تنسوا\s+الاشتراك/, /subtitles?\s+by/i, /amara\.org/i,
  /thanks?\s+for\s+watching/i, /please\s+subscribe/i, /^\[?\(?(music|موسيقى)\)?\]?$/i,
]
const NOISE_WHEN_UNSURE = ['thank you', 'thanks', 'you', 'bye', 'شكرا', 'شكرا لكم', 'شكرا جزيلا', 'مع السلامة']

/** Drops segments Whisper itself marks as probably not speech, and the phrases it hallucinates on silence. */
export function cleanTranscript(body) {
  const segments = Array.isArray(body?.segments) ? body.segments : null
  const kept = segments
    ? segments.filter((s) => !(s.no_speech_prob > 0.6 && s.avg_logprob < -0.5))
    : null
  let text = (kept ? kept.map((s) => s.text).join(' ') : String(body?.text ?? '')).replace(/\s+/g, ' ').trim()

  const bare = text.toLowerCase().replace(/[.!?،,؟…\s]+/g, ' ').trim()
  const unsure = kept?.length ? Math.max(...kept.map((s) => s.no_speech_prob ?? 0)) > 0.2 : !segments
  if (ALWAYS_NOISE.some((pattern) => pattern.test(bare))) text = ''
  else if (unsure && NOISE_WHEN_UNSURE.includes(bare)) text = ''
  if (!/[\p{L}\p{N}]/u.test(text)) text = ''

  const language = String(body?.language ?? '').toLowerCase()
  return { text: text.slice(0, 400), language: LANGUAGE_CODES[language] ?? (language || null) }
}
