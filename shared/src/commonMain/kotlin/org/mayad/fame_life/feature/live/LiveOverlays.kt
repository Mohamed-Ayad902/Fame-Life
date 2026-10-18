package org.mayad.fame_life.feature.live

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.live_gift_detail
import famelife.shared.generated.resources.live_gift_sent
import famelife.shared.generated.resources.live_heard
import famelife.shared.generated.resources.live_joined
import famelife.shared.generated.resources.live_new_high
import famelife.shared.generated.resources.live_opening
import famelife.shared.generated.resources.live_reactions_count
import famelife.shared.generated.resources.live_trending
import famelife.shared.generated.resources.live_trending_default
import famelife.shared.generated.resources.live_vip
import famelife.shared.generated.resources.live_watching
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.domain.simulation.AudienceEvent
import org.mayad.fame_life.domain.simulation.AudienceMember
import org.mayad.fame_life.domain.simulation.CommentTone
import org.mayad.fame_life.domain.simulation.GiftTier
import org.mayad.fame_life.domain.simulation.ReactionKind
import kotlin.math.PI
import kotlin.math.sin

/** Particles per reaction burst: enough to read as a burst, few enough to stay cheap on a busy room. */
private const val MIN_PARTICLES = 3
private const val MAX_PARTICLES = 7
private const val GIFT_HALO = 1.8f
private const val COUNTDOWN_SCALE = 3
private const val AVATAR_SATURATION = 0.5f

fun AudienceMember.color(): Color = Color.hsv(hue, AVATAR_SATURATION, 1f)

/**
 * The comment stream. Newest at the bottom, older ones fade towards the top so the stream never becomes a wall
 * over the creator's face. Each comment animates in once, keyed by its id.
 */
@Composable
fun LiveCommentStream(comments: List<AudienceEvent.Comment>, modifier: Modifier = Modifier) {
    val fade = Pulse.colors.shadow
    Column(
        modifier
            .heightIn(max = Pulse.dimens.liveChatMaxHeight)
            // One offscreen layer for the fade mask, bounded to the chat column only.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.3f to fade), blendMode = BlendMode.DstIn)
            },
        verticalArrangement = Arrangement.spacedBy(Pulse.dimens.xs, Alignment.Bottom),
    ) {
        comments.forEach { comment -> key(comment.id) { CommentBubble(comment) } }
    }
}

@Composable
private fun CommentBubble(comment: AudienceEvent.Comment) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val reduce = Pulse.reducedMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, Motion.enter()) }
    val critical = comment.tone == CommentTone.CRITICAL
    val rise = dimens.xl
    Row(
        Modifier
            .widthIn(max = dimens.liveChatMaxWidth)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * rise.toPx()
            }
            .clip(Pulse.shapes.bubble)
            .background(if (critical) colors.criticalBubble else colors.glass)
            .padding(start = dimens.xs, top = dimens.xs, bottom = dimens.xs, end = dimens.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(dimens.iconMedium).clip(CircleShape).background(Brush.linearGradient(listOf(comment.author.color(), comment.author.color().copy(alpha = 0.5f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(comment.author.handle.take(1).uppercase(), style = Pulse.type.meta, color = colors.onLight)
        }
        Spacer(Modifier.width(dimens.sm))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (comment.author.isVip) colors.gold else colors.textSecondary)) {
                    append(comment.author.handle)
                    if (comment.author.isVip) append(" ★")
                }
                append("  ")
                withStyle(SpanStyle(color = colors.textPrimary)) { append(comment.text) }
            },
            style = Pulse.type.bodySmall,
        )
    }
}

/** Center-stage moments: gifts, milestones, VIP arrivals and trending. One at a time, announced to screen readers. */
@Composable
fun LiveSpotlight(event: AudienceEvent?, style: LiveStyle, modifier: Modifier = Modifier) {
    val reduce = Pulse.reducedMotion
    val dimens = Pulse.dimens
    AnimatedContent(
        targetState = event,
        contentKey = { it?.id },
        transitionSpec = {
            if (reduce) {
                fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())
            } else {
                (fadeIn(Motion.enter()) + scaleIn(Motion.settle(), initialScale = 0.6f)) togetherWith (fadeOut(Motion.standard()) + scaleOut(Motion.standard(), targetScale = 1.1f))
            }
        },
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        label = "spotlight",
    ) { current ->
        when (current) {
            is AudienceEvent.GiftSent -> GiftMoment(current, style)
            is AudienceEvent.Milestone -> Moment(
                art = { GlossyIcon(Glyphs.Eye, Gloss.Cyan, size = dimens.tileXl, glyphSize = dimens.iconLarge) },
                headline = stringResource(Res.string.live_watching, compactCount(current.viewers)),
                detail = stringResource(Res.string.live_new_high),
            )
            is AudienceEvent.VipJoined -> Moment(
                art = { GlossyIcon(Glyphs.Crown, Gloss.Gold, size = dimens.tileXl, glyphSize = dimens.iconLarge) },
                headline = stringResource(Res.string.live_joined, current.member.handle),
                detail = stringResource(Res.string.live_vip),
            )
            is AudienceEvent.TrendingMoment -> Moment(
                art = { GlossyIcon(Glyphs.Flame, Gloss.Coral, size = dimens.tileXl, glyphSize = dimens.iconLarge) },
                headline = stringResource(Res.string.live_trending),
                detail = current.reason ?: stringResource(Res.string.live_trending_default),
            )
            else -> Box(Modifier)
        }
    }
}

