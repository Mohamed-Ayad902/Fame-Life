package org.mayad.fame_life.domain.simulation

import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Evolves the simulated viewer count one second at a time.
 *
 * The count eases toward a target that grows with time and engagement, with small seeded fluctuations
 * and spikes that decay. Same seed and inputs always give the same sequence.
 */
class ViewerCountEngine(
    private val preset: AudiencePreset,
    private val random: Random,
) {
    private val start = preset.startRange.random(random)
    private val ceiling = preset.targetRange.random(random)
    private var surge = 0.0
    private var value = start.toDouble()

    var current: Int = start
        private set
    var peak: Int = start
        private set

    /** Advances one second. [engagement] is 0..1, where 0 is a silent stream and 1 is a lively one. */
    fun step(elapsedSeconds: Int, engagement: Double): Int {
        val e = engagement.coerceIn(0.0, 1.0)
        val floor = preset.targetRange.first
        val target = floor + (ceiling - floor) * (0.25 + 0.75 * e)
        val growth = 1.0 - exp(-elapsedSeconds / GROWTH_SECONDS)
        val desired = start + (target - start) * growth + surge
        val wobble = desired * random.nextDouble(-WOBBLE, WOBBLE)
        value += (desired - value) * EASING + wobble
        value = value.coerceAtLeast(1.0)
        surge *= SURGE_DECAY
        current = value.roundToInt()
        if (current > peak) peak = current
        return current
    }

    /** Adds a decaying surge, used for gifts, trending moments and other big beats. [strength] is 0..1. */
    fun spike(strength: Double) {
        surge += ceiling * SPIKE_SHARE * strength.coerceIn(0.0, 1.0)
    }

    private companion object {
        const val GROWTH_SECONDS = 110.0
        const val EASING = 0.16
        const val WOBBLE = 0.008
        const val SURGE_DECAY = 0.95
        const val SPIKE_SHARE = 0.14
    }
}

/** Milestones are round numbers the stream crosses, so the celebration feels earned rather than arbitrary. */
object ViewerMilestones {
    private val steps = listOf(100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000, 100_000)

    fun crossed(previous: Int, current: Int): Int? = steps.lastOrNull { it in (previous + 1)..current }
}
