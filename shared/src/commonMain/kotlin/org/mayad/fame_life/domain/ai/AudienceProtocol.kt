package org.mayad.fame_life.domain.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The contract between the app and whatever generates audience reactions (the trusted backend calling an LLM,
 * or the on-device fallback). Bump [PROTOCOL_VERSION] when either side changes shape.
 */
const val PROTOCOL_VERSION = 1

enum class GenerationTrigger { UTTERANCE, QUESTION, TOPIC_SHIFT, IDLE, OPENING }

@Serializable
data class AudienceRequest(
    @SerialName("protocol_version") val protocolVersion: Int = PROTOCOL_VERSION,
    @SerialName("session_id") val sessionId: String,
    @SerialName("request_seq") val requestSeq: Int,
    val trigger: String,
    val language: String,
    val vibes: List<String>,
    @SerialName("haters_mode") val hatersMode: Boolean,
    @SerialName("stream_title") val streamTitle: String,
    /** Most recent settled speech, oldest first. Bounded by the context buffer. */
    @SerialName("recent_speech") val recentSpeech: List<String>,
    @SerialName("context_summary") val contextSummary: String,
    val topic: String,
    @SerialName("recent_comments") val recentComments: List<String>,
    @SerialName("elapsed_seconds") val elapsedSeconds: Int,
    @SerialName("viewer_count") val viewerCount: Int,
    @SerialName("max_comments") val maxComments: Int,
)

@Serializable
data class AudienceResponseDto(
    val comments: List<CommentDto> = emptyList(),
    val reactions: List<String> = emptyList(),
    @SerialName("suggested_event") val suggestedEvent: String? = null,
    @SerialName("event_reason") val eventReason: String? = null,
    @SerialName("engagement_intensity") val engagementIntensity: Double = 0.5,
    @SerialName("updated_topic") val updatedTopic: String? = null,
    @SerialName("context_summary") val contextSummary: String? = null,
)

@Serializable
data class CommentDto(
    val text: String,
    val tone: String = "neutral",
)