@Composable
private fun GiftMoment(event: AudienceEvent.GiftSent, style: LiveStyle) {
    val dimens = Pulse.dimens
    val reduce = Pulse.reducedMotion
    val size = when (event.gift.tier) {
        GiftTier.SMALL -> dimens.giftSmall
        GiftTier.MEDIUM -> dimens.giftMedium
        GiftTier.MAJOR -> dimens.giftMajor
    }
    val burst = remember { Animatable(0f) }
    // Bounded: one burst per gift, and small gifts get none.
    LaunchedEffect(Unit) { if (!reduce && event.gift.tier != GiftTier.SMALL) burst.animateTo(1f, tween(Motion.GIFT_BURST, easing = Motion.decelerate)) }
    val name = event.gift.label()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size * GIFT_HALO)
                .drawBehind {
                    // A light burst behind bigger gifts, read in the draw phase.
                    val radius = this.size.minDimension / 2 * (0.5f + burst.value * 0.5f)
                    drawCircle(
                        Brush.radialGradient(listOf(style.glow.copy(alpha = 0.45f * (1f - burst.value * 0.6f)), Color.Transparent), center, radius),
                        radius,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            GiftArt(event.gift.id, size = size, name = name)
        }
        MomentCaption(
            headline = stringResource(Res.string.live_gift_sent, event.from.handle, name),
            detail = event.reason ?: stringResource(Res.string.live_gift_detail, event.gift.famePoints),
        )
    }
}

@Composable
private fun Moment(art: @Composable () -> Unit, headline: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        art()
        Spacer(Modifier.height(Pulse.dimens.sm))
        MomentCaption(headline, detail)
    }
}

