package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.mayad.fame_life.core.platform.isReduceMotionEnabled
import kotlin.math.min

private val LocalPulseColors = staticCompositionLocalOf { DarkPulseColors }
private val LocalPulseType = staticCompositionLocalOf<PulseTypography?> { null }
private val LocalPulseDimens = compositionLocalOf { pulseDimens(PulseScale.Compact, PulseWidth.Compact) }
private val LocalPulseShapes = compositionLocalOf { pulseDimens(PulseScale.Compact, PulseWidth.Compact).toShapes() }
private val LocalPulseWidth = compositionLocalOf { PulseWidth.Compact }
private val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * The single design-system root. Everything visual reads from [Pulse]: `Pulse.colors.cyan`, `Pulse.type.title`,
 * `Pulse.dimens.gutter`, `Pulse.shapes.card`, `Pulse.width`, `Pulse.reducedMotion`.
 *
 * Dimens and type are recomputed when the window changes (rotation, foldables, split screen): sizes scale by the
 * window's shortest side, while [PulseWidth] (by width) decides when a screen splits into panes.
 */
@Composable
fun PulseTheme(
    reducedMotion: Boolean = remember { isReduceMotionEnabled() },
    content: @Composable () -> Unit,
) {
    val colors = DarkPulseColors
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val widthDp = with(density) { window.width.toDp().value }
    val heightDp = with(density) { window.height.toDp().value }
    // Before the first layout the window can report zero; fall back to the phone baseline.
    val scale = if (widthDp <= 0f) PulseScale.Compact else scaleFor(min(widthDp, heightDp))
    val width = if (widthDp <= 0f) PulseWidth.Compact else widthClassFor(widthDp)
    val dimens = remember(scale, width) { pulseDimens(scale, width) }
    val shapes = remember(dimens) { dimens.toShapes() }
    val type = rememberPulseTypography(scale)
    val scheme = remember(colors) {
        darkColorScheme(
            primary = colors.primary,
            secondary = colors.violet,
            tertiary = colors.cyan,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceElevated,
            onPrimary = colors.textOnAccent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.borderStrong,
            error = colors.error,
        )
    }
    CompositionLocalProvider(
        LocalPulseColors provides colors,
        LocalPulseType provides type,
        LocalPulseDimens provides dimens,
        LocalPulseShapes provides shapes,
        LocalPulseWidth provides width,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = type.toMaterial(), content = content)
    }
}

/** Single entry point for design tokens. */
object Pulse {
    val colors: PulseColors
        @Composable @ReadOnlyComposable get() = LocalPulseColors.current

    val type: PulseTypography
        @Composable @ReadOnlyComposable get() = LocalPulseType.current ?: error("Pulse.type used outside PulseTheme")

    val dimens: PulseDimens
        @Composable @ReadOnlyComposable get() = LocalPulseDimens.current

    val shapes: PulseShapes
        @Composable @ReadOnlyComposable get() = LocalPulseShapes.current

    /** Layout class for composition decisions (columns, side-by-side panes). */
    val width: PulseWidth
        @Composable @ReadOnlyComposable get() = LocalPulseWidth.current

    /** True when the OS asks for less motion. Decorative animation must snap or stay still. */
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}
