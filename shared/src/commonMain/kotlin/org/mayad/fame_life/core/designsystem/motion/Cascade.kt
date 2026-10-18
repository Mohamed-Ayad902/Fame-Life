package org.mayad.fame_life.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.platform.monotonicMillis
import kotlin.math.min

/**
 * Remembers which items of a screen have already appeared, so each one rises in exactly once: scrolling back to it,
 * or recomposing, never replays the entrance. Items that appear close together form a batch and start
 * [Motion.CASCADE_STAGGER] ms apart, capped at [Motion.CASCADE_MAX_STAGGERED] so long lists never wait.
 */
@Stable
class CascadeState internal constructor(
    internal val enabled: Boolean,
    internal val rise: Dp,
    seen: Collection<String> = emptyList(),
) {
    internal val seen = seen.toMutableSet()
    private var lastStart = Long.MIN_VALUE / 2
    private var batchIndex = 0

    /** Returns the start delay in ms when [key] should animate now, or null when it was already seen. */
    internal fun claim(key: String): Long? {
        if (!seen.add(key) || !enabled) return null
        val now = monotonicMillis()
        if (now - lastStart > Motion.CASCADE_BATCH_WINDOW) batchIndex = 0
        lastStart = now
        return (min(batchIndex++, Motion.CASCADE_MAX_STAGGERED) * Motion.CASCADE_STAGGER).toLong()
    }
}

/** One per screen. Survives navigating away and back (saved with the screen), so nothing replays. */
@Composable
fun rememberCascadeState(): CascadeState {
    val enabled = !Pulse.reducedMotion
    val rise = Pulse.dimens.xxl
    return rememberSaveable(
        enabled,
        saver = listSaver(save = { it.seen.toList() }, restore = { CascadeState(enabled, rise, it) }),
    ) { CascadeState(enabled, rise) }
}

/**
 * Fades and lifts the element in the first time [key] is shown. Animation values are read only in the placement
 * layer, so the entrance never recomposes the item.
 */
fun Modifier.cascadeIn(state: CascadeState, key: String): Modifier = this then CascadeElement(state, key)

private data class CascadeElement(val state: CascadeState, val key: String) : ModifierNodeElement<CascadeNode>() {
    override fun create() = CascadeNode(state, key)
    override fun update(node: CascadeNode) {
        node.state = state
        node.key = key
    }
}

private class CascadeNode(var state: CascadeState, var key: String) : Modifier.Node(), LayoutModifierNode {
    private var progress: Animatable<Float, *>? = null

    override fun onAttach() {
        val startDelay = state.claim(key) ?: return
        val animatable = Animatable(0f).also { progress = it }
        coroutineScope.launch {
            delay(startDelay)
            animatable.animateTo(1f, Motion.cascade())
            progress = null
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val risePx = state.rise.toPx()
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val p = progress?.value ?: 1f
                alpha = p
                translationY = (1f - p) * risePx
            }
        }
    }
}
