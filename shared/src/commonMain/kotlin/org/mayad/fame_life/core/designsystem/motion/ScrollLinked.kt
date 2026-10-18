package org.mayad.fame_life.core.designsystem.motion

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.mayad.fame_life.core.designsystem.theme.Pulse

/**
 * How far the first item has scrolled, in px. Returns a large value once it is fully gone. Read it only inside
 * graphicsLayer / draw lambdas so scrolling invalidates a layer, not composition.
 */
fun LazyListState.headerScroll(): Float = if (firstVisibleItemIndex > 0) Float.MAX_VALUE else firstVisibleItemScrollOffset.toFloat()

fun LazyStaggeredGridState.headerScroll(): Float = if (firstVisibleItemIndex > 0) Float.MAX_VALUE else firstVisibleItemScrollOffset.toFloat()

fun ScrollState.headerScroll(): Float = value.toFloat()

/**
 * A gentle scroll-linked header: as content scrolls under it, the header drifts up at a fraction of the speed,
 * fades and shrinks slightly. Snaps to static under reduced motion.
 */
@Composable
fun Modifier.scrollLinkedHeader(scroll: () -> Float): Modifier {
    if (Pulse.reducedMotion) return this
    return graphicsLayer {
        val distance = size.height.coerceAtLeast(1f)
        val offset = scroll().coerceAtMost(distance)
        val p = offset / distance
        translationY = offset * HEADER_DRIFT
        alpha = 1f - p * HEADER_FADE
        val s = 1f - p * HEADER_SHRINK
        scaleX = s
        scaleY = s
    }
}

/** Parallax for hero artwork: moves at [factor] of the scroll while its row is the first visible one. */
@Composable
fun Modifier.parallax(scroll: () -> Float, factor: Float = PARALLAX): Modifier {
    if (Pulse.reducedMotion) return this
    return graphicsLayer {
        val offset = scroll()
        translationY = if (offset == Float.MAX_VALUE) 0f else offset * factor
    }
}

private const val HEADER_DRIFT = 0.4f
private const val HEADER_FADE = 0.7f
private const val HEADER_SHRINK = 0.04f
private const val PARALLAX = 0.35f
