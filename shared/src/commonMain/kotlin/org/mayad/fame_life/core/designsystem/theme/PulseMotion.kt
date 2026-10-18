package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens. Use these instead of ad-hoc durations so the app moves with one rhythm. Content arrives slowly
 * enough to be seen (the cascade), controls answer fast. Every spec has a reduced-motion variant via [Motion.spec].
 */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Long, soft landing for content entering the screen. */
    val easeOutSoft = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    const val FAST = 140
    const val STANDARD = 260
    const val EMPHASIZED = 420
    const val REVEAL = 700
    const val COUNT = 600

    /** Cascade: each item takes [CASCADE] ms and starts [CASCADE_STAGGER] ms after the previous one. */
    const val CASCADE = 550
    const val CASCADE_STAGGER = 70
    /** Items beyond this many in one batch start together, so long lists never wait. */
    const val CASCADE_MAX_STAGGERED = 8
    /** Items that start within this window of each other count as one batch. */
    const val CASCADE_BATCH_WINDOW = 120

    /** A reaction particle's rise, the gap between particles of one burst, and a major gift's light burst. */
    const val PARTICLE = 2000
    const val PARTICLE_STAGGER = 140
    const val GIFT_BURST = 1100

    /** Live badge breathing period. */
    const val BREATH = 900
    /** Onboarding countdown step before a live, and its reduced-motion version. */
    const val COUNTDOWN_STEP = 800
    const val COUNTDOWN_STEP_REDUCED = 500

    fun <T> press() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> settle() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    fun <T> standard() = tween<T>(STANDARD, easing = emphasized)
    fun <T> enter() = tween<T>(EMPHASIZED, easing = decelerate)
    fun <T> exit() = tween<T>(FAST, easing = accelerate)
    fun <T> reveal() = tween<T>(REVEAL, easing = emphasized)
    fun <T> cascade() = tween<T>(CASCADE, easing = easeOutSoft)
    val slide = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))

    /** Returns [spec] normally, or an instant snap when the user asked the OS for reduced motion. */
    @Composable
    fun <T> spec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (Pulse.reducedMotion) snap() else spec
}
