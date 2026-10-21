package org.mayad.fame_life.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_live
import famelife.shared.generated.resources.home_mission_start
import famelife.shared.generated.resources.hud_level_short
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseColors
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec

/*
 * The game-HUD layer of Pulse: currency pills, lit panels with a tab header, segmented progress, stat cells and
 * collectible medals. Everything is drawn with Canvas and brushes (no bitmaps, no shaders), so it renders the same on
 * every Android version and on iOS.
 */

/** Smallest visible fill, so an empty bar still reads as a bar. */
private const val MIN_FILL = 0.03f
private const val SEGMENTS = 10
private const val LEVEL_RING_START = 135f
private const val LEVEL_RING_SWEEP = 270f
private const val LOCKED_ALPHA = 0.55f
private const val RING_SPIN_MS = 3200

/** The fame-point coin: a minted gold disc with a raised rim, an embossed star and a specular sweep. */
@Composable
fun CoinEmblem(modifier: Modifier = Modifier, size: Dp = Pulse.dimens.emblem) {
    val colors = Pulse.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawCoin(colors) }
        PulseIcon(Glyphs.StarFilled, colors.coinRim, size = size * 0.46f)
    }
}

private fun DrawScope.drawCoin(colors: PulseColors) {
    val r = size.minDimension / 2
    val c = center
    // Warm glow pool under the coin.
    drawCircle(Brush.radialGradient(listOf(colors.gold.copy(alpha = 0.45f), Color.Transparent), c, r * 1.5f), r * 1.5f, c)
    drawCircle(Brush.verticalGradient(listOf(colors.goldLight, colors.goldDeep, colors.coinRim), c.y - r, c.y + r), r, c)
    drawCircle(
        Brush.radialGradient(listOf(colors.goldLight, colors.coinFace, colors.gold), Offset(c.x - r * 0.3f, c.y - r * 0.35f), r),
        r * 0.78f,
        c,
    )
    drawCircle(colors.coinRim.copy(alpha = 0.5f), r * 0.78f, c, style = Stroke(r * 0.06f))
    // Specular arc across the upper-left of the face.
    drawArc(
        colors.highlight.copy(alpha = 0.65f),
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
        size = Size(r * 1.24f, r * 1.24f),
        style = Stroke(r * 0.1f, cap = StrokeCap.Round),
    )
}

/**
 * A HUD currency or counter pill: the emblem overlaps the pill's leading edge like a game wallet, the number sits in
 * bold numeric type. [value] eases to new totals.
 */
@Composable
fun HudPill(
    value: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    accent: Color = Pulse.colors.gold,
    emblem: @Composable () -> Unit = { CoinEmblem() },
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(modifier.semantics(mergeDescendants = true) { this.contentDescription = contentDescription }, contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .padding(start = dimens.emblem / 2)
                .height(dimens.hudPill)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)), size.width * 0.6f) }
                .clip(Pulse.shapes.pill)
                .background(colors.hudPanelBrush)
                .border(dimens.hairline, colors.hudRimBrush, Pulse.shapes.pill)
                .drawBehind { drawRect(Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.12f), Color.Transparent), 0f, size.height * 0.5f)) }
                .padding(start = dimens.emblem / 2 + dimens.sm, end = dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedCount(value, Pulse.type.metricSmall, colors.textPrimary)
        }
        emblem()
    }
}

/** A glossy flame emblem for streak and heat counters. */
@Composable
fun FlameEmblem(modifier: Modifier = Modifier, size: Dp = Pulse.dimens.emblem) {
    GlossDisc(Gloss.Coral, Glyphs.Flame, modifier, size)
}

/** A round glossy emblem: gradient disc, specular cap and a light glyph. Shared by emblems and medals. */
@Composable
fun GlossDisc(gloss: Gloss, glyph: ImageVector, modifier: Modifier = Modifier, size: Dp = Pulse.dimens.emblem, glyphScale: Float = 0.5f) {
    val colors = Pulse.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawGlossDisc(gloss, colors) }
        PulseIcon(glyph, colors.highlight, size = size * glyphScale)
    }
}

