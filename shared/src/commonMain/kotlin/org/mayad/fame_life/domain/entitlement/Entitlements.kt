package org.mayad.fame_life.domain.entitlement

import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.GiftTier

enum class Plan { FREE, PRO }

/**
 * Where the current plan came from. Only [VERIFIED] may unlock paid limits for anything that costs money
 * (AI requests); the server must re-check, because anything on the device can be tampered with.
 */
enum class EntitlementSource { DEFAULT_FREE, CACHED, VERIFIED }

data class Entitlements(
    val plan: Plan,
    val source: EntitlementSource,
) {
    val limits: PlanLimits get() = PlanLimits.forPlan(if (source == EntitlementSource.DEFAULT_FREE) Plan.FREE else plan)

    companion object {
        val Free = Entitlements(Plan.FREE, EntitlementSource.DEFAULT_FREE)
    }
}

/** Product hypotheses for the Free/Pro split. Tuned through Remote Config later, never trusted for billing. */
data class PlanLimits(
    val maxSessionSeconds: Int,
    val aiRequestsPerSession: Int,
    val aiRequestsPerMinute: Int,
    val sessionsPerDay: Int,
    val presets: Set<AudiencePreset>,
    val vibes: Set<AudienceVibe>,
    val giftTiers: Set<GiftTier>,
    val hatersMode: Boolean,
    val liveStyles: Set<String>,
    val historySize: Int,
) {
    companion object {
        val Free = PlanLimits(
            maxSessionSeconds = 10 * 60,
            aiRequestsPerSession = 60,
            aiRequestsPerMinute = 6,
            sessionsPerDay = 3,
            presets = setOf(AudiencePreset.INTIMATE, AudiencePreset.RISING),
            vibes = setOf(AudienceVibe.SUPPORTIVE, AudienceVibe.CURIOUS, AudienceVibe.FUNNY),
            giftTiers = setOf(GiftTier.SMALL, GiftTier.MEDIUM),
            hatersMode = false,
            liveStyles = setOf("aurora"),
            historySize = 10,
        )
        val Pro = PlanLimits(
            maxSessionSeconds = 45 * 60,
            aiRequestsPerSession = 300,
            aiRequestsPerMinute = 12,
            sessionsPerDay = 30,
            presets = AudiencePreset.entries.toSet(),
            vibes = AudienceVibe.entries.toSet(),
            giftTiers = GiftTier.entries.toSet(),
            hatersMode = true,
            liveStyles = setOf("aurora", "noir"),
            historySize = 200,
        )

        fun forPlan(plan: Plan) = when (plan) {
            Plan.FREE -> Free
            Plan.PRO -> Pro
        }
    }
}
