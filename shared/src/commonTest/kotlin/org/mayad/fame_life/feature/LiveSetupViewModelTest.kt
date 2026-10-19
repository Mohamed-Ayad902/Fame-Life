package org.mayad.fame_life.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mayad.fame_life.core.analytics.InMemoryAnalytics
import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.live.LiveSetupEvent
import org.mayad.fame_life.feature.live.LiveSetupIntent
import org.mayad.fame_life.feature.live.LiveSetupViewModel
import org.mayad.fame_life.feature.live.ProHint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LiveSetupViewModelTest : ViewModelTest() {
    private val day = 24 * 3_600_000L
    private val now = 10 * day

    private fun summary(id: String, startedAt: Long) = SessionSummary(
        id = id, title = "t", startedAtEpochMs = startedAt, durationSeconds = 60, preset = "RISING", liveStyleId = "aurora",
        peakViewers = 100, comments = 10, contextualComments = 8, reactions = 5, gifts = 1, giftPoints = 5,
        milestones = 0, famePointsEarned = 50,
    )

    private val history = SessionHistoryRepository(InMemoryTextStore()) { _, _ -> }
    private val profiles = ProfileRepository(InMemoryTextStore()) { _, _ -> }.apply {
        save(CreatorProfile(displayName = "Mo", username = "mo", createdAtEpochMs = 1))
    }

    private fun viewModel() = LiveSetupViewModel("", profiles, history, EntitlementRepository(), InMemoryAnalytics()) { now }

    @Test
    fun livesLeftCountsOnlyTheLast24Hours() {
        val limits = PlanLimits.Free
        val sessions = listOf(summary("old", now - day - 1), summary("a", now - 1_000), summary("b", now - day + 1))
        assertEquals(limits.sessionsPerDay - 2, LiveSetupViewModel.livesLeft(limits, sessions, now))
        assertEquals(0, LiveSetupViewModel.livesLeft(limits, List(10) { summary("$it", now) }, now))
    }

    @Test
    fun lockedOptionsShowAHintInsteadOfChangingTheDraft() {
        val vm = viewModel()
        val locked = AudiencePreset.entries.first { it !in PlanLimits.Free.presets }
        val before = vm.state.value.draft.preset
        vm.onIntent(LiveSetupIntent.Preset(locked))
        assertEquals(before, vm.state.value.draft.preset)
        assertEquals(ProHint.Preset(locked), vm.state.value.proHint)
    }

    @Test
    fun theLastMoodCannotBeRemoved() {
        val vm = viewModel()
        vm.state.value.draft.vibes.forEach { vm.onIntent(LiveSetupIntent.ToggleVibe(it)) }
        assertEquals(1, vm.state.value.draft.vibes.size)
        assertTrue(vm.state.value.draft.vibes.first() in AudienceVibe.entries)
    }

    @Test
    fun goLiveStartsWithAConfigWithinThePlan() = runTest {
        val vm = viewModel()
        vm.onIntent(LiveSetupIntent.Title("  Story time  "))
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        val start = assertIs<LiveSetupEvent.Start>(vm.events.first())
        assertEquals("Story time", start.config.title)
        assertTrue(start.config.maxDurationSeconds <= PlanLimits.Free.maxSessionSeconds)
    }

    @Test
    fun goLiveIsBlockedWhenNoLivesAreLeft() {
        repeat(PlanLimits.Free.sessionsPerDay) { history.save(summary("s$it", now), keep = 50) }
        val vm = viewModel()
        assertEquals(0, vm.state.value.livesLeft)
        assertTrue(!vm.state.value.canGoLive)
    }
}