@Composable
private fun MomentCaption(headline: String, detail: String) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(
        Modifier
            .clip(Pulse.shapes.control)
            .background(colors.glassStrong)
            .border(dimens.hairline, colors.border, Pulse.shapes.control)
            .padding(horizontal = dimens.lg, vertical = dimens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(headline, style = Pulse.type.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(detail, style = Pulse.type.meta, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReactionKind.visual(): Pair<ImageVector, Color> {
    val colors = Pulse.colors
    return when (this) {
        ReactionKind.HEART -> Glyphs.HeartFilled to colors.live
        ReactionKind.FIRE -> Glyphs.Flame to colors.coral
        ReactionKind.LAUGH -> Glyphs.Smile to colors.gold
        ReactionKind.WOW -> Glyphs.StarFilled to colors.cyan
        ReactionKind.CLAP -> Glyphs.Sparkles to colors.violet
    }
}

/**
 * Reaction bursts drift up the trailing edge. With reduced motion they become a small static counter instead.
 * Each particle runs one finite animation and leaves with its reaction, so nothing animates forever or offscreen.
 */
@Composable
fun LiveReactions(reactions: List<AudienceEvent.Reaction>, modifier: Modifier = Modifier) {
    val dimens = Pulse.dimens
    if (Pulse.reducedMotion) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(dimens.xs), horizontalAlignment = Alignment.End) {
            reactions.takeLast(2).forEach { reaction ->
                key(reaction.id) {
                    val (glyph, tint) = reaction.kind.visual()
                    val description = stringResource(Res.string.live_reactions_count, reaction.count)
                    Row(
                        Modifier
                            .clip(Pulse.shapes.pill)
                            .background(Pulse.colors.glass)
                            .padding(horizontal = dimens.sm, vertical = dimens.xs)
                            .semantics { contentDescription = description },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PulseIcon(glyph, tint, size = dimens.iconTiny)
                        Spacer(Modifier.width(dimens.xs))
                        Text("×${reaction.count}", style = Pulse.type.meta, color = Pulse.colors.textPrimary)
                    }
                }
            }
        }
        return
    }
    BoxWithConstraints(modifier) {
        val height = maxHeight
        reactions.forEach { reaction ->
            key(reaction.id) {
                repeat(reaction.count.coerceIn(MIN_PARTICLES, MAX_PARTICLES)) { index -> ReactionParticle(reaction.kind, index, reaction.id, height) }
            }
        }
    }
}

@Composable
private fun ReactionParticle(kind: ReactionKind, index: Int, seed: Long, travel: Dp) {
    val dimens = Pulse.dimens
    val progress = remember { Animatable(0f) }
    val phase = remember { ((seed * 31 + index * 17) % 100) / 100f }
    LaunchedEffect(Unit) {
        delay(index * Motion.PARTICLE_STAGGER.toLong())
        progress.animateTo(1f, tween(Motion.PARTICLE, easing = LinearEasing))
    }
    val (glyph, tint) = kind.visual()
    val sway = dimens.reactionSway
    val drift = dimens.reactionDrift
    Box(
        Modifier
            .fillMaxSize()
            // All motion is read in the layer block: a running particle never recomposes.
            .graphicsLayer {
                val p = progress.value
                translationY = -p * travel.toPx() * 0.9f
                translationX = sin((p * 2f + phase) * PI.toFloat()) * sway.toPx() - phase * drift.toPx()
                alpha = if (p == 0f) 0f else (1f - p) * 1.4f
                val scale = 0.6f + p * 0.6f
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        PulseIcon(glyph, tint, size = dimens.iconMedium)
    }
}

/** The 3-2-1 before the room opens. The signature beat between the studio and the live scene. */
@Composable
fun CountdownOverlay(value: Int, style: LiveStyle) {
    val colors = Pulse.colors
    val reduce = Pulse.reducedMotion
    val numeral = Pulse.type.display.let { it.copy(fontSize = it.fontSize * COUNTDOWN_SCALE, lineHeight = it.lineHeight * COUNTDOWN_SCALE) }
    Box(Modifier.fillMaxSize().background(colors.glassStrong), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                if (reduce) fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())
                else (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 1.8f)) togetherWith (fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.6f))
            },
            label = "countdown",
        ) { n ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    n.toString(),
                    style = numeral,
                    color = colors.textPrimary,
                    modifier = Modifier.drawBehind {
                        drawCircle(Brush.radialGradient(listOf(style.accent.copy(alpha = 0.5f), Color.Transparent)), size.maxDimension, Offset(size.width / 2, size.height / 2))
                    },
                )
                Text(stringResource(Res.string.live_opening), style = Pulse.type.label, color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, glyph: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val dimens = Pulse.dimens
    Row(
        modifier
            .clip(Pulse.shapes.pill)
            .background(Pulse.colors.glass)
            .border(dimens.hairline, Pulse.colors.glassBorder, Pulse.shapes.pill)
            .padding(horizontal = dimens.sm, vertical = dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(glyph, tint, size = dimens.iconTiny)
        Spacer(Modifier.width(dimens.xs))
        Text(text, style = Pulse.type.meta, color = Pulse.colors.textPrimary, maxLines = 1)
    }
}

@Composable
fun TranscriptCaption(text: String, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.live_heard, text)
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.textSecondary,
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.control)
            .background(Pulse.colors.glassFaint)
            .padding(horizontal = Pulse.dimens.md, vertical = Pulse.dimens.xs)
            .semantics { contentDescription = description },
    )
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ComponentPreviews
@Composable
private fun CommentStreamPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter)) {
        LiveCommentStream(SampleData.comments)
        Spacer(Modifier.height(Pulse.dimens.sm))
        TranscriptCaption(SampleData.liveState.livePartial.orEmpty())
        Spacer(Modifier.height(Pulse.dimens.sm))
        StatusChip(stringResource(Res.string.live_vip), Glyphs.Sparkles, Pulse.colors.cyan)
    }
}

@ComponentPreviews
@Composable
private fun SpotlightPreview() = PreviewContainer {
    LiveSpotlight(SampleData.gift, LiveStyles.Aurora, Modifier.padding(Pulse.dimens.gutter))
}

@ComponentPreviews
@Composable
private fun CountdownPreview() = PreviewContainer {
    Box(Modifier.width(Pulse.dimens.contentMaxWidth).height(Pulse.dimens.contentMaxWidth)) { CountdownOverlay(3, LiveStyles.Noir) }
}
