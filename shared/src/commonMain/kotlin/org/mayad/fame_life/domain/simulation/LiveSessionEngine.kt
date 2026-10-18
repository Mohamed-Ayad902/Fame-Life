package org.mayad.fame_life.domain.simulation

import org.mayad.fame_life.domain.ai.AudienceResponse
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.AudienceEvent.Comment
import kotlin.random.Random

enum class SessionStatus { READY, RUNNING, PAUSED, COMPLETED }

/** Everything the live screen renders. Immutable, so the UI can never mutate simulation state. */
data class LiveSnapshot(
    val status: SessionStatus = SessionStatus.READY,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val viewers: Int = 0,
    val peakViewers: Int = 0,
    val comments: List<Comment> = emptyList(),
    /** The most recent attention-worthy event (gift, milestone, VIP, trending) for the overlay. */
    val spotlight: AudienceEvent? = null,
    val reactions: List<AudienceEvent.Reaction> = emptyList(),
    val totals: SessionTotals = SessionTotals(),
    val engagement: Double = 0.0,
)

/**
 * The deterministic core of a live session: viewer counts, comment pacing, gifts and events, totals.
 * It has no clock, no coroutines and no I/O. The controller feeds it time and validated AI responses.
 */
class LiveSessionEngine(
    val config: SimulationConfig,
    private val limits: PlanLimits,
    private val maxVisibleComments: Int = 7,
) {
    private val random = Random(config.seed)
    private val viewers = ViewerCountEngine(config.preset, random)
    private val roster = AudienceRoster(size = 48, language = config.language, random = random)
    private val pacer = CommentPacer(config.frequency)
    private val policy = EventPolicy(config, limits)

    private var nextEventId = 1L
    private var lastTickMs: Long? = null
    private var sessionMs = 0L
    private var lastWholeSecond = 0
    private var engagement = 0.35
    private var topGift: Gift? = null
    private var highlight: String? = null

    var snapshot = LiveSnapshot(remainingSeconds = config.maxDurationSeconds, viewers = viewers.current, peakViewers = viewers.peak)
        private set

    val sessionTimeMs: Long get() = sessionMs
    val isTimeUp: Boolean get() = snapshot.elapsedSeconds >= config.maxDurationSeconds

    fun start(nowMs: Long) {
        if (snapshot.status != SessionStatus.READY) return
        lastTickMs = nowMs
        snapshot = snapshot.copy(status = SessionStatus.RUNNING)
    }

    fun pause(nowMs: Long) {
        if (snapshot.status != SessionStatus.RUNNING) return
        advanceClock(nowMs)
        pacer.clear()
        snapshot = snapshot.copy(status = SessionStatus.PAUSED)
    }

    fun resume(nowMs: Long) {
        if (snapshot.status != SessionStatus.PAUSED) return
        lastTickMs = nowMs
        snapshot = snapshot.copy(status = SessionStatus.RUNNING)
    }

    /** Advances the simulation to [nowMs]. Safe to call at any rate; the audience moves in whole seconds. */
    fun tick(nowMs: Long) {
        if (snapshot.status != SessionStatus.RUNNING) return
        advanceClock(nowMs)
        val second = (sessionMs / 1000).toInt()
        while (lastWholeSecond < second) {
            lastWholeSecond++
            stepSecond()
        }
        pacer.poll(sessionMs, random)?.let { emitComment(it) }
        snapshot = snapshot.copy(
            elapsedSeconds = second,
            remainingSeconds = (config.maxDurationSeconds - second).coerceAtLeast(0),
            reactions = snapshot.reactions.filter { sessionMs - it.atMs < REACTION_LIFETIME_MS },
            spotlight = snapshot.spotlight?.takeIf { sessionMs - it.atMs < SPOTLIGHT_LIFETIME_MS },
        )
    }

    /** Speech keeps the room warm. Called when the creator finishes a phrase. */
    fun onCreatorSpoke() {
        engagement = (engagement + 0.08).coerceAtMost(1.0)
    }

    /** Applies a validated response. Comments are paced; any suggested event still has to pass the policy. */
    fun apply(response: AudienceResponse, fromFallback: Boolean) {
        if (snapshot.status != SessionStatus.RUNNING) return
        engagement = (engagement * 0.6 + response.intensity * 0.4).coerceIn(0.0, 1.0)
        pacer.offer(response.comments)
        response.reactions.firstOrNull()?.let { kind ->
            emitReaction(kind, count = 3 + (response.intensity * 8).toInt())
        }
        policy.decide(response.suggestion, sessionMs, response.intensity, random)?.let { approved ->
            emitApproved(approved, response.eventReason)
        }
        val totals = snapshot.totals
        snapshot = snapshot.copy(
            totals = if (fromFallback) totals.copy(fallbackResponses = totals.fallbackResponses + 1) else totals,
        )
    }

    fun recordAiRequest(failed: Boolean) {
        val totals = snapshot.totals
        snapshot = snapshot.copy(
            totals = totals.copy(
                aiRequests = totals.aiRequests + 1,
                aiFailures = totals.aiFailures + if (failed) 1 else 0,
            ),
        )
    }

    /** Ends the session and produces the summary. Idempotent: only the first call ends the session. */
    fun finish(nowMs: Long, sessionId: String, startedAtEpochMs: Long): SessionSummary? {
        if (snapshot.status == SessionStatus.COMPLETED || snapshot.status == SessionStatus.READY) return null
        if (snapshot.status == SessionStatus.RUNNING) advanceClock(nowMs)
        pacer.clear()
        val durationSeconds = (sessionMs / 1000).toInt()
        snapshot = snapshot.copy(status = SessionStatus.COMPLETED, elapsedSeconds = durationSeconds, spotlight = null)
        val totals = snapshot.totals
        return SessionSummary(
            id = sessionId,
            title = config.title,
            startedAtEpochMs = startedAtEpochMs,
            durationSeconds = durationSeconds,
            preset = config.preset.name,
            liveStyleId = config.liveStyleId,
            peakViewers = viewers.peak,
            comments = totals.comments,
            contextualComments = totals.contextualComments,
            reactions = totals.reactions,
            gifts = totals.gifts,
            giftPoints = totals.giftPoints,
            milestones = totals.milestones,
            topGiftId = topGift?.id,
            highlight = highlight,
            famePointsEarned = FameProgression.pointsFor(
                durationSeconds = durationSeconds,
                peakViewers = viewers.peak,
                contextualComments = totals.contextualComments,
                giftPoints = totals.giftPoints,
                milestones = totals.milestones,
            ),
        )
    }

    private fun advanceClock(nowMs: Long) {
        val last = lastTickMs ?: nowMs
        if (nowMs > last) sessionMs += nowMs - last
        lastTickMs = nowMs
    }

    private fun stepSecond() {
        engagement = (engagement * ENGAGEMENT_DECAY).coerceAtLeast(MIN_ENGAGEMENT)
        val before = viewers.current
        val now = viewers.step(lastWholeSecond, engagement)
        ViewerMilestones.crossed(before, now)?.let { milestone ->
            val event = AudienceEvent.Milestone(nextEventId++, sessionMs, milestone)
            snapshot = snapshot.copy(
                spotlight = event,
                totals = snapshot.totals.copy(milestones = snapshot.totals.milestones + 1),
            )
        }
        policy.ambient(sessionMs, engagement, random)?.let { emitApproved(it, reason = null) }
        snapshot = snapshot.copy(viewers = now, peakViewers = viewers.peak, engagement = engagement)
    }

    private fun emitComment(pending: PendingComment) {
        val comment = Comment(
            id = nextEventId++,
            atMs = sessionMs,
            author = roster.pickCommenter(random),
            text = pending.text,
            tone = pending.tone,
            contextual = pending.contextual,
        )
        if (pending.contextual && highlight == null && pending.text.length >= 12) highlight = pending.text
        val totals = snapshot.totals
        snapshot = snapshot.copy(
            comments = (snapshot.comments + comment).takeLast(maxVisibleComments),
            totals = totals.copy(
                comments = totals.comments + 1,
                contextualComments = totals.contextualComments + if (pending.contextual) 1 else 0,
            ),
        )
    }

    private fun emitReaction(kind: ReactionKind, count: Int) {
        val event = AudienceEvent.Reaction(nextEventId++, sessionMs, kind, count)
        snapshot = snapshot.copy(
            reactions = (snapshot.reactions + event).takeLast(MAX_REACTION_BURSTS),
            totals = snapshot.totals.copy(reactions = snapshot.totals.reactions + count),
        )
    }

    private fun emitApproved(approved: ApprovedEvent, reason: String?) {
        when (approved) {
            is ApprovedEvent.Burst -> emitReaction(approved.kind, approved.count)
            is ApprovedEvent.SendGift -> {
                val gift = approved.gift
                val event = AudienceEvent.GiftSent(nextEventId++, sessionMs, roster.pickCommenter(random), gift, reason)
                if (topGift == null || gift.famePoints > topGift!!.famePoints) topGift = gift
                viewers.spike(
                    when (gift.tier) {
                        GiftTier.SMALL -> 0.1
                        GiftTier.MEDIUM -> 0.35
                        GiftTier.MAJOR -> 0.8
                    },
                )
                val totals = snapshot.totals
                snapshot = snapshot.copy(
                    spotlight = if (gift.tier == GiftTier.SMALL && snapshot.spotlight != null) snapshot.spotlight else event,
                    totals = totals.copy(gifts = totals.gifts + 1, giftPoints = totals.giftPoints + gift.famePoints),
                )
            }
            ApprovedEvent.VipJoin -> {
                viewers.spike(0.25)
                snapshot = snapshot.copy(spotlight = AudienceEvent.VipJoined(nextEventId++, sessionMs, roster.pickVip(random)))
            }
            ApprovedEvent.Trending -> {
                viewers.spike(1.0)
                snapshot = snapshot.copy(
                    spotlight = AudienceEvent.TrendingMoment(nextEventId++, sessionMs, reason),
                )
            }
        }
    }

    private companion object {
        const val ENGAGEMENT_DECAY = 0.985
        const val MIN_ENGAGEMENT = 0.12
        const val REACTION_LIFETIME_MS = 2_400L
        const val SPOTLIGHT_LIFETIME_MS = 3_800L
        const val MAX_REACTION_BURSTS = 4
    }
}
