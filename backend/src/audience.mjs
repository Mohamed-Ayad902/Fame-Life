import { UpstreamError } from './upstream.mjs'

/** Must match PROTOCOL_VERSION in the app's AudienceProtocol.kt. */
export const PROTOCOL_VERSION = 1
export const PROMPT_VERSION = 'audience-v1'

const TONES = ['supportive', 'curious', 'funny', 'hype', 'critical', 'neutral']
const REACTIONS = ['heart', 'fire', 'laugh', 'wow', 'clap']
const EVENTS = ['none', 'small_gift', 'medium_gift', 'major_gift', 'vip_join', 'trending', 'reaction_burst']
const LANGUAGES = ['english', 'arabic', 'mixed']

export class BadRequest extends Error {}

const SYSTEM_PROMPT = `You write the live chat for a simulated live stream in an app where people practise being a streamer.
You get the creator's latest speech (newest last) and session details as JSON. Write what different viewers would type right now.

Relevance comes first:
- React to the newest speech. Mention the concrete things the creator said, answer their questions, ask natural follow-ups.
- Use the earlier speech and context_summary for continuity, but never react only to old speech.
- Speech comes from automatic transcription and can contain small mistakes. Read it for its likely meaning.

Language:
- language "english": comments in English. language "arabic": comments in Arabic.
- language "mixed": write in whatever the creator is speaking in the newest speech. Arabic gets Arabic replies in the same dialect (Egyptian Arabic for an Egyptian speaker), and if the creator mixes Arabic and English, the chat mixes too.
- Arabic comments use Arabic script.

Style:
- Each comment is a different viewer. Vary length (mostly 1 to 10 words), wording, punctuation and emoji. Sound like real live chat, casual and lowercase.
- Never repeat or closely rephrase anything in recent_comments.
- No usernames, hashtags, links, phone numbers or emails. Never say you are an AI or that the stream is simulated.
- Tones: supportive, curious, funny, hype, neutral. Use "critical" only when haters_mode is true, and keep it a light disagreement: no insults about looks or identity, no slurs, nothing about self-harm.
- vibes lists the moods the creator asked for; lean towards them.
- trigger "idle" means the creator went quiet: write one comment that nudges them about the stream topic.
- Write at most max_comments comments.

Other fields:
- reactions: 0 to 3 emoji reactions that fit the moment.
- suggested_event: usually "none". Suggest a gift, vip_join, trending or reaction_burst only when the moment clearly earns it (big news, a joke that lands, a moving story).
- engagement_intensity: 0 to 1, how exciting this moment is.
- updated_topic: 1 to 4 words naming what the stream is about now, in the creator's language.
- context_summary: at most 2 short sentences remembering the key facts the creator has shared so far (names, plans, stories), merged with the previous summary.`

const RESPONSE_SCHEMA = {
  type: 'OBJECT',
  properties: {
    comments: {
      type: 'ARRAY',
      items: {
        type: 'OBJECT',
        properties: { text: { type: 'STRING' }, tone: { type: 'STRING', enum: TONES } },
        required: ['text', 'tone'],
      },
    },
    reactions: { type: 'ARRAY', items: { type: 'STRING', enum: REACTIONS } },
    suggested_event: { type: 'STRING', enum: EVENTS },
    engagement_intensity: { type: 'NUMBER' },
    updated_topic: { type: 'STRING' },
    context_summary: { type: 'STRING' },
  },
  required: ['comments', 'reactions', 'suggested_event', 'engagement_intensity', 'updated_topic', 'context_summary'],
}

const text = (value, max) => (typeof value === 'string' ? value.replace(/\s+/g, ' ').trim().slice(0, max) : '')
const textList = (value, maxItems, maxLength) =>
  (Array.isArray(value) ? value : []).map((v) => text(v, maxLength)).filter(Boolean).slice(-maxItems)

/** Copies only the fields the prompt needs from the untrusted request, with every size bounded. */
export function sanitizeRequest(raw) {
  if (!raw || typeof raw !== 'object') throw new BadRequest('body must be a JSON object')
  if (raw.protocol_version !== PROTOCOL_VERSION) throw new BadRequest('unsupported protocol_version')
  const maxComments = Number.isInteger(raw.max_comments) ? Math.min(Math.max(raw.max_comments, 1), 4) : 3
  return {
    trigger: text(raw.trigger, 20) || 'utterance',
    language: LANGUAGES.includes(raw.language) ? raw.language : 'mixed',
    vibes: textList(raw.vibes, 6, 20),
    haters_mode: raw.haters_mode === true,
    stream_title: text(raw.stream_title, 80),
    topic: text(raw.topic, 60),
    context_summary: text(raw.context_summary, 400),
    recent_speech: textList(raw.recent_speech, 5, 400),
    recent_comments: textList(raw.recent_comments, 8, 140),
    elapsed_seconds: Number.isFinite(raw.elapsed_seconds) ? Math.max(0, Math.round(raw.elapsed_seconds)) : 0,
    viewer_count: Number.isFinite(raw.viewer_count) ? Math.max(0, Math.round(raw.viewer_count)) : 0,
    max_comments: maxComments,
  }
}

