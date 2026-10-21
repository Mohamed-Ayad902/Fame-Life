package org.mayad.fame_life.data.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mayad.fame_life.domain.ai.AudienceTransport
import org.mayad.fame_life.domain.ai.Wav

/**
 * The trusted Pulse backend (see backend/README.md). It holds the AI provider keys; the app only sends speech and
 * session context with the user's Firebase ID token.
 */
interface PulseBackend {
    /** Posts [body] to [path] and returns the response body. Throws [BackendException] on an HTTP error. */
    suspend fun post(path: String, body: ByteArray, contentType: String): String
}

class BackendException(val status: Int) : Exception("Backend answered HTTP $status")

/** The signed-in user's Firebase ID token, refreshed by the SDK when it's close to expiring. */
suspend fun firebaseIdToken(forceRefresh: Boolean): String? = Firebase.auth.currentUser?.getIdToken(forceRefresh)

@Serializable
private data class TranscriptionDto(val text: String = "", val language: String? = null)

private val json = Json { ignoreUnknownKeys = true }

/** Sends one utterance to the server's Whisper route and returns the text (empty when it held no words). */
suspend fun PulseBackend.transcribe(samples: ShortArray): String {
    val body = post("/v1/transcribe", Wav.encode(samples), "audio/wav")
    return json.decodeFromString(TranscriptionDto.serializer(), body).text.trim()
}

fun PulseBackend.audienceTransport(): AudienceTransport =
    AudienceTransport { requestJson -> post("/v1/audience", requestJson.encodeToByteArray(), "application/json") }
