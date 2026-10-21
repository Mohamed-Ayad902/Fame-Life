package org.mayad.fame_life.domain.simulation

import kotlinx.serialization.Serializable
import org.mayad.fame_life.domain.entitlement.PlanLimits

/** Audience size presets. The ranges are simulation parameters, not a number of connected people. */
enum class AudiencePreset(val startRange: IntRange, val targetRange: IntRange) {
    INTIMATE(startRange = 18..45, targetRange = 160..420),
    RISING(startRange = 70..160, targetRange = 900..2_600),
    BUZZING(startRange = 280..650, targetRange = 4_500..12_000),
    VIRAL(startRange = 900..2_200, targetRange = 26_000..80_000),
}

/** Tone of the simulated audience. Vibes influence tone, never business rules. */
enum class AudienceVibe { SUPPORTIVE, CURIOUS, FUNNY, HYPE, CRITICAL }

enum class EventFrequency(val commentGapMs: LongRange, val eventCooldownMs: Long) {
    CALM(commentGapMs = 1_400L..2_800L, eventCooldownMs = 28_000L),
    LIVELY(commentGapMs = 800L..1_900L, eventCooldownMs = 16_000L),
    WILD(commentGapMs = 450L..1_200L, eventCooldownMs = 9_000L),
}

/** The audience's language. [MIXED] (shown as "Auto") follows whatever language the creator is speaking. */
enum class AudienceLanguage { ENGLISH, ARABIC, MIXED }

@Serializable
data class SimulationConfig(
    val title: String = "",
    val preset: AudiencePreset = AudiencePreset.RISING,
    val vibes: Set<AudienceVibe> = setOf(AudienceVibe.SUPPORTIVE, AudienceVibe.CURIOUS, AudienceVibe.FUNNY),
    val hatersMode: Boolean = false,
    val giftsEnabled: Boolean = true,
    val frequency: EventFrequency = EventFrequency.LIVELY,
    val language: AudienceLanguage = AudienceLanguage.MIXED,
    val liveStyleId: String = "aurora",
    val maxDurationSeconds: Int = 10 * 60,
    val seed: Long = 0L,
) {
    /** Clamps every field the plan restricts, so the engine never runs with options the user is not entitled to. */
    fun constrainedTo(limits: PlanLimits): SimulationConfig {
        val allowedVibes = vibes.filter { it in limits.vibes }.toSet().ifEmpty { setOf(AudienceVibe.SUPPORTIVE) }
        return copy(
            preset = if (preset in limits.presets) preset else limits.presets.maxBy { it.ordinal },
            vibes = allowedVibes,
            hatersMode = hatersMode && limits.hatersMode,
            liveStyleId = if (liveStyleId in limits.liveStyles) liveStyleId else limits.liveStyles.first(),
            maxDurationSeconds = maxDurationSeconds.coerceIn(60, limits.maxSessionSeconds),
        )
    }
}
