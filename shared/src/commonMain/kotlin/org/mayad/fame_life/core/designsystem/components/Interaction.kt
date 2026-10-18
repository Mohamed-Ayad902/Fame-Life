package org.mayad.fame_life.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import org.mayad.fame_life.core.designsystem.theme.Motion

/** Whether decorative motion should run. Provided once at the root from the OS setting. */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Tap handling for custom surfaces: a springy press-down instead of a ripple, which suits the layered, glassy
 * surfaces in this app. Keeps the semantics role so screen readers announce it correctly.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) pressedScale else 1f,
        animationSpec = Motion.press(),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun rememberReduceMotion(): Boolean = LocalReduceMotion.current

/**
 * Lets a child extend past its parent's horizontal padding, for edge-to-edge rails inside padded lists.
 * Negative padding is illegal in Compose, so this measures wider and offsets instead.
 */
fun Modifier.bleed(horizontal: androidx.compose.ui.unit.Dp): Modifier = this.then(
    androidx.compose.ui.Modifier.layout { measurable, constraints ->
        val extra = horizontal.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
    },
)
