package org.mayad.fame_life.domain.progression

import org.mayad.fame_life.domain.simulation.SessionSummary

/** Collectible badges, derived on the device from progress. They unlock nothing paid. Display text lives in `badge_*`. */
enum class Achievement { FIRST_LIVE, CROWD, GIFTED, STORYTELLER, REGULAR, RISING }

data class AchievementState(val achievement: Achievement, val unlocked: Boolean)

object Achievements {
    const val CROWD_PEAK = 1_000
    const val STORYTELLER_SECONDS = 600
    const val REGULAR_LIVES = 5
    private val MAJOR_GIFTS = setOf("crown", "galaxy")

    fun evaluate(points: Int, sessions: List<SessionSummary>): List<AchievementState> = Achievement.entries.map { achievement ->
        val unlocked = when (achievement) {
            Achievement.FIRST_LIVE -> sessions.isNotEmpty()
            Achievement.CROWD -> sessions.any { it.peakViewers >= CROWD_PEAK }
            Achievement.GIFTED -> sessions.any { it.topGiftId in MAJOR_GIFTS }
            Achievement.STORYTELLER -> sessions.any { it.durationSeconds >= STORYTELLER_SECONDS }
            Achievement.REGULAR -> sessions.size >= REGULAR_LIVES
            Achievement.RISING -> FameProgression.levelFor(points) >= FameLevel.RISING_CREATOR
        }
        AchievementState(achievement, unlocked)
    }
}
