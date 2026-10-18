package org.mayad.fame_life.domain.ai

import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.CommentTone
import org.mayad.fame_life.domain.simulation.EventSuggestion
import org.mayad.fame_life.domain.simulation.SimulationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptBufferTest {
    @Test
    fun partialsDoNotSettleUntilFinal() {
        val buffer = TranscriptBuffer()
        assertNull(buffer.accept(TranscriptUpdate("so today", false, 0)))
        assertNull(buffer.accept(TranscriptUpdate("so today I want", false, 100)))
        assertEquals("so today I want to talk", buffer.accept(TranscriptUpdate("so today I want to talk", true, 200))?.text)
        assertNull(buffer.livePartial)
    }

    @Test
    fun repeatedFinalsSettleOnce() {
        val buffer = TranscriptBuffer()
        assertNotNull(buffer.accept(TranscriptUpdate("hello everyone", true, 0)))
        assertNull(buffer.accept(TranscriptUpdate("Hello everyone!", true, 50)))
        assertEquals(1, buffer.utterances.size)
    }

    @Test
    fun extendedFinalReplacesThePreviousOne() {
        val buffer = TranscriptBuffer()
        buffer.accept(TranscriptUpdate("I went to", true, 0))
        buffer.accept(TranscriptUpdate("I went to Tokyo last week", true, 100))
        assertEquals(listOf("I went to Tokyo last week"), buffer.utterances.map { it.text })
    }

    @Test
    fun stablePartialIsPromoted() {
        val buffer = TranscriptBuffer(stablePartialMs = 1_000)
        buffer.accept(TranscriptUpdate("anyone here play chess", false, 0))
        assertNull(buffer.flushStale(500))
        assertEquals("anyone here play chess", buffer.flushStale(1_200)?.text)
        assertNull(buffer.flushStale(5_000))
    }
}

class ResponseValidatorTest {
    private val validator = ResponseValidator(maxComments = 3)

    @Test
    fun parsesWellFormedJsonEvenWithSurroundingText() {
        val raw = """Sure! {"comments":[{"text":"tell us more about Tokyo","tone":"curious"}],
            "reactions":["heart","nonsense"],"suggested_event":"small_gift","engagement_intensity":0.7,
            "updated_topic":"travel","unknown_field":true}"""
        val response = validator.parse(raw, hatersMode = false)
        assertEquals("tell us more about Tokyo", response.comments.single().text)
        assertEquals(CommentTone.CURIOUS, response.comments.single().tone)
        assertEquals(1, response.reactions.size)
        assertEquals(EventSuggestion.SMALL_GIFT, response.suggestion)
        assertEquals("travel", response.topic)
    }

    @Test
    fun rejectsMalformedOutput() {
        assertFailsWith<InvalidAudienceResponse> { validator.parse("I can't do that", hatersMode = false) }
        assertFailsWith<InvalidAudienceResponse> { validator.parse("{\"comments\": [oops}", hatersMode = false) }
    }

    @Test
    fun dropsUnsafeOrInvalidCommentsAndClampsValues() {
        val dto = AudienceResponseDto(
            comments = listOf(
                CommentDto("visit www.spam.example for free followers"),
                CommentDto("kys"),
                CommentDto("x"),
                CommentDto("this is a lovely story"),
                CommentDto("call me at +1 555 123 4567"),
                CommentDto("mid take honestly", tone = "critical"),
            ),
            suggestedEvent = "launch_missiles",
            engagementIntensity = 9.0,
        )
        val response = validator.validate(dto, hatersMode = false)
        assertEquals(listOf("this is a lovely story"), response.comments.map { it.text })
        assertEquals(EventSuggestion.NONE, response.suggestion)
        assertEquals(1.0, response.intensity)
    }

    @Test
    fun criticalCommentsOnlyInHatersMode() {
        val dto = AudienceResponseDto(comments = listOf(CommentDto("not convinced", tone = "critical")))
        assertTrue(validator.validate(dto, hatersMode = false).comments.isEmpty())
        assertEquals(1, validator.validate(dto, hatersMode = true).comments.size)
    }

    @Test
    fun capsCommentCount() {
        val dto = AudienceResponseDto(comments = (1..10).map { CommentDto("comment $it") })
        assertEquals(3, validator.validate(dto, false).comments.size)
    }
}

class AiRequestPolicyTest {
    @Test
    fun waitsForSpeechBeforeTheFirstRequest() {
        val policy = AiRequestPolicy(perMinute = 6, perSession = 10)
        assertNull(policy.shouldRequest(60_000, hasNewSpeech = false, isQuestion = false))
        assertEquals(GenerationTrigger.QUESTION, policy.shouldRequest(60_000, hasNewSpeech = true, isQuestion = true))
    }

    @Test
    fun enforcesIntervalPerMinuteAndSessionCaps() {
        val policy = AiRequestPolicy(perMinute = 3, perSession = 4, minIntervalMs = 1_000)
        var now = 0L
        var made = 0
        repeat(200) {
            now += 500
            if (policy.shouldRequest(now, hasNewSpeech = true, isQuestion = false) != null) {
                policy.onStarted(now)
                policy.onSucceeded()
                made++
            }
        }
        assertEquals(4, made)
        assertTrue(policy.sessionBudgetExhausted)
    }