private fun DrawScope.drawGlossDisc(gloss: Gloss, colors: PulseColors) {
    val r = size.minDimension / 2
    val c = center
    drawCircle(Brush.radialGradient(listOf(gloss.glow.copy(alpha = 0.4f), Color.Transparent), c, r * 1.45f), r * 1.45f, c)
    drawCircle(Brush.verticalGradient(listOf(gloss.top, gloss.mid, gloss.bottom), c.y - r, c.y + r), r, c)
    drawOval(
        Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.5f), Color.Transparent), c.y - r * 0.92f, c.y),
        topLeft = Offset(c.x - r * 0.66f, c.y - r * 0.9f),
        size = Size(r * 1.32f, r * 0.9f),
    )
    drawCircle(colors.highlight.copy(alpha = 0.35f), r - r * 0.04f, c, style = Stroke(r * 0.08f))
}

/**
 * Round glossy control with a halo ring, for HUD actions (inbox, settings). A count above zero shows a live-red
 * badge; the description must say the same thing for screen readers.
 */
@Composable
fun HudIconButton(
    glyph: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    gloss: Gloss = Gloss.Blue,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(
        modifier
            .size(dimens.touch)
            .pressable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dimens.glassButtonSmall)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(gloss.glow.copy(alpha = 0.35f), Color.Transparent)), size.width * 0.75f)
                    drawCircle(gloss.glow.copy(alpha = 0.55f), size.width / 2, style = Stroke(dimens.stroke.toPx()))
                }
                .padding(dimens.xxs),
        ) { GlossDisc(gloss, glyph, Modifier.fillMaxSize(), glyphScale = 0.46f) }
        if (badge > 0) {
            Text(
                compactCount(badge),
                style = Pulse.type.overline,
                color = colors.onMedia,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimens.xxs)
                    .widthIn(min = dimens.iconSmall)
                    .clip(Pulse.shapes.pill)
                    .background(colors.liveGradient)
                    .border(dimens.hairline, colors.hudPanel, Pulse.shapes.pill)
                    .padding(horizontal = dimens.xs),
            )
        }
    }
}

/**
 * A lit navy panel: top-down gradient body, a cool rim brighter at the top and an inner specular edge. With a
 * [title], a glossy blue tab sits across the top border, the way game dashboards label their panels.
 */
@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    tab: Brush = Pulse.colors.hudHeaderBrush,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val shape = Pulse.shapes.panel
    Box(modifier) {
        Column(
            Modifier
                .then(if (title != null) Modifier.padding(top = dimens.panelTabHeight / 2) else Modifier)
                .fillMaxWidth()
                .clip(shape)
                .background(colors.hudPanelBrush)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.07f), Color.Transparent), 0f, size.height * 0.3f))
                }
                .border(dimens.hairline, colors.hudRimBrush, shape)
                .padding(start = dimens.lg, end = dimens.lg, bottom = dimens.lg, top = if (title != null) dimens.xl else dimens.lg),
            content = content,
        )
        if (title != null) {
            Row(
                Modifier
                    .padding(start = dimens.lg)
                    .height(dimens.panelTabHeight)
                    .clip(Pulse.shapes.pill)
                    .background(tab)
                    .drawBehind { drawRect(Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.35f), Color.Transparent), 0f, size.height * 0.55f)) }
                    .border(dimens.hairline, colors.highlight.copy(alpha = 0.35f), Pulse.shapes.pill)
                    .padding(horizontal = dimens.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = Pulse.type.overline, color = colors.textOnAccent, maxLines = 1)
            }
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(end = dimens.lg)) { trailing() }
        }
    }
}

/**
 * Segmented progress: a recessed track split into ticks, a gradient fill with a glowing tip. The animated value is
 * read only in the draw phase.
 */
