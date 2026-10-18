package org.mayad.fame_life.domain.simulation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mayad.fame_life.domain.ai.AudienceGenerator
import org.mayad.fame_life.domain.ai.AudienceResponse
import org.mayad.fame_life.domain.ai.SpeechEvent
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.entitlement.PlanLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LiveSessionControllerTest {
    private class FakeSpeech : SpeechInput {
        val events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 16)
        var listening = false
        override fun listen(language: AudienceLanguage) = flow {
            listening = true
            emit(SpeechEvent.Status(SpeechStatus.LISTENING))
            events.collect { emit(it) }
        }.onCompletion { listening = false }
    }

    private fun TestScope.controller(
        speech: SpeechInput?,
        remote: AudienceGenerator?,
        config: SimulationConfig = SimulationConfig(seed = 9, maxDurationSeconds = 600),
    ) = LiveSessionController(
        sessionId = "test",
        config = config,
        limits = PlanLimits.Free,
        speechInput = speech,
        remote = remote,
        scope = backgroundScope,
        clock = { testScheduler.currentTime },
        epochClock = { 1_700_000_000_000 },
    )

    private val reply = AudienceResponse(
        comments = listOf(PendingComment("tell us more about the trip", CommentTone.CURIOUS, true)),
        reactions = emptyList(),
        suggestion = EventSuggestion.NONE,
        eventReason = null,
        intensity = 0.7,
        topic = "travel",
        contextSummary = "creator is talking about a trip",
    )

    @Test
    fun speechTriggersTheRemoteGeneratorAndItsCommentsAppear() = runTest {
        val speech = FakeSpeech()
        val requests = mutableListOf<String>()
        val session = controller(speech, AudienceGenerator { request ->
            requests += request.recentSpeech.last()
            reply
        })
        session.start()
        runCurrent()
        speech.events.emit(SpeechEvent.Transcript("I just got back from a trip to Japan", isFinal = true))
        advanceTimeBy(10_000)
        assertEquals(listOf("I just got back from a trip to Japan"), requests)
        assertTrue(session.state.value.live.comments.any { it.text == "tell us more about the trip" })
        assertEquals(SpeechStatus.LISTENING, session.state.value.speech)
    }

    @Test
    fun failingProviderFallsBackOnDeviceAndSessionContinues() = runTest {
        val speech = FakeSpeech()
        val session = controller(speech, AudienceGenerator { error("provider down") })
        session.start()
        runCurrent()
        speech.events.emit(SpeechEvent.Transcript("what is everyone eating tonight?", isFinal = true))
        advanceTimeBy(10_000)
        val totals = session.state.value.live.totals
        assertEquals(1, totals.aiFailures)
        assertEquals(1, totals.fallbackResponses)
        assertTrue(session.state.value.live.comments.size >= 2, "opening line plus fallback replies")
    }

    @Test
    fun slowProviderTimesOutIntoFallback() = runTest {
        val speech = FakeSpeech()
        val session = controller(speech, AudienceGenerator { awaitCancellation() })
        session.start()
        runCurrent()
        speech.events.emit(SpeechEvent.Transcript("does anyone here play guitar?", isFinal = true))
        advanceTimeBy(8_000)
        assertEquals(1, session.state.value.live.totals.fallbackResponses)
    }

    @Test
    fun endingCancelsWorkReleasesTheMicAndIsIdempotent() = runTest {
        val speech = FakeSpeech()
        val pending = CompletableDeferred<AudienceResponse>()
        val session = controller(speech, AudienceGenerator { pending.await() })
        session.start()
        runCurrent()
        speech.events.emit(SpeechEvent.Transcript("hello everyone welcome to the stream", isFinal = true))
        advanceTimeBy(30_000)
        val summary = session.end()
        runCurrent()
        assertNotNull(summary)
        assertEquals(30, summary.durationSeconds)
        assertTrue(!speech.listening, "microphone must be released")
        val commentsAtEnd = session.state.value.live.totals.comments
        pending.complete(reply)
        advanceTimeBy(10_000)
        assertEquals(commentsAtEnd, session.state.value.live.totals.comments, "late responses are dropped")
        assertSame(summary, session.end())
    }

    @Test
    fun sessionEndsItselfAtTheTimeLimit() = runTest {
        val session = controller(null, null, SimulationConfig(seed = 1, maxDurationSeconds = 60))
        session.start()
        advanceTimeBy(61_000)
        val summary = session.state.value.summary
        assertNotNull(summary)
        assertEquals(60, summary.durationSeconds)
        assertEquals(SessionStatus.COMPLETED, session.state.value.live.status)
    }

    @Test
    fun mutingStopsListeningAndUnmutingRestarts() = runTest {
        val speech = FakeSpeech()
        val session = controller(speech, null)
        session.start()
        runCurrent()
        assertTrue(speech.listening)
        session.setMicMuted(true)
        runCurrent()
        assertTrue(!speech.listening)
        session.setMicMuted(false)
        runCurrent()
        assertTrue(speech.listening)
    }

    @Test
    fun onDeviceAudienceStillReactsWithoutABackend() = runTest {
        val speech = FakeSpeech()
        val session = controller(speech, null)
        session.start()
        runCurrent()
        speech.events.emit(SpeechEvent.Transcript("haha my cat just knocked over my coffee", isFinal = true))
        advanceTimeBy(12_000)
        assertEquals(AiActivity.ON_DEVICE, session.state.value.ai)
        assertTrue(session.state.value.live.totals.contextualComments >= 3)
    }
}
