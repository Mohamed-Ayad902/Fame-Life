package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens. Components read these, never raw hex values, so the palette can evolve in one place.
 * Blue and violet carry the identity, cyan marks AI and selection, warm tones are reserved for gifts and
 * celebration.
 */
@Immutable
data class PulseColors(
    val background: Color,
    val backgroundRaised: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val border: Color,
    val borderStrong: Color,
    val primary: Color,
    val violet: Color,
    val cyan: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnAccent: Color,
    val live: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val gold: Color,
    val coral: Color,
    val scrim: Color,
) {
    val brandGradient: Brush get() = Brush.linearGradient(listOf(primary, violet))
    val liveGradient: Brush get() = Brush.linearGradient(listOf(live, coral))
    val goldGradient: Brush get() = Brush.linearGradient(listOf(Color(0xFFFFE29A), gold, Color(0xFFFF9F43)))
}

val DarkPulseColors = PulseColors(
    background = Color(0xFF080B14),
    backgroundRaised = Color(0xFF101526),
    surface = Color(0xFF151D32),
    surfaceElevated = Color(0xFF1D2843),
    surfaceGlass = Color(0xCC121A2E),
    border = Color(0x1AFFFFFF),
    borderStrong = Color(0x33FFFFFF),
    primary = Color(0xFF4C7DFF),
    violet = Color(0xFF945CFF),
    cyan = Color(0xFF55E5F2),
    textPrimary = Color(0xFFF7F8FC),
    textSecondary = Color(0xFFA5AEC4),
    textMuted = Color(0xFF717C96),
    textOnAccent = Color(0xFFFFFFFF),
    live = Color(0xFFFF4D6D),
    success = Color(0xFF52D6A0),
    warning = Color(0xFFFFC66D),
    error = Color(0xFFFF647C),
    gold = Color(0xFFFFC66D),
    coral = Color(0xFFFF8A5B),
    scrim = Color(0x99050710),
)

val LocalPulseColors = staticCompositionLocalOf { DarkPulseColors }
val LocalPulseType = staticCompositionLocalOf<PulseTypography> { error("PulseTheme not applied") }

object Pulse {
    val colors: PulseColors @Composable get() = LocalPulseColors.current
    val type: PulseTypography @Composable get() = LocalPulseType.current
}

/** Dark-first theme. Material components that remain in use inherit a matching scheme. */
@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    val colors = DarkPulseColors
    val type = rememberPulseTypography()
    val scheme = darkColorScheme(
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
    CompositionLocalProvider(LocalPulseColors provides colors, LocalPulseType provides type) {
        MaterialTheme(colorScheme = scheme, typography = type.toMaterial(), content = content)
    }
}