@Composable
fun HudProgressBar(fraction: Float, modifier: Modifier = Modifier, fill: Brush = Pulse.colors.goldGradient, glow: Color = Pulse.colors.gold) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val shown = animateFloatAsState(fraction.coerceIn(0f, 1f), Motion.spec(Motion.reveal()), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(dimens.track * 1.6f)
            .drawWithCache {
                val radius = CornerRadius(size.height / 2)
                val gap = dimens.hairline.toPx() * 2
                onDrawBehind {
                    drawRoundRect(colors.background.copy(alpha = 0.7f), cornerRadius = radius)
                    drawRoundRect(colors.hudRim, cornerRadius = radius, style = Stroke(dimens.hairline.toPx()))
                    val width = size.width * shown.value.coerceAtLeast(MIN_FILL)
                    drawRoundRect(fill, size = Size(width, size.height), cornerRadius = radius)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.45f), Color.Transparent), 0f, size.height * 0.6f),
                        size = Size(width, size.height * 0.5f),
                        cornerRadius = radius,
                    )
                    val step = size.width / SEGMENTS
                    for (i in 1 until SEGMENTS) {
                        drawRect(colors.hudPanel, topLeft = Offset(step * i - gap / 2, 0f), size = Size(gap, size.height))
                    }
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.8f), Color.Transparent), Offset(width, size.height / 2), size.height * 1.6f), size.height * 1.6f, Offset(width, size.height / 2))
                }
            },
    )
}

/** One HUD statistic: glossy emblem, big number, small caps label. */
@Composable
fun HudStat(value: String, label: String, glyph: ImageVector, gloss: Gloss, modifier: Modifier = Modifier, valueColor: Color = Pulse.colors.textPrimary) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .clip(Pulse.shapes.control)
            .background(colors.background.copy(alpha = 0.45f))
            .border(dimens.hairline, colors.hudRim, Pulse.shapes.control)
            .padding(horizontal = dimens.sm, vertical = dimens.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossDisc(gloss, glyph, size = dimens.tileXs)
        Spacer(Modifier.width(dimens.sm))
        Column {
            Text(value, style = Pulse.type.metricSmall, color = valueColor, maxLines = 1)
            Text(label.uppercase(), style = Pulse.type.overline, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * A collectible medal. Earned: gold sweep rim around a glossy disc in the badge's color. Locked: a dim slate disc
 * with a lock, so the state never depends on color alone.
 */
@Composable
fun AchievementMedal(glyph: ImageVector, gloss: Gloss, unlocked: Boolean, contentDescription: String, modifier: Modifier = Modifier, size: Dp = Pulse.dimens.medal) {
    val colors = Pulse.colors
    Box(
        modifier
            .size(size)
            .graphicsLayer { alpha = if (unlocked) 1f else LOCKED_ALPHA }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val rim = if (unlocked) {
                Brush.sweepGradient(listOf(colors.goldLight, colors.goldDeep, colors.gold, colors.goldLight, colors.coinRim, colors.goldLight))
            } else {
                Brush.sweepGradient(listOf(Gloss.Slate.top, Gloss.Slate.bottom, Gloss.Slate.top))
            }
            drawCircle(rim, r)
            drawCircle(colors.hudPanel, r * 0.84f)
        }
        GlossDisc(if (unlocked) gloss else Gloss.Slate, if (unlocked) glyph else Glyphs.Lock, size = size * 0.74f)
    }
}

/**
 * The player's avatar inside a 270° level ring, with the level number on a small tab at the bottom, like a game
 * profile badge. [fraction] is progress toward the next level.
 */
@Composable
fun LevelAvatar(spec: ArtSpec, level: Int, fraction: Float, contentDescription: String, modifier: Modifier = Modifier, size: Dp = Pulse.dimens.avatarMd) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val shown = animateFloatAsState(fraction.coerceIn(0f, 1f), Motion.spec(Motion.reveal()), label = "level")
    Box(modifier.size(size + dimens.sm).semantics(mergeDescendants = true) { this.contentDescription = contentDescription }) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val stroke = dimens.ring.toPx() * 1.4f
                    val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val fill = Brush.sweepGradient(listOf(colors.cyan, colors.primary, colors.violet, colors.gold, colors.cyan))
                    onDrawBehind {
                        drawArc(colors.hudRim, LEVEL_RING_START, LEVEL_RING_SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(fill, LEVEL_RING_START, LEVEL_RING_SWEEP * shown.value.coerceAtLeast(MIN_FILL), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                },
        )
        CreatorAvatar(spec, size - dimens.xs, Modifier.align(Alignment.Center))
        Text(
            stringResource(Res.string.hud_level_short, level),
            style = Pulse.type.overline,
            color = colors.onLight,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = dimens.xxs)
                .height(dimens.levelBadge)
                .clip(Pulse.shapes.pill)
                .background(colors.goldGradient)
                .border(dimens.hairline, colors.goldLight, Pulse.shapes.pill)
                .padding(horizontal = dimens.sm, vertical = dimens.xxs),
        )
    }
}

