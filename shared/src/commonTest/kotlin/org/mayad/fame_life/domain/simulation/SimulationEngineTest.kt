package org.mayad.fame_life.domain.simulation

import org.mayad.fame_life.domain.ai.AudienceResponse
import org.mayad.fame_life.domain.entitlement.PlanLimits
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewerCountEngineTest {
    private fun run(seed: Long, seconds: Int, engagement: Double): List<Int> {
        val engine = ViewerCountEngine(AudiencePreset.RISING, Random(seed))
        return (1..seconds).map { engine.step(it, engagement) }
    }

    @Test
    fun sameSeedGivesSameSequence() {
        assertEquals(run(7, 120, 0.5), run(7, 120, 0.5))
    }

    @Test
    fun countGrowsTowardThePresetAndStaysInBounds() {
        val counts = run(3, 600, 0.6)
        assertTrue(counts.last() > counts.first(), "audience should grow over a session")
        assertTrue(counts.all { it in 1..(AudiencePreset.RISING.targetRange.last * 2) })
    }

    @Test
    fun countChangesSmoothlyWithoutJumps() {
        val counts = run(11, 300, 0.5)
        counts.zipWithNext().forEach { (a, b) ->
            assertTrue(kotlin.math.abs(b - a) <= maxOf(40, a / 5), "jump from $a to $b")
        }
    }

    @Test
    fun engagedSessionsOutgrowQuietOnes() {
        assertTrue(run(5, 400, 0.95).last() > run(5, 400, 0.05).last())
    }

    @Test
    fun spikeRaisesThenDecays() {
        val engine = ViewerCountEngine(AudiencePreset.RISING, Random(1))
        repeat(200) { engine.step(it, 0.5) }
        val before = engine.current
        engine.spike(1.0)
        repeat(10) { engine.step(200 + it, 0.5) }
        val during = engine.current
        repeat(200) { engine.step(210 + it, 0.5) }
        assertTrue(during > before)
        assertTrue(engine.peak >= during)
    }

    @Test
    fun milestonesReportTheHighestRoundNumberCrossed() {
        assertEquals(1_000, ViewerMilestones.crossed(480, 1_020))
        assertNull(ViewerMilestones.crossed(1_020, 1_100))
    }
}

class CommentPacerTest {
    @Test
    fun releasesOneCommentPerSlot() {
        val pacer = CommentPacer(EventFrequency.CALM)
        pacer.offer(listOf(PendingComment("first one", CommentTone.NEUTRAL, true), PendingComment("second one here", CommentTone.NEUTRAL, true)))
        val random = Random(0)
        assertNotNull(pacer.poll(0, random))
        assertNull(pacer.poll(100, random), "second comment must wait for its slot")
        assertNotNull(pacer.poll(5_000, random))
    }

    @Test
    fun rejectsDuplicatesAndNearDuplicates() {
        val pacer = CommentPacer(EventFrequency.LIVELY)
        val accepted = pacer.offer(
            listOf(
                PendingComment("LOVE this energy!!", CommentTone.SUPPORTIVE, true),
                PendingComment("love this energy", CommentTone.SUPPORTIVE, true),
                PendingComment("love this energy 🔥", CommentTone.SUPPORTIVE, true),
                PendingComment("tell us about the trip", CommentTone.CURIOUS, true),
            ),
        )
        assertEquals(2, accepted)
    }

    @Test
    fun pendingQueueIsBounded() {
        val pacer = CommentPacer(EventFrequency.LIVELY, maxPending = 3)
        pacer.offer((1..10).map { PendingComment("comment number $it is unique", CommentTone.NEUTRAL, true) })
        assertEquals(3, pacer.pendingCount)
    }
}

class EventPolicyTest {
    private val config = SimulationConfig(frequency = EventFrequency.LIVELY)

    @Test
    fun nothingHappensDuringWarmup() {
        val policy = EventPolicy(config, PlanLimits.Pro)
        assertNull(policy.decide(EventSuggestion.MEDIUM_GIFT, 1_000, 0.9, Random(0)))
    }

    @Test
    fun giftsRespectCooldowns() {
        val policy = EventPolicy(config, PlanLimits.Pro)
        val random = Random(0)
        assertNotNull(policy.decide(EventSuggestion.MEDIUM_GIFT, 30_000, 0.6, random))
        assertNull(policy.decide(EventSuggestion.MEDIUM_GIFT, 31_000, 0.6, random))
        assertNotNull(policy.decide(EventSuggestion.MEDIUM_GIFT, 30_000 + EventPolicy.MEDIUM_COOLDOWN_MS, 0.6, random))
    }

    @Test
    fun majorGiftNeedsIntensityAndIsDowngradedOtherwise() {
        val policy = EventPolicy(config, PlanLimits.Pro)
        val event = policy.decide(EventSuggestion.MAJOR_GIFT, 70_000, 0.3, Random(0))
        assertTrue(event is ApprovedEvent.SendGift && event.gift.tier == GiftTier.MEDIUM)
    }

