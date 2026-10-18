package org.mayad.fame_life.domain.simulation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.mayad.fame_life.domain.ai.AiRequestPolicy
import org.mayad.fame_life.domain.ai.AudienceContext
import org.mayad.fame_life.domain.ai.AudienceGenerator
import org.mayad.fame_life.domain.ai.AudienceRequest
import org.mayad.fame_life.domain.ai.AudienceResponse
import org.mayad.fame_life.domain.ai.GenerationTrigger
import org.mayad.fame_life.domain.ai.LocalAudienceGenerator
import org.mayad.fame_life.domain.ai.SpeechEvent
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.ai.SpeechSignals
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.ai.TranscriptBuffer
import org.mayad.fame_life.domain.ai.TranscriptUpdate
import org.mayad.fame_life.domain.entitlement.PlanLimits

/** What the audience "brain" is doing, shown as a small indicator on the live screen. */
enum class AiActivity { LISTENING, THINKING, ON_DEVICE, BUDGET_REACHED }

data class LiveSessionState(
    val live: LiveSnapshot = LiveSnapshot(),
    val ai: AiActivity = AiActivity.LISTENING,
    val speech: SpeechStatus = SpeechStatus.OFF,
    val micMuted: Boolean = false,
    val livePartial: String? = null,
    val summary: SessionSummary? = null,
)

/**
 * Runs one live session: drives the engine clock, listens to speech, decides when to ask the generator, and
 * guarantees cleanup. All work runs in [scope]; [end] cancels everything, and responses that arrive after the
 * session ended are discarded.
 */
