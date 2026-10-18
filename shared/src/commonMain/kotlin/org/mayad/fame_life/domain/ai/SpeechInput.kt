package org.mayad.fame_life.domain.ai

import kotlinx.coroutines.flow.Flow
import org.mayad.fame_life.domain.simulation.AudienceLanguage

enum class SpeechStatus { STARTING, LISTENING, UNAVAILABLE, PERMISSION_DENIED, ERROR, OFF }

sealed interface SpeechEvent {
    data class Status(val status: SpeechStatus) : SpeechEvent
    data class Transcript(val text: String, val isFinal: Boolean) : SpeechEvent
}

/**
 * Platform speech recognition. Collecting [listen] starts the microphone and cancelling the collection stops it,
 * so the recognizer can never outlive the session that started it. Audio is never stored or uploaded by the app.
 */
fun interface SpeechInput {
    fun listen(language: AudienceLanguage): Flow<SpeechEvent>
}