export function buildGeminiBody(request, { thinking = true } = {}) {
  const generationConfig = {
    responseMimeType: 'application/json',
    responseSchema: RESPONSE_SCHEMA,
    temperature: 1.0,
    maxOutputTokens: 800,
  }
  // Thinking adds seconds of latency; short chat lines don't need it.
  if (thinking) generationConfig.thinkingConfig = { thinkingBudget: 0 }
  return {
    systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
    contents: [{ role: 'user', parts: [{ text: JSON.stringify(request) }] }],
    generationConfig,
  }
}

/** Turns the model's JSON into the app's AudienceResponseDto shape. The app validates it again. */
export function shapeResponse(modelJson, maxComments) {
  const out = modelJson && typeof modelJson === 'object' ? modelJson : {}
  const comments = (Array.isArray(out.comments) ? out.comments : [])
    .map((c) => ({ text: text(c?.text, 140), tone: TONES.includes(c?.tone) ? c.tone : 'neutral' }))
    .filter((c) => c.text.length >= 2)
    .slice(0, maxComments)
  const intensity = Number(out.engagement_intensity)
  const event = EVENTS.includes(out.suggested_event) ? out.suggested_event : 'none'
  return {
    comments,
    reactions: (Array.isArray(out.reactions) ? out.reactions : []).filter((r) => REACTIONS.includes(r)).slice(0, 3),
    suggested_event: event === 'none' ? null : event,
    event_reason: null,
    engagement_intensity: Number.isFinite(intensity) ? Math.min(Math.max(intensity, 0), 1) : 0.5,
    updated_topic: text(out.updated_topic, 60) || null,
    context_summary: text(out.context_summary, 400) || null,
  }
}

/** Default model order: the fast lite model first, the full one when the lite model is busy. */
export const DEFAULT_GEMINI_MODELS = 'gemini-flash-lite-latest,gemini-flash-latest'

/** Any model error but a rejected key is worth trying the next model for: busy, slow, or this model refuses the request. */
const retryable = (e) => e?.name === 'TimeoutError' || (e instanceof UpstreamError && e.status !== 401 && e.status !== 403)

/**
 * Calls Gemini, trying each model in `model` (comma-separated) in turn while the time budget lasts.
 * The app stops waiting after 6 s, so the whole call stays inside `budgetMs`. A model that rejects the thinking
 * setting is remembered and called without it.
 */
export function createAudienceGenerator({ apiKey, model, fetchImpl = fetch, budgetMs = 5_000, now = Date.now }) {
  const models = String(model || DEFAULT_GEMINI_MODELS).split(',').map((m) => m.trim()).filter(Boolean)
  const noThinking = new Set()
  const urlFor = (m) => `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(m)}:generateContent`

  function call(m, request, timeoutMs) {
    return fetchImpl(urlFor(m), {
      method: 'POST',
      headers: { 'content-type': 'application/json', 'x-goog-api-key': apiKey },
      body: JSON.stringify(buildGeminiBody(request, { thinking: !noThinking.has(m) })),
      signal: AbortSignal.timeout(timeoutMs),
    })
  }

  async function generateWith(m, request, timeoutMs) {
    let res = await call(m, request, timeoutMs)
    // Some models refuse the thinking setting, not always saying so ("invalid argument"): retry once without it.
    if (res.status === 400 && !noThinking.has(m)) {
      await res.body?.cancel().catch(() => {})
      noThinking.add(m)
      res = await call(m, request, timeoutMs)
    }
    if (!res.ok) throw new UpstreamError('gemini', res.status, `${m}: ${await res.text().catch(() => '')}`)
    const body = await res.json()
    const part = body?.candidates?.[0]?.content?.parts?.find((p) => typeof p.text === 'string')
    if (!part) throw new UpstreamError('gemini', 502, `${m}: no text in response (${body?.candidates?.[0]?.finishReason ?? 'no candidate'})`)
    let parsed
    try {
      parsed = JSON.parse(part.text)
    } catch {
      throw new UpstreamError('gemini', 502, `${m}: response was not JSON`)
    }
    return shapeResponse(parsed, request.max_comments)
  }

  return async function generate(request) {
    const deadline = now() + budgetMs
    let lastError
    for (const [i, m] of models.entries()) {
      const remaining = deadline - now()
      if (remaining < 500) break
      // Leave the later models some time: no single attempt but the last may use more than 60 % of what is left.
      const timeoutMs = i === models.length - 1 ? remaining : Math.max(500, Math.round(remaining * 0.6))
      try {
        return await generateWith(m, request, timeoutMs)
      } catch (e) {
        lastError = e
        if (!retryable(e)) throw e
      }
    }
    throw lastError ?? new UpstreamError('gemini', 504, 'no time left to call a model')
  }
}