class LiveSessionController(
    private val sessionId: String,
    config: SimulationConfig,
    limits: PlanLimits,
    private val speechInput: SpeechInput?,
    /** The backend generator. Null when no backend is configured; the on-device audience is used instead. */
    private val remote: AudienceGenerator?,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val epochClock: () -> Long,
    private val requestTimeoutMs: Long = 6_000L,
    private val tickMs: Long = 200L,
) {
    private val engine = LiveSessionEngine(config, limits)
    private val local = LocalAudienceGenerator(seed = config.seed)
    private val transcript = TranscriptBuffer()
    private val context = AudienceContext(sessionId, config)
    private val remotePolicy = remote?.let {
        AiRequestPolicy(perMinute = limits.aiRequestsPerMinute, perSession = limits.aiRequestsPerSession)
    }

    // On-device generation costs nothing, but it is still paced like the real thing.
    private val localPolicy = AiRequestPolicy(perMinute = 20, perSession = Int.MAX_VALUE)

    private val _state = MutableStateFlow(LiveSessionState(live = engine.snapshot))
    val state: StateFlow<LiveSessionState> = _state.asStateFlow()

    private var startedAtEpochMs = 0L
    private var hasNewSpeech = false
    private var lastQuestion = false
    private var loopJob: Job? = null
    private var speechJob: Job? = null
    private var requestJob: Job? = null

    fun start() {
        if (engine.snapshot.status != SessionStatus.READY) return
        startedAtEpochMs = epochClock()
        engine.start(clock())
        startSpeech()
        loopJob = scope.launch {
            // Open with a greeting so the room is never silent while the creator collects their thoughts.
            deliver(local.respond(context.buildRequest(GenerationTrigger.OPENING, 0, engine.snapshot.viewers, 2)), fromFallback = false)
            while (isActive) {
                step()
                delay(tickMs)
            }
        }
        publish()
    }

    fun pause() {
        engine.pause(clock())
        speechJob?.cancel()
        requestJob?.cancel()
        remotePolicy?.onCancelled()
        localPolicy.onCancelled()
        _state.update { it.copy(speech = SpeechStatus.OFF) }
        publish()
    }

    fun resume() {
        engine.resume(clock())
        if (!_state.value.micMuted) startSpeech()
        publish()
    }

    fun setMicMuted(muted: Boolean) {
        _state.update { it.copy(micMuted = muted) }
        if (muted) {
            speechJob?.cancel()
            _state.update { it.copy(speech = SpeechStatus.OFF, livePartial = null) }
        } else if (engine.snapshot.status == SessionStatus.RUNNING) {
            startSpeech()
        }
    }

    /** Stops every job, releases the microphone and returns the summary. Repeated calls return the same summary. */
    fun end(): SessionSummary? {
        _state.value.summary?.let { return it }
        loopJob?.cancel()
        speechJob?.cancel()
        requestJob?.cancel()
        val summary = engine.finish(clock(), sessionId, startedAtEpochMs)
        _state.update { it.copy(live = engine.snapshot, speech = SpeechStatus.OFF, livePartial = null, summary = summary) }
        return summary
    }

    private fun step() {
        val now = clock()
        engine.tick(now)
        transcript.flushStale(now)?.let { onUtterance(it.text) }
        if (engine.isTimeUp) {
            end()
            return
        }
        maybeRequest(now)
        publish()
    }

    private fun startSpeech() {
        val input = speechInput ?: run {
            _state.update { it.copy(speech = SpeechStatus.UNAVAILABLE) }
            return
        }
        speechJob?.cancel()
        speechJob = scope.launch {
            input.listen(engine.config.language).collect { event ->
                when (event) {
                    is SpeechEvent.Status -> _state.update { it.copy(speech = event.status) }
                    is SpeechEvent.Transcript -> {
                        transcript.accept(TranscriptUpdate(event.text, event.isFinal, clock()))?.let { onUtterance(it.text) }
                        _state.update { it.copy(livePartial = transcript.livePartial) }
                    }
                }
            }
        }
    }

    private fun onUtterance(text: String) {
        context.addSpeech(text)
        engine.onCreatorSpoke()
        hasNewSpeech = true
        lastQuestion = SpeechSignals.of(text).isQuestion
    }

    private fun maybeRequest(now: Long) {
        if (engine.snapshot.status != SessionStatus.RUNNING || requestJob?.isActive == true) return
        val remotePolicy = remotePolicy
        val useRemote = remotePolicy != null && !remotePolicy.sessionBudgetExhausted && !remotePolicy.isCircuitOpen(now)
        val policy = if (useRemote && remotePolicy != null) remotePolicy else localPolicy
        val trigger = policy.shouldRequest(now, hasNewSpeech, lastQuestion) ?: return
        val request = context.buildRequest(
            trigger = trigger,
            elapsedSeconds = engine.snapshot.elapsedSeconds,
            viewerCount = engine.snapshot.viewers,
            maxComments = if (trigger == GenerationTrigger.IDLE) 1 else 4,
        )
        hasNewSpeech = false
        policy.onStarted(now)
        _state.update { it.copy(ai = AiActivity.THINKING) }
        requestJob = scope.launch {
            if (useRemote) {
                requestRemote(policy, request)
            } else {
                policy.onSucceeded()
                deliver(local.respond(request), fromFallback = false)
            }
        }
    }

    private suspend fun requestRemote(policy: AiRequestPolicy, request: AudienceRequest) {
        val response = try {
            withTimeout(requestTimeoutMs) { remote!!.generate(request) }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            // end() or pause() cancelled us: drop the work, never deliver into a finished session.
            policy.onCancelled()
            throw e
        } catch (e: Exception) {
            null
        }
        engine.recordAiRequest(failed = response == null)
        if (response != null) {
            policy.onSucceeded()
            deliver(response, fromFallback = false)
        } else {
            policy.onFailed(clock())
            deliver(local.respond(request), fromFallback = true)
        }
    }

    private fun deliver(response: AudienceResponse, fromFallback: Boolean) {
        if (engine.snapshot.status != SessionStatus.RUNNING) return
        context.absorb(response)
        response.comments.forEach { context.addAudienceComment(it.text) }
        engine.apply(response, fromFallback)
        publish()
    }

    private fun publish() {
        val remotePolicy = remotePolicy
        val activity = when {
            requestJob?.isActive == true -> AiActivity.THINKING
            remotePolicy == null || remotePolicy.isCircuitOpen(clock()) -> AiActivity.ON_DEVICE
            remotePolicy.sessionBudgetExhausted -> AiActivity.BUDGET_REACHED
            else -> AiActivity.LISTENING
        }
        _state.update { it.copy(live = engine.snapshot, ai = activity) }
    }
}
