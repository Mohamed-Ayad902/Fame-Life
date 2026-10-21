/** A provider (Groq, Gemini) answered with an error. The body is kept for server logs only, never sent to clients. */
export class UpstreamError extends Error {
  constructor(provider, status, body) {
    super(`${provider} returned ${status}`)
    this.provider = provider
    this.status = status
    this.body = String(body).slice(0, 500)
  }
}
