package org.mayad.fame_life.domain.progression

import org.mayad.fame_life.domain.simulation.SessionSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementsTest {
    private fun session(i: Int, peak: Int = 10, seconds: Int = 60, gift: String? = null) = SessionSummary(
        id = "s$i", title = "", startedAtEpochMs = 0, durationSeconds = seconds, preset = "", liveStyleId = "",
        peakViewers = peak, comments = 0, contextualComments = 0, reactions = 0, gifts = 0, giftPoints = 0, milestones = 0,
        topGiftId = gift, famePointsEarned = 0,
    )

    @Test
    fun newCreatorHasEveryBadgeLocked() {
        val states = Achievements.evaluate(0, emptyList())
        assertEquals(Achievement.entries.size, states.size)
        assertTrue(states.none { it.unlocked })
    }

    @Test
    fun eachRuleUnlocksItsOwnBadge() {
        val sessions = listOf(session(1, peak = Achievements.CROWD_PEAK, seconds = Achievements.STORYTELLER_SECONDS, gift = "crown"))
        val unlocked = Achievements.evaluate(FameLevel.RISING_CREATOR.threshold, sessions).filter { it.unlocked }.map { it.achievement }.toSet()
        assertEquals(Achievement.entries.toSet() - Achievement.REGULAR, unlocked)
        val regular = Achievements.evaluate(0, List(Achievements.REGULAR_LIVES) { session(it) }).first { it.achievement == Achievement.REGULAR }
        assertTrue(regular.unlocked)
    }
}