/** A dimensional glossy button: three-tone body, specular cap and a rim. For in-panel calls to action. */
@Composable
fun GlossyPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, gloss: Gloss = Gloss.Gold, leading: ImageVector? = null, ink: Color = Pulse.colors.onLight) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .height(dimens.chipHeight)
            .pressable(onClick = onClick)
            .drawBehind { drawRoundRect(Brush.radialGradient(listOf(gloss.glow.copy(alpha = 0.35f), Color.Transparent)), cornerRadius = CornerRadius(size.height)) }
            .clip(Pulse.shapes.pill)
            .background(Brush.verticalGradient(listOf(gloss.top, gloss.mid, gloss.bottom)))
            .drawBehind { drawRect(Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.45f), Color.Transparent), 0f, size.height * 0.5f)) }
            .border(dimens.hairline, colors.highlight.copy(alpha = 0.5f), Pulse.shapes.pill)
            .padding(horizontal = dimens.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            PulseIcon(leading, ink, size = dimens.iconSmall)
            Spacer(Modifier.width(dimens.xs))
        }
        Text(text, style = Pulse.type.label, color = ink, maxLines = 1)
    }
}

/**
 * A spinning live ring for story portraits. The rotation is read in the layer, so it never recomposes; with reduced
 * motion the ring holds still.
 */
@Composable
fun LiveRing(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val spin = if (Pulse.reducedMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "ring").animateFloat(0f, 360f, infiniteRepeatable(tween(RING_SPIN_MS, easing = LinearEasing)), label = "spin")
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = spin?.value ?: 0f }
                .drawWithCache {
                    val ring = Brush.sweepGradient(listOf(colors.live, colors.coral, colors.gold, colors.violet, colors.live))
                    val stroke = dimens.ring.toPx() * 1.5f
                    onDrawBehind {
                        drawCircle(Brush.radialGradient(listOf(colors.live.copy(alpha = 0.35f), Color.Transparent)), size.minDimension * 0.62f)
                        drawCircle(ring, (size.minDimension - stroke) / 2, style = Stroke(stroke))
                    }
                },
        )
        content()
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

private val PreviewArt = ArtSpec(3, ArtMotif.PORTRAIT, 42)

@ComponentPreviews
@Composable
private fun HudPillsPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md), verticalAlignment = Alignment.CenterVertically) {
            LevelAvatar(PreviewArt, level = 2, fraction = 0.62f, contentDescription = "")
            HudPill(1_840, "")
            HudPill(4, "", accent = Pulse.colors.coral, emblem = { FlameEmblem() })
            HudIconButton(Glyphs.Bell, "", {}, badge = 3)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            AchievementMedal(Glyphs.Mic, Gloss.Violet, unlocked = true, contentDescription = "")
            AchievementMedal(Glyphs.Gift, Gloss.Gold, unlocked = false, contentDescription = "")
            CoinEmblem(size = Pulse.dimens.emblemLarge)
        }
    }
}

@ComponentPreviews
@Composable
private fun HudPanelPreview() = PreviewContainer {
    HudPanel(Modifier.padding(Pulse.dimens.gutter), title = stringResource(Res.string.common_live)) {
        HudProgressBar(0.46f)
        Spacer(Modifier.height(Pulse.dimens.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            HudStat("7", stringResource(Res.string.common_live), Glyphs.Mic, Gloss.Violet, Modifier.weight(1f))
            HudStat("2.4K", stringResource(Res.string.common_live), Glyphs.Eye, Gloss.Cyan, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Pulse.dimens.md))
        GlossyPillButton(stringResource(Res.string.home_mission_start), {}, leading = Glyphs.Play)
        Spacer(Modifier.height(Pulse.dimens.md))
        LiveRing(Modifier.size(Pulse.dimens.storyAvatar)) { CreatorAvatar(PreviewArt, Pulse.dimens.storyAvatar - Pulse.dimens.md) }
    }
}
