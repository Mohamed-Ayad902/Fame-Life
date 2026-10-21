package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Raw brand values. Screens use the semantic roles in [PulseColors]; these exist for brand assets and art. */
object PulseBrand {
    val Midnight = Color(0xFF080B14)
    val Blue = Color(0xFF4C7DFF)
    val BlueLight = Color(0xFF7FA2FF)
    val BlueDeep = Color(0xFF2F55D8)
    val Violet = Color(0xFF945CFF)
    val Cyan = Color(0xFF55E5F2)
    val Gold = Color(0xFFFFC66D)
    val GoldLight = Color(0xFFFFE29A)
    val GoldDeep = Color(0xFFFF9F43)
    val Coral = Color(0xFFFF8A5B)
    val Rose = Color(0xFFFF4D6D)
}

/**
 * Semantic color tokens. Blue and violet carry the identity, cyan marks AI and selection, warm tones are reserved for
 * gifts and celebration. Everything drawn over photos or the camera uses the `onMedia` / `glass` / `mediaScrim`
 * family, so text stays readable on bright and dark frames alike.
 */
@Immutable
data class PulseColors(
    val background: Color,
    val backgroundRaised: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val navSurface: Color,
    val border: Color,
    val borderStrong: Color,
    val primary: Color,
    val primaryLight: Color,
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
    val goldLight: Color,
    val goldDeep: Color,
    val coral: Color,
    val scrim: Color,
    /** Text and icons drawn on photos, artwork and the camera. */
    val onMedia: Color,
    /** Dark ink for text on bright light-colored chips (avatar initials). */
    val onLight: Color,
    /** Specular highlight for glossy surfaces; alpha is applied where it's drawn. */
    val highlight: Color,
    /** Shadow ink; alpha is applied where it's drawn. */
    val shadow: Color,
    /** Bottom fade over artwork so captions stay readable. */
    val mediaScrim: Color,
    val mediaScrimSoft: Color,
    /** Translucent pills over the camera and artwork, light to strong. */
    val glassFaint: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    /** Bubble for critical (but civil) audience comments. */
    val criticalBubble: Color,
    /** Behind the camera before frames arrive. */
    val cameraBase: Color,
    /** Game-HUD panels: a deep navy body lit from the top, with a cool blue rim. */
    val hudPanel: Color,
    val hudPanelTop: Color,
    val hudRim: Color,
    val hudRimBright: Color,
    /** Tint over the blurred, pinned HUD; [hudGlassFallback] where the platform can't blur. */
    val hudGlass: Color,
    val hudGlassFallback: Color,
    /** Tint over the blurred tab bar; [navSurface] stays the opaque fallback. */
    val navGlass: Color,
    /** Metallic coin and medal faces. */
    val coinRim: Color,
    val coinFace: Color,
) {
    // Built once per palette instead of on every read.
    val brandGradient: Brush = Brush.linearGradient(listOf(primary, violet))
    val liveGradient: Brush = Brush.linearGradient(listOf(live, coral))
    val goldGradient: Brush = Brush.linearGradient(listOf(goldLight, gold, goldDeep))
    val orbGradient: Brush = Brush.linearGradient(listOf(primaryLight, primary, violet))
    val hudPanelBrush: Brush = Brush.verticalGradient(listOf(hudPanelTop, hudPanel))
    val hudRimBrush: Brush = Brush.verticalGradient(listOf(hudRimBright, hudRim))
    val hudHeaderBrush: Brush = Brush.verticalGradient(listOf(primaryLight, primary, PulseBrand.BlueDeep))
    val successGradient: Brush = Brush.linearGradient(listOf(success, cyan))
}

val DarkPulseColors = PulseColors(
    background = PulseBrand.Midnight,
    backgroundRaised = Color(0xFF101526),
    surface = Color(0xFF151D32),
    surfaceElevated = Color(0xFF1D2843),
    surfaceGlass = Color(0xCC121A2E),
    navSurface = Color(0xF00D1220),
    border = Color(0x1AFFFFFF),
    borderStrong = Color(0x33FFFFFF),
    primary = PulseBrand.Blue,
    primaryLight = PulseBrand.BlueLight,
    violet = PulseBrand.Violet,
    cyan = PulseBrand.Cyan,
    textPrimary = Color(0xFFF7F8FC),
    textSecondary = Color(0xFFA5AEC4),
    textMuted = Color(0xFF717C96),
    textOnAccent = Color.White,
    live = PulseBrand.Rose,
    success = Color(0xFF52D6A0),
    warning = PulseBrand.Gold,
    error = Color(0xFFFF647C),
    gold = PulseBrand.Gold,
    goldLight = PulseBrand.GoldLight,
    goldDeep = PulseBrand.GoldDeep,
    coral = PulseBrand.Coral,
    scrim = Color(0x99050710),
    onMedia = Color.White,
    onLight = PulseBrand.Midnight,
    highlight = Color.White,
    shadow = Color.Black,
    mediaScrim = Color(0xE6070A14),
    mediaScrimSoft = Color(0xAA05070D),
    glassFaint = Color(0x55080B14),
    glass = Color(0x66080B14),
    glassStrong = Color(0x99080B14),
    glassBorder = Color(0x22FFFFFF),
    criticalBubble = Color(0x553A1420),
    cameraBase = Color.Black,
    hudPanel = Color(0xFF0C1430),
    hudPanelTop = Color(0xFF16245A),
    hudRim = Color(0x334C7DFF),
    hudRimBright = Color(0x997FA2FF),
    hudGlass = Color(0xB30A1024),
    hudGlassFallback = Color(0xF20A1024),
    navGlass = Color(0x8C0D1220),
    coinRim = Color(0xFFB86B12),
    coinFace = Color(0xFFFFD36B),
)