    @Test
    fun noConcurrentRequests() {
        val policy = AiRequestPolicy(perMinute = 10, perSession = 10, minIntervalMs = 0)
        policy.onStarted(0)
        assertNull(policy.shouldRequest(10_000, hasNewSpeech = true, isQuestion = false))
    }

    @Test
    fun repeatedFailuresOpenTheCircuit() {
        val policy = AiRequestPolicy(perMinute = 60, perSession = 100, minIntervalMs = 0, failureThreshold = 2, circuitOpenMs = 10_000)
        policy.onStarted(0); policy.onFailed(0)
        policy.onStarted(1); policy.onFailed(1)
        assertTrue(policy.isCircuitOpen(5_000))
        assertNull(policy.shouldRequest(5_000, hasNewSpeech = true, isQuestion = false))
        assertNotNull(policy.shouldRequest(12_000, hasNewSpeech = true, isQuestion = false))
    }

    @Test
    fun idleRequestsAfterQuietStretch() {
        val policy = AiRequestPolicy(perMinute = 6, perSession = 10, idleIntervalMs = 15_000)
        policy.onStarted(0); policy.onSucceeded()
        assertNull(policy.shouldRequest(10_000, hasNewSpeech = false, isQuestion = false))
        assertEquals(GenerationTrigger.IDLE, policy.shouldRequest(16_000, hasNewSpeech = false, isQuestion = false))
    }
}

class AudienceContextTest {
    @Test
    fun keepsOnlyRecentSpeechAndComments() {
        val context = AudienceContext("s", SimulationConfig(title = "Late night chat"), maxSpeech = 2, maxComments = 2)
        listOf("one", "two", "three").forEach(context::addSpeech)
        listOf("a", "b", "c").forEach(context::addAudienceComment)
        val request = context.buildRequest(GenerationTrigger.UTTERANCE, elapsedSeconds = 30, viewerCount = 120, maxComments = 4)
        assertEquals(listOf("two", "three"), request.recentSpeech)
        assertEquals(listOf("b", "c"), request.recentComments)
        assertEquals("Late night chat", request.topic)
        assertEquals(1, request.requestSeq)
        assertEquals(2, context.buildRequest(GenerationTrigger.IDLE, 31, 120, 1).requestSeq)
    }
}

/**
 * Scripted conversations for the on-device audience. They check that reactions follow what was said rather
 * than being generic filler. The same scripts are the starting point for evaluating the backend LLM prompt.
 */
class LocalAudienceGeneratorTest {
    private fun ask(vararg speech: String, language: AudienceLanguage = AudienceLanguage.ENGLISH, haters: Boolean = false): AudienceResponse {
        val config = SimulationConfig(
            language = language,
            hatersMode = haters,
            vibes = setOf(AudienceVibe.SUPPORTIVE, AudienceVibe.CURIOUS),
        )
        val context = AudienceContext("s", config)
        speech.forEach(context::addSpeech)
        return LocalAudienceGenerator(seed = 42).respond(context.buildRequest(GenerationTrigger.UTTERANCE, 30, 200, 4))
    }

    @Test
    fun questionsGetMoreRepliesThanStatements() {
        assertEquals(3, ask("what should I cook for dinner tonight?").comments.size)
        assertEquals(2, ask("I finally finished my project").comments.size)
    }

    @Test
    fun jokesTriggerLaughterAndBurst() {
        val response = ask("haha that was the worst joke ever")
        assertEquals(EventSuggestion.REACTION_BURST, response.suggestion)
        assertTrue(response.reactions.isNotEmpty())
    }

    @Test
    fun sadStoriesGetSupportNotHype() {
        val response = ask("honestly I've been really tired and stressed this week")
        assertEquals(EventSuggestion.MEDIUM_GIFT, response.suggestion)
        assertTrue(response.comments.none { it.tone == CommentTone.HYPE })
    }

    @Test
    fun commentsCanReferToWhatWasSaid() {
        val mentions = (0 until 20).count { seed ->
            val context = AudienceContext("s", SimulationConfig())
            context.addSpeech("I just came back from Barcelona")
            LocalAudienceGenerator(seed.toLong())
                .respond(context.buildRequest(GenerationTrigger.UTTERANCE, 30, 200, 4))
                .comments.any { "barcelona" in it.text.lowercase() }
        }
        assertTrue(mentions >= 5, "keyword should appear in a meaningful share of responses, was $mentions/20")
    }

    @Test
    fun arabicSpeechGetsArabicComments() {
        val response = ask("السلام عليكم كيف حالكم اليوم؟", language = AudienceLanguage.MIXED)
        assertTrue(response.comments.isNotEmpty())
        assertTrue(response.comments.all { c -> c.text.any { it in '؀'..'ۿ' } || c.text.all { !it.isLetter() } })
    }

    @Test
    fun noCriticismWithoutHatersMode() {
        repeat(10) {
            assertTrue(ask("this is my new song").comments.none { it.tone == CommentTone.CRITICAL })
        }
    }

    @Test
    fun deterministicForSameSeedAndRequest() {
        assertEquals(ask("I love football").comments, ask("I love football").comments)
    }
}
