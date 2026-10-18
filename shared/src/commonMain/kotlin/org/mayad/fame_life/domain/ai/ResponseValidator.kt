package org.mayad.fame_life.domain.ai

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.mayad.fame_life.domain.simulation.CommentTone
import org.mayad.fame_life.domain.simulation.EventSuggestion
import org.mayad.fame_life.domain.simulation.PendingComment
import org.mayad.fame_life.domain.simulation.ReactionKind

/** A generator response after validation. Only this type reaches the simulation. */
data class AudienceResponse(
    val comments: List<PendingComment>,
    val reactions: List<ReactionKind>,
    val suggestion: EventSuggestion,
    val eventReason: String?,
    val intensity: Double,
    val topic: String?,
    val contextSummary: String?,
)

class InvalidAudienceResponse(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Validates untrusted generator output. Anything malformed is rejected as a whole; individual comments that
 * break content rules are dropped. Nothing here can trigger an action by itself: suggestions still go through
 * the event policy.
 */
class ResponseValidator(
    private val maxComments: Int = 6,
    private val maxCommentLength: Int = 140,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(raw: String, hatersMode: Boolean): AudienceResponse {
        val dto = try {
            json.decodeFromString(AudienceResponseDto.serializer(), extractJsonObject(raw))
        } catch (e: SerializationException) {
            throw InvalidAudienceResponse("Response is not valid audience JSON", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidAudienceResponse("Response is not valid audience JSON", e)
        }
        return validate(dto, hatersMode)
    }

    fun validate(dto: AudienceResponseDto, hatersMode: Boolean): AudienceResponse {
        val comments = dto.comments.asSequence()
            .mapNotNull { comment ->
                val text = clean(comment.text) ?: return@mapNotNull null
                val tone = toneOf(comment.tone)
                if (tone == CommentTone.CRITICAL && !hatersMode) return@mapNotNull null
                PendingComment(text, tone, contextual = true)
            }
            .take(maxComments)
            .toList()
        return AudienceResponse(
            comments = comments,
            reactions = dto.reactions.mapNotNull { reactionOf(it) }.take(5),
            suggestion = suggestionOf(dto.suggestedEvent),
            eventReason = dto.eventReason?.let { clean(it) },
            intensity = dto.engagementIntensity.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.5,
            topic = dto.updatedTopic?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
            contextSummary = dto.contextSummary?.trim()?.take(400)?.takeIf { it.isNotEmpty() },
        )
    }

    private fun clean(text: String): String? {
        val trimmed = text.replace(Regex("\\s+"), " ").trim()
        if (trimmed.length < 2 || trimmed.length > maxCommentLength) return null
        val lower = trimmed.lowercase()
        if (urlPattern.containsMatchIn(lower) || contactPattern.containsMatchIn(lower)) return null
        if (blockedPattern.containsMatchIn(lower)) return null
        return trimmed
    }

    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw InvalidAudienceResponse("No JSON object in response")
        return raw.substring(start, end + 1)
    }

    companion object {
        private val urlPattern = Regex("(https?://|www\\.|\\.com\\b|\\.net\\b|\\.io\\b)")
        private val contactPattern = Regex("(\\b[\\w.]+@[\\w.]+\\b|\\+?\\d[\\d\\s-]{7,}\\d)")

        /** Self-harm encouragement and slurs never reach the screen, even in haters mode. Extended server-side. */
        private val blockedPattern = Regex("\\b(kill yourself|kys|go die|retard\\w*|end your life)\\b")

        fun toneOf(value: String): CommentTone =
            CommentTone.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: CommentTone.NEUTRAL

        fun reactionOf(value: String): ReactionKind? =
            ReactionKind.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

        fun suggestionOf(value: String?): EventSuggestion =
            EventSuggestion.entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: EventSuggestion.NONE
    }
}
