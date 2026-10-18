package org.mayad.fame_life.domain.simulation

import kotlin.random.Random

/** A comment that has been generated and validated, waiting for its turn on screen. */
data class PendingComment(val text: String, val tone: CommentTone, val contextual: Boolean)

/**
 * Releases comments at a human pace instead of dumping a batch at once, and rejects near-duplicates.
 * The pending queue is bounded: when the AI produces more than the stream can show, the oldest drop first,
 * because stale comments read as if the audience is not listening.
 */
class CommentPacer(
    private val frequency: EventFrequency,
    private val maxPending: Int = 8,
    private val memory: Int = 60,
) {
    private val pending = ArrayDeque<PendingComment>()
    private val recent = ArrayDeque<String>()
    private var nextReleaseAtMs = 0L

    val pendingCount: Int get() = pending.size

    /** Returns how many of [comments] were accepted. */
    fun offer(comments: List<PendingComment>): Int {
        var accepted = 0
        for (comment in comments) {
            val key = normalize(comment.text)
            if (key.isEmpty() || isDuplicate(key)) continue
            remember(key)
            pending.addLast(comment)
            accepted++
            while (pending.size > maxPending) pending.removeFirst()
        }
        return accepted
    }

    /** Pops the next comment when its slot has come up, otherwise null. */
    fun poll(nowMs: Long, random: Random): PendingComment? {
        if (nowMs < nextReleaseAtMs) return null
        val next = pending.removeFirstOrNull() ?: return null
        nextReleaseAtMs = nowMs + frequency.commentGapMs.random(random)
        return next
    }

    fun clear() {
        pending.clear()
    }

    private fun isDuplicate(key: String): Boolean {
        val words = key.split(' ').toSet()
        return recent.any { previous ->
            previous == key || jaccard(words, previous.split(' ').toSet()) >= 0.8
        }
    }

    private fun remember(key: String) {
        recent.addLast(key)
        while (recent.size > memory) recent.removeFirst()
    }

    companion object {
        fun normalize(text: String): String = text.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ")

        private fun jaccard(a: Set<String>, b: Set<String>): Double {
            if (a.isEmpty() || b.isEmpty()) return 0.0
            return (a intersect b).size.toDouble() / (a union b).size
        }
    }
}
