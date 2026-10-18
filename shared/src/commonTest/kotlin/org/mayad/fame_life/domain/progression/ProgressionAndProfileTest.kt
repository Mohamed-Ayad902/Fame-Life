package org.mayad.fame_life.domain.progression

import org.mayad.fame_life.domain.profile.ProfileRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FameProgressionTest {
    @Test
    fun levelsFollowThresholds() {
        assertEquals(FameLevel.NEW_CREATOR, FameProgression.levelFor(0))
        assertEquals(FameLevel.RISING_CREATOR, FameProgression.levelFor(600))
        assertEquals(FameLevel.SUPERSTAR, FameProgression.levelFor(1_000_000))
    }

    @Test
    fun progressReportsPointsToNextLevel() {
        val p = FameProgression.progress(300)
        assertEquals(300, p.pointsToNext)
        assertEquals(0.5f, p.fraction)
        assertNull(FameProgression.progress(50_000).pointsToNext)
    }

    @Test
    fun sessionPointsRewardTimeWithDiminishingReturnsAndAreCapped() {
        val short = FameProgression.pointsFor(10 * 60, 500, 30, 50, 1)
        val long = FameProgression.pointsFor(20 * 60, 500, 30, 50, 1)
        val marathon = FameProgression.pointsFor(200 * 60, 500, 30, 50, 1)
        assertTrue(long > short)
        assertTrue(marathon - long < (long - short) * 10)
        assertTrue(FameProgression.pointsFor(10_000 * 60, 1_000_000, 10_000, 100_000, 50) <= FameProgression.SESSION_CAP)
    }
}

class ProfileRulesTest {
    @Test
    fun usernames() {
        assertNull(ProfileRules.usernameError("@Mo.Ayad_7"))
        assertEquals("mo.ayad_7", ProfileRules.normalizeUsername("@Mo.Ayad_7 "))
        assertNotNull(ProfileRules.usernameError("ab"))
        assertNotNull(ProfileRules.usernameError("has space"))
        assertNotNull(ProfileRules.usernameError("double..dot"))
        assertNotNull(ProfileRules.usernameError("pulse"))
        assertNotNull(ProfileRules.usernameError("a".repeat(21)))
    }

    @Test
    fun displayNames() {
        assertNull(ProfileRules.displayNameError("رانيا"))
        assertNotNull(ProfileRules.displayNameError("   "))
    }
}
