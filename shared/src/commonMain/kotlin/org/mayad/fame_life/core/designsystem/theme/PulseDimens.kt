package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * Every size in the app comes from here; screens never write `16.dp`.
 *
 * One base set (a 360–599dp phone) is multiplied by a [PulseScale] picked from the window's *shortest side*, so a
 * landscape phone doesn't suddenly get tablet-sized padding. Layout changes (columns, side-by-side panes) are driven
 * separately by [PulseWidth]. Touch targets never shrink below 48dp.
 */
@Immutable
data class PulseDimens(
    // Rhythm
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,
    /** Screen side margin. Grows with width class. */
    val gutter: Dp,

    // Lines
    val hairline: Dp,
    val stroke: Dp,
    val ring: Dp,

    // Icons and touch
    val touch: Dp,
    val iconTiny: Dp,
    val iconSmall: Dp,
    val icon: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,

    // Glossy 3D tiles
    val tileXs: Dp,
    val tileSm: Dp,
    val tileMd: Dp,
    val tileLg: Dp,
    val tileXl: Dp,
    val tileXxl: Dp,
    val tileHero: Dp,

    // Avatars
    val avatarXs: Dp,
    val avatarSm: Dp,
    val avatarMd: Dp,
    val avatarLg: Dp,
    val avatarXl: Dp,
    val avatarHero: Dp,

    // Controls
    val buttonHeight: Dp,
    val buttonHeightCompact: Dp,
    val chipHeight: Dp,
    val segmentHeight: Dp,
    val glassButton: Dp,
    val glassButtonSmall: Dp,
    val fieldPaddingVertical: Dp,
    val dot: Dp,
    val dotSmall: Dp,
    val badgeDot: Dp,
    val track: Dp,

    // Navigation
    val navBarHeight: Dp,
    val navItemWidth: Dp,
    val navItemHeight: Dp,
    val navElevation: Dp,
    val createOrb: Dp,
    val createOrbLift: Dp,
    val navRailWidth: Dp,
    /** Space kept free at the bottom of tab content so the floating bar never covers it. */
    val bottomBarClearance: Dp,

    // Cards and media
    val railCard: Dp,
    val railCardWide: Dp,
    val railCardNarrow: Dp,
    val storyItem: Dp,
    val heroCardHeight: Dp,
    val storyAvatar: Dp,
    val profileHeroHeight: Dp,
    val profileCoverHeight: Dp,
    val setupStageHeight: Dp,
    val setupStageTop: Dp,
    val brandMark: Dp,

    // Live
    val liveChatMaxWidth: Dp,
    val liveChatMaxHeight: Dp,
    val liveControlsClearance: Dp,
    val liveChatClearance: Dp,
    val giftSmall: Dp,
    val giftMedium: Dp,
    val giftMajor: Dp,
    val reactionLane: Dp,
    /** Sideways sway and drift of a reaction particle. */
    val reactionSway: Dp,
    val reactionDrift: Dp,
    val endButtonWidth: Dp,

    // Game HUD
    val hudPill: Dp,
    val emblem: Dp,
    val emblemLarge: Dp,
    val medal: Dp,
    val panelTabHeight: Dp,
    val levelBadge: Dp,
    val topicCard: Dp,
    /** Backdrop blur radius behind pinned glass (the Home HUD). */
    val hudBlur: Dp,

    /** Max width for reading and forms on wide windows. */
    val contentMaxWidth: Dp,

    // Corner radii (shapes are built from these)
    val radiusTag: Dp,
    val radiusControl: Dp,
    val radiusCard: Dp,
    val radiusPanel: Dp,
    val radiusHero: Dp,
    val radiusSheet: Dp,
    val radiusBubbleTail: Dp,
    val radiusBubble: Dp,
)

/** Size buckets by the window's shortest side. */
enum class PulseScale(val factor: Float, val typeFactor: Float) {
    Small(0.9f, 0.94f), // < 360dp
    Compact(1f, 1f), // 360–599dp: the design baseline
    Medium(1.12f, 1.06f), // 600–839dp: small tablets, unfolded foldables
    Large(1.22f, 1.12f), // >= 840dp
}

/** Layout (not size) classes by window width. Composition changes at these. */
enum class PulseWidth {
    Compact,
    Medium,
    Expanded,
    ;

    /** True when a screen should show two panes side by side. */
    val split: Boolean get() = this != Compact
}

internal fun scaleFor(shortestSideDp: Float) = when {
    shortestSideDp < 360f -> PulseScale.Small
    shortestSideDp < 600f -> PulseScale.Compact
    shortestSideDp < 840f -> PulseScale.Medium
    else -> PulseScale.Large
}

internal fun widthClassFor(widthDp: Float) = when {
    widthDp < 600f -> PulseWidth.Compact
    widthDp < 840f -> PulseWidth.Medium
    else -> PulseWidth.Expanded
}

