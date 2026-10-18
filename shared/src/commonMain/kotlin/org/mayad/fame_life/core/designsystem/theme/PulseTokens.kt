package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Spacing scale. Browsing screens breathe more than the live overlay, which stays compact. */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val xxxl = 40.dp
    val screen = 20.dp
}

/**
 * Shape language: big radii for hero panels, moderate for controls, tight for metadata, full for pills and
 * avatars. Feed art alternates radii so the grid does not read as one repeated card.
 */
object Shapes {
    val hero = RoundedCornerShape(32.dp)
    val panel = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(20.dp)
    val control = RoundedCornerShape(16.dp)
    val tag = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val bubble = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
}

/** Motion tokens. Use these instead of ad-hoc durations so the app moves with one rhythm. */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val FAST = 140
    const val STANDARD = 260
    const val EMPHASIZED = 420
    const val REVEAL = 700

    fun <T> press() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> settle() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    fun <T> standard() = tween<T>(STANDARD, easing = emphasized)
    fun <T> enter() = tween<T>(EMPHASIZED, easing = decelerate)
    fun <T> exit() = tween<T>(FAST, easing = accelerate)
    val slide = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))
}
