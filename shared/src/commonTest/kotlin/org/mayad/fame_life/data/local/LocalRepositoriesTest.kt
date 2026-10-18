package org.mayad.fame_life.data.local

import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.domain.simulation.SessionSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalRepositoriesTest {
    private fun summary(id: String) = SessionSummary(
        id = id, title = "t", startedAtEpochMs = 0, durationSeconds = 60, preset = "RISING", liveStyleId = "aurora",
        peakViewers = 100, comments = 10, contextualComments = 8, reactions = 5, gifts = 1, giftPoints = 5,
        milestones = 0, famePointsEarned = 50,
    )

    @Test
    fun profileSurvivesReloadAndReportsLevelUps() {
        val store = InMemoryTextStore()
        val repo = ProfileRepository(store) { _, _ -> }
        repo.save(CreatorProfile(displayName = "Mo", username = "mo", createdAtEpochMs = 1))
        assertNull(repo.recordSession(100))
        assertNotNull(repo.recordSession(600), "crossing 600 points reaches Rising Creator")
        val reloaded = ProfileRepository(store) { _, _ -> }
        assertEquals(700, reloaded.profile.value?.famePoints)
        assertEquals(2, reloaded.profile.value?.sessionsCompleted)
    }

    @Test
    fun historyIsIdempotentNewestFirstAndTrimmed() {
        val repo = SessionHistoryRepository(InMemoryTextStore()) { _, _ -> }
        repo.save(summary("a"), keep = 2)
        repo.save(summary("b"), keep = 2)
        repo.save(summary("b"), keep = 2)
        repo.save(summary("c"), keep = 2)
        assertEquals(listOf("c", "b"), repo.sessions.value.map { it.id })
    }

    @Test
    fun corruptDataIsReportedAndReset() {
        val store = InMemoryTextStore()
        store.write("sessions.v1", "{not json")
        var reported = false
        val repo = SessionHistoryRepository(store) { _, _ -> reported = true }
        assertTrue(reported)
        assertTrue(repo.sessions.value.isEmpty())
        assertNull(store.read("sessions.v1"))
    }
}
