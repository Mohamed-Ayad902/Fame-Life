package org.mayad.fame_life.domain.ai

import org.mayad.fame_life.domain.simulation.CommentPacer

/** What a speech recognizer reports. Partial results are replaced by later ones; final results are settled. */
data class TranscriptUpdate(val text: String, val isFinal: Boolean, val atMs: Long)

data class Utterance(val text: String, val atMs: Long)

/**
 * Turns a noisy stream of partial and final recognizer results into distinct utterances.
 *
 * Recognizers often repeat a final result, finalize text that is a prefix of the previous one, or stop
 * on a partial without finalizing it. This buffer settles each phrase once, and promotes a partial
 * that has been stable for [stablePartialMs] so speech is not lost.
 */
class TranscriptBuffer(
    private val stablePartialMs: Long = 2_500L,
    private val maxUtterances: Int = 24,
) {
    private val settled = ArrayDeque<Utterance>()
    private var partial: TranscriptUpdate? = null
    private var lastSettledKey = ""

    val utterances: List<Utterance> get() = settled.toList()
    val livePartial: String? get() = partial?.text

    /** Returns the newly settled utterance, if this update settled one. */
    fun accept(update: TranscriptUpdate): Utterance? {
        val text = update.text.trim()
        if (text.isEmpty()) return null
        if (!update.isFinal) {
            if (partial?.text != text) partial = update
            return null
        }
        partial = null
        return settle(text, update.atMs)
    }

    /** Called periodically; settles a partial that stopped changing. */
    fun flushStale(nowMs: Long): Utterance? {
        val current = partial ?: return null
        if (nowMs - current.atMs < stablePartialMs) return null
        partial = null
        return settle(current.text, nowMs)
    }

    fun clear() {
        settled.clear()
        partial = null
        lastSettledKey = ""
    }

    private fun settle(text: String, atMs: Long): Utterance? {
        val key = CommentPacer.normalize(text)
        if (key.isEmpty() || key == lastSettledKey || lastSettledKey.endsWith(key)) return null
        // A final that extends the previous one replaces it instead of counting twice.
        if (lastSettledKey.isNotEmpty() && key.startsWith(lastSettledKey)) settled.removeLastOrNull()
        lastSettledKey = key
        val utterance = Utterance(text, atMs)
        settled.addLast(utterance)
        while (settled.size > maxUtterances) settled.removeFirst()
        return utterance
    }
}