    @Test
    fun freePlanNeverReceivesMajorGifts() {
        val policy = EventPolicy(config, PlanLimits.Free)
        val event = policy.decide(EventSuggestion.MAJOR_GIFT, 120_000, 1.0, Random(0))
        assertTrue(event is ApprovedEvent.SendGift && event.gift.tier != GiftTier.MAJOR)
    }

    @Test
    fun disabledGiftsBlockEveryGift() {
        val policy = EventPolicy(config.copy(giftsEnabled = false), PlanLimits.Pro)
        assertNull(policy.decide(EventSuggestion.SMALL_GIFT, 30_000, 1.0, Random(0)))
        assertNull(policy.ambient(30_000, 1.0, Random(0)))
    }

    @Test
    fun giftsPerMinuteAreCapped() {
        val policy = EventPolicy(config, PlanLimits.Pro)
        val random = Random(0)
        val accepted = (0 until 20).count { i ->
            policy.decide(EventSuggestion.SMALL_GIFT, 10_000L + i * EventPolicy.SMALL_COOLDOWN_MS / 2, 1.0, random) != null
        }
        assertTrue(accepted <= EventPolicy.MAX_GIFTS_PER_MINUTE)
    }

    @Test
    fun trendingNeedsTimeAndIntensity() {
        val policy = EventPolicy(config, PlanLimits.Pro)
        assertNull(policy.decide(EventSuggestion.TRENDING, 20_000, 1.0, Random(0)))
        assertNull(policy.decide(EventSuggestion.TRENDING, 60_000, 0.4, Random(0)))
        assertEquals(ApprovedEvent.Trending, policy.decide(EventSuggestion.TRENDING, 60_000, 0.9, Random(0)))
    }
}

class LiveSessionEngineTest {
    private fun response(vararg comments: String, suggestion: EventSuggestion = EventSuggestion.NONE, intensity: Double = 0.6) =
        AudienceResponse(
            comments = comments.map { PendingComment(it, CommentTone.SUPPORTIVE, contextual = true) },
            reactions = emptyList(),
            suggestion = suggestion,
            eventReason = null,
            intensity = intensity,
            topic = null,
            contextSummary = null,
        )

    @Test
    fun visibleCommentsAreBoundedButTotalsKeepCounting() {
        val engine = LiveSessionEngine(SimulationConfig(seed = 1), PlanLimits.Free, maxVisibleComments = 3)
        engine.start(0)
        var now = 0L
        repeat(6) { batch ->
            engine.apply(response("batch $batch first comment", "batch $batch second comment"), fromFallback = false)
            repeat(40) {
                now += 250
                engine.tick(now)
            }
        }
        assertEquals(3, engine.snapshot.comments.size)
        assertEquals(12, engine.snapshot.totals.comments)
        assertEquals(12, engine.snapshot.totals.contextualComments)
    }

    @Test
    fun pausedTimeDoesNotCount() {
        val engine = LiveSessionEngine(SimulationConfig(seed = 1), PlanLimits.Free)
        engine.start(0)
        engine.tick(10_000)
        engine.pause(10_000)
        engine.tick(60_000)
        engine.resume(60_000)
        engine.tick(65_000)
        assertEquals(15, engine.snapshot.elapsedSeconds)
    }

    @Test
    fun finishIsIdempotentAndSummaryMatchesSnapshot() {
        val engine = LiveSessionEngine(SimulationConfig(seed = 2), PlanLimits.Free)
        engine.start(0)
        var now = 0L
        repeat(300) {
            now += 200
            engine.tick(now)
        }
        val summary = engine.finish(now, "s1", startedAtEpochMs = 1_000)
        assertNotNull(summary)
        assertEquals(60, summary.durationSeconds)
        assertEquals(engine.snapshot.peakViewers, summary.peakViewers)
        assertEquals(engine.snapshot.totals.gifts, summary.gifts)
        assertNull(engine.finish(now + 1_000, "s1", 1_000))
    }

    @Test
    fun responsesAfterTheSessionEndedAreIgnored() {
        val engine = LiveSessionEngine(SimulationConfig(seed = 3), PlanLimits.Free)
        engine.start(0)
        engine.tick(5_000)
        engine.finish(5_000, "s", 0)
        engine.apply(response("too late"), fromFallback = false)
        engine.tick(10_000)
        assertEquals(0, engine.snapshot.totals.comments)
    }

    @Test
    fun configIsClampedToThePlan() {
        val config = SimulationConfig(
            preset = AudiencePreset.VIRAL,
            vibes = setOf(AudienceVibe.CRITICAL),
            hatersMode = true,
            liveStyleId = "noir",
            maxDurationSeconds = 3 * 60 * 60,
        ).constrainedTo(PlanLimits.Free)
        assertEquals(AudiencePreset.RISING, config.preset)
        assertEquals(setOf(AudienceVibe.SUPPORTIVE), config.vibes)
        assertEquals(false, config.hatersMode)
        assertEquals("aurora", config.liveStyleId)
        assertEquals(PlanLimits.Free.maxSessionSeconds, config.maxDurationSeconds)
    }
}
