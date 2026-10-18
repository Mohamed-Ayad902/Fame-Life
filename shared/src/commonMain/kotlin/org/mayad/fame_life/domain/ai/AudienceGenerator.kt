package org.mayad.fame_life.domain.ai

import kotlinx.serialization.json.Json

/** Anything that turns session context into a validated audience response. */
fun interface AudienceGenerator {
    suspend fun generate(request: AudienceRequest): AudienceResponse
}

/**
 * Sends the request to the trusted backend, which holds the LLM credentials, enforces quotas server-side
 * and calls the provider. The app never talks to an LLM provider directly.
 */
fun interface AudienceTransport {
    /** Posts the request JSON and returns the raw response body. Throws on network or HTTP failure. */
    suspend fun post(requestJson: String): String
}

class RemoteAudienceGenerator(
    private val transport: AudienceTransport,
    private val validator: ResponseValidator = ResponseValidator(),
) : AudienceGenerator {
    private val json = Json { encodeDefaults = true }

    override suspend fun generate(request: AudienceRequest): AudienceResponse {
        val body = transport.post(json.encodeToString(AudienceRequest.serializer(), request))
        return validator.parse(body, hatersMode = request.hatersMode)
    }
}
