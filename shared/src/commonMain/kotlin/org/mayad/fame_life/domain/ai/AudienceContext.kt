package org.mayad.fame_life.domain.ai

import org.mayad.fame_life.domain.simulation.SimulationConfig

/**
 * The bounded memory the generator sees. It keeps only recent speech plus a rolling summary returned by the
 * generator, so request size stays flat no matter how long the session runs.
 */
class AudienceContext(
    private val sessionId: String,
    private val config: SimulationConfig,
    private val maxSpeech: Int = 5,
    private val maxComments: Int = 8,
) {
    private val speech = ArrayDeque<String>()
    private val comments = ArrayDeque<String>()
    private var requestSeq = 0

    var topic: String = config.title.ifBlank { "just chatting" }
        private set
    var summary: String = ""
        private set

    val latestSpeech: String? get() = speech.lastOrNull()
    val hasSpeech: Boolean get() = speech.isNotEmpty()

    fun addSpeech(text: String) {
        speech.addLast(text.take(MAX_UTTERANCE_CHARS))
        while (speech.size > maxSpeech) speech.removeFirst()
    }

    fun addAudienceComment(text: String) {
        comments.addLast(text)
        while (comments.size > maxComments) comments.removeFirst()
    }

    fun absorb(response: AudienceResponse) {
        response.topic?.let { topic = it }
        response.contextSummary?.let { summary = it }
    }

    fun buildRequest(trigger: GenerationTrigger, elapsedSeconds: Int, viewerCount: Int, maxComments: Int): AudienceRequest =
        AudienceRequest(
            sessionId = sessionId,
            requestSeq = ++requestSeq,
            trigger = trigger.name.lowercase(),
            language = config.language.name.lowercase(),
            vibes = config.vibes.map { it.name.lowercase() }.sorted(),
            hatersMode = config.hatersMode,
            streamTitle = config.title,
            recentSpeech = speech.toList(),
            contextSummary = summary,
            topic = topic,
            recentComments = comments.toList(),
            elapsedSeconds = elapsedSeconds,
            viewerCount = viewerCount,
            maxComments = maxComments,
        )

    private companion object {
        const val MAX_UTTERANCE_CHARS = 400
    }
}
