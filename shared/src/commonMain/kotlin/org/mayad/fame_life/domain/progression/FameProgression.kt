package org.mayad.fame_life.domain.progression

import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

/** Fictional in-app levels. They reward taking part; they are not proof of real-world popularity. */
enum class FameLevel(val title: String, val threshold: Int) {
    NEW_CREATOR("New Creator", 0),
    RISING_CREATOR("Rising Creator", 600),
    KNOWN_CREATOR("Known Creator", 2_200),
    INFLUENCER("Influencer", 6_500),
    CELEBRITY("Celebrity", 16_000),
    SUPERSTAR("Superstar", 40_000);

    val next: FameLevel? get() = entries.getOrNull(ordinal + 1)
}

data class LevelProgress(val level: FameLevel, val points: Int, val fraction: Float, val pointsToNext: Int?)

object FameProgression {
    fun levelFor(points: Int): FameLevel = FameLevel.entries.last { points >= it.threshold }

    fun progress(points: Int): LevelProgress {
        val level = levelFor(points)
        val next = level.next ?: return LevelProgress(level, points, 1f, null)
        val span = (next.threshold - level.threshold).toFloat()
        return LevelProgress(level, points, ((points - level.threshold) / span).coerceIn(0f, 1f), next.threshold - points)
    }

    /**
     * Points earned by one session. Time counts with diminishing returns past twenty minutes and the total is
     * capped, so progression rewards coming back rather than marathon sessions.
     */
    fun pointsFor(durationSeconds: Int, peakViewers: Int, contextualComments: Int, giftPoints: Int, milestones: Int): Int {
        val minutes = durationSeconds / 60.0
        val timePoints = if (minutes <= 20) minutes * 12 else 240 + (minutes - 20) * 3
        val audiencePoints = ln(1.0 + peakViewers) * 18
        val conversationPoints = min(contextualComments, 120) * 1.5
        val raw = timePoints + audiencePoints + conversationPoints + giftPoints * 0.5 + milestones * 25
        return min(raw.roundToInt(), SESSION_CAP)
    }

    const val SESSION_CAP = 1_500
}