private val MinTouchTarget = 48.dp

internal fun pulseDimens(scale: PulseScale, width: PulseWidth): PulseDimens {
    val f = scale.factor
    fun d(base: Float): Dp = (base * f).dp
    return PulseDimens(
        xxs = d(2f), xs = d(4f), sm = d(8f), md = d(12f), lg = d(16f), xl = d(20f), xxl = d(28f), xxxl = d(40f),
        gutter = when (width) {
            PulseWidth.Compact -> d(20f)
            PulseWidth.Medium -> d(32f)
            PulseWidth.Expanded -> d(48f)
        },
        hairline = 1.dp, stroke = 1.5.dp, ring = 2.dp,
        touch = max(MinTouchTarget, d(48f)),
        iconTiny = d(12f), iconSmall = d(16f), icon = d(20f), iconMedium = d(24f), iconLarge = d(30f),
        tileXs = d(30f), tileSm = d(36f), tileMd = d(44f), tileLg = d(54f), tileXl = d(64f), tileXxl = d(84f), tileHero = d(96f),
        avatarXs = d(18f), avatarSm = d(32f), avatarMd = d(44f), avatarLg = d(64f), avatarXl = d(112f), avatarHero = d(132f),
        buttonHeight = max(MinTouchTarget, d(54f)),
        buttonHeightCompact = max(MinTouchTarget, d(50f)),
        chipHeight = d(38f), segmentHeight = d(40f),
        glassButton = max(MinTouchTarget, d(44f)), glassButtonSmall = d(40f),
        fieldPaddingVertical = d(15f),
        dot = d(6f), dotSmall = d(4f), badgeDot = d(9f), track = d(6f),
        navBarHeight = d(68f), navItemWidth = d(60f), navItemHeight = d(56f), navElevation = d(24f),
        createOrb = d(70f), createOrbLift = d(22f), navRailWidth = d(96f), bottomBarClearance = d(112f),
        railCard = d(148f), railCardWide = d(160f), railCardNarrow = d(132f), storyItem = d(68f),
        heroCardHeight = d(300f), storyAvatar = d(66f), profileHeroHeight = d(300f), profileCoverHeight = d(240f),
        setupStageHeight = d(140f), setupStageTop = d(120f), brandMark = d(112f),
        liveChatMaxWidth = d(300f), liveChatMaxHeight = d(300f),
        liveControlsClearance = d(140f), liveChatClearance = d(120f),
        giftSmall = d(72f), giftMedium = d(104f), giftMajor = d(148f), reactionLane = d(72f), reactionSway = d(16f), reactionDrift = d(28f), endButtonWidth = d(72f),
        hudPill = d(34f), emblem = d(24f), emblemLarge = d(64f), medal = d(48f), panelTabHeight = d(26f),
        levelBadge = d(20f), topicCard = d(168f), hudBlur = 24.dp,
        contentMaxWidth = d(560f),
        radiusTag = d(8f), radiusControl = d(16f), radiusCard = d(20f), radiusPanel = d(24f), radiusHero = d(32f),
        radiusSheet = d(28f), radiusBubbleTail = d(6f), radiusBubble = d(18f),
    )
}

/**
 * Shape language: big radii for hero panels, moderate for controls, tight for metadata, full for pills and avatars.
 */
@Immutable
data class PulseShapes(
    val tag: Shape,
    val control: Shape,
    val card: Shape,
    val panel: Shape,
    val hero: Shape,
    val sheet: Shape,
    /** Chat bubble: the tail corner sits at the start (leading) edge, so it flips in RTL. */
    val bubble: Shape,
    /** Masonry variant with one tight corner, so a feed grid doesn't read as identical cards. */
    val editorial: Shape,
    val pill: Shape = CircleShape,
)

internal fun PulseDimens.toShapes() = PulseShapes(
    tag = RoundedCornerShape(radiusTag),
    control = RoundedCornerShape(radiusControl),
    card = RoundedCornerShape(radiusCard),
    panel = RoundedCornerShape(radiusPanel),
    hero = RoundedCornerShape(radiusHero),
    sheet = RoundedCornerShape(topStart = radiusSheet, topEnd = radiusSheet),
    bubble = RoundedCornerShape(topStart = radiusBubbleTail, topEnd = radiusBubble, bottomEnd = radiusBubble, bottomStart = radiusBubble),
    editorial = RoundedCornerShape(topStart = radiusSheet, topEnd = radiusTag, bottomEnd = radiusSheet, bottomStart = radiusSheet),
)

/** Artwork crops (width / height), so feeds keep rhythm. */
object PulseAspect {
    const val Portrait = 4f / 5f
    const val Tall = 3f / 4f
    const val Phone = 0.66f
    const val Square = 1f
    const val Wide = 16f / 10f
}
