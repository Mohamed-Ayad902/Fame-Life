package org.mayad.fame_life.domain.simulation

import org.mayad.fame_life.domain.entitlement.PlanLimits
import kotlin.random.Random

sealed interface ApprovedEvent {
    data class SendGift(val gift: Gift) : ApprovedEvent
    data object VipJoin : ApprovedEvent
    data object Trending : ApprovedEvent
    data class Burst(val kind: ReactionKind, val count: Int) : ApprovedEvent
}

/**
 * The only place that decides whether a gift or special event happens. The AI can suggest; this policy
 * applies cooldowns, plan limits, session settings and pacing, so a model can never flood the stream.
 */
class EventPolicy(
    private val config: SimulationConfig,
    private val limits: PlanLimits,
) {
    private var lastEventAtMs = Long.MIN_VALUE / 2
    private val lastByKind = mutableMapOf<String, Long>()
    private val recentGiftTimes = ArrayDeque<Long>()

    fun decide(
        suggestion: EventSuggestion,
        nowMs: Long,
        intensity: Double,
        random: Random,
    ): ApprovedEvent? {
        if (nowMs < WARMUP_MS) return null
        return when (suggestion) {
            EventSuggestion.NONE -> null
            EventSuggestion.REACTION_BURST -> burst(nowMs, intensity, random)
            EventSuggestion.SMALL_GIFT -> gift(GiftTier.SMALL, nowMs, intensity, random)
            EventSuggestion.MEDIUM_GIFT -> gift(GiftTier.MEDIUM, nowMs, intensity, random)
            EventSuggestion.MAJOR_GIFT -> gift(GiftTier.MAJOR, nowMs, intensity, random)
            EventSuggestion.VIP_JOIN -> special("vip", VIP_COOLDOWN_MS, nowMs) { ApprovedEvent.VipJoin }
            EventSuggestion.TRENDING ->
                if (intensity >= TRENDING_MIN_INTENSITY && nowMs >= TRENDING_EARLIEST_MS) {
                    special("trending", TRENDING_COOLDOWN_MS, nowMs) { ApprovedEvent.Trending }
                } else {
                    null
                }
        }
    }

    /** Low-probability background activity, so a quiet stretch still feels inhabited. Called once per second. */
    fun ambient(nowMs: Long, engagement: Double, random: Random): ApprovedEvent? {
        val chance = AMBIENT_GIFT_CHANCE * (0.4 + engagement)
        return if (random.nextDouble() < chance) gift(GiftTier.SMALL, nowMs, engagement, random) else null
    }

    private fun gift(requested: GiftTier, nowMs: Long, intensity: Double, random: Random): ApprovedEvent? {
        if (!config.giftsEnabled) return null
        var tier = requested
        if (tier == GiftTier.MAJOR && (intensity < MAJOR_MIN_INTENSITY || nowMs < MAJOR_EARLIEST_MS)) tier = GiftTier.MEDIUM
        while (tier !in limits.giftTiers) {
            if (tier.ordinal == 0) return null
            tier = GiftTier.entries[tier.ordinal - 1]
        }
        val cooldown = when (tier) {
            GiftTier.SMALL -> SMALL_COOLDOWN_MS
            GiftTier.MEDIUM -> MEDIUM_COOLDOWN_MS
            GiftTier.MAJOR -> MAJOR_COOLDOWN_MS
        }
        if (!cooledDown("gift-$tier", cooldown, nowMs)) return null
        while (recentGiftTimes.isNotEmpty() && nowMs - recentGiftTimes.first() > 60_000) recentGiftTimes.removeFirst()
        if (recentGiftTimes.size >= MAX_GIFTS_PER_MINUTE) return null
        if (tier != GiftTier.SMALL && !cooledDown("event", config.frequency.eventCooldownMs, nowMs)) return null

        val options = GiftCatalog.ofTier(tier)
        val gift = weighted(options, random)
        recentGiftTimes.addLast(nowMs)
        lastByKind["gift-$tier"] = nowMs
        if (tier != GiftTier.SMALL) lastEventAtMs = nowMs
        return ApprovedEvent.SendGift(gift)
    }

    private fun burst(nowMs: Long, intensity: Double, random: Random): ApprovedEvent? {
        if (!cooledDown("burst", BURST_COOLDOWN_MS, nowMs)) return null
        lastByKind["burst"] = nowMs
        val kind = ReactionKind.entries.random(random)
        return ApprovedEvent.Burst(kind, count = (4 + intensity * 14).toInt())
    }

    private inline fun special(kind: String, cooldown: Long, nowMs: Long, make: () -> ApprovedEvent): ApprovedEvent? {
        if (!cooledDown(kind, cooldown, nowMs) || !cooledDown("event", config.frequency.eventCooldownMs, nowMs)) return null
        lastByKind[kind] = nowMs
        lastEventAtMs = nowMs
        return make()
    }

    private fun cooledDown(kind: String, cooldown: Long, nowMs: Long): Boolean {
        val last = if (kind == "event") lastEventAtMs else lastByKind[kind] ?: return true
        return nowMs - last >= cooldown
    }

    private fun weighted(gifts: List<Gift>, random: Random): Gift {
        var roll = random.nextInt(gifts.sumOf { it.weight })
        for (gift in gifts) {
            roll -= gift.weight
            if (roll < 0) return gift
        }
        return gifts.last()
    }

    companion object {
        const val WARMUP_MS = 8_000L
        const val SMALL_COOLDOWN_MS = 6_000L
        const val MEDIUM_COOLDOWN_MS = 25_000L
        const val MAJOR_COOLDOWN_MS = 90_000L
        const val MAJOR_MIN_INTENSITY = 0.7
        const val MAJOR_EARLIEST_MS = 60_000L
        const val MAX_GIFTS_PER_MINUTE = 5
        const val VIP_COOLDOWN_MS = 120_000L
        const val TRENDING_COOLDOWN_MS = 150_000L
        const val TRENDING_MIN_INTENSITY = 0.75
        const val TRENDING_EARLIEST_MS = 45_000L
        const val BURST_COOLDOWN_MS = 5_000L
        const val AMBIENT_GIFT_CHANCE = 0.025
    }
}
