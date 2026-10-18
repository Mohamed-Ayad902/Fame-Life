package org.mayad.fame_life.domain.ai

/**
 * Decides when the generator may be called. Generation is event-driven (new speech, a question, a long quiet
 * stretch) and capped per minute and per session, so cost stays bounded and predictable.
 */
class AiRequestPolicy(
    private val perMinute: Int,
    private val perSession: Int,
    private val minIntervalMs: Long = 3_500L,
    private val idleIntervalMs: Long = 18_000L,
    private val failureThreshold: Int = 3,
    private val circuitOpenMs: Long = 30_000L,
) {
    private val recent = ArrayDeque<Long>()
    private var lastRequestAtMs = Long.MIN_VALUE / 2
    private var consecutiveFailures = 0
    private var circuitOpenUntilMs = Long.MIN_VALUE / 2

    var totalRequests = 0
        private set
    var inFlight = false
        private set

    val sessionBudgetExhausted: Boolean get() = totalRequests >= perSession

    /** Returns the trigger to use now, or null when the call should not be made. */
    fun shouldRequest(nowMs: Long, hasNewSpeech: Boolean, isQuestion: Boolean): GenerationTrigger? {
        if (inFlight || sessionBudgetExhausted) return null
        if (nowMs < circuitOpenUntilMs) return null
        if (nowMs - lastRequestAtMs < minIntervalMs) return null
        while (recent.isNotEmpty() && nowMs - recent.first() >= 60_000L) recent.removeFirst()
        if (recent.size >= perMinute) return null
        return when {
            totalRequests == 0 && !hasNewSpeech -> null
            isQuestion && hasNewSpeech -> GenerationTrigger.QUESTION
            hasNewSpeech -> GenerationTrigger.UTTERANCE
            nowMs - lastRequestAtMs >= idleIntervalMs -> GenerationTrigger.IDLE
            else -> null
        }
    }

    /** True while repeated failures have tripped the breaker; callers use the local fallback instead. */
    fun isCircuitOpen(nowMs: Long): Boolean = nowMs < circuitOpenUntilMs

    fun onStarted(nowMs: Long) {
        inFlight = true
        lastRequestAtMs = nowMs
        recent.addLast(nowMs)
        totalRequests++
    }

    fun onSucceeded() {
        inFlight = false
        consecutiveFailures = 0
    }

    fun onFailed(nowMs: Long) {
        inFlight = false
        consecutiveFailures++
        if (consecutiveFailures >= failureThreshold) {
            circuitOpenUntilMs = nowMs + circuitOpenMs
            consecutiveFailures = 0
        }
    }

    fun onCancelled() {
        inFlight = false
    }
}
