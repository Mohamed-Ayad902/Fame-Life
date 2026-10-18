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
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.domain.simulation.AudienceEvent
import org.mayad.fame_life.domain.simulation.AudienceMember
import org.mayad.fame_life.domain.simulation.CommentTone
import org.mayad.fame_life.domain.simulation.GiftTier
import org.mayad.fame_life.domain.simulation.ReactionKind
import kotlin.math.PI
import kotlin.math.sin

fun AudienceMember.color(): Color = Color.hsv(hue, 0.5f, 1f)

/**
 * The comment stream. Newest at the bottom, older ones fade towards the top so the stream never becomes a wall
 * over the creator's face. Each comment animates in once, keyed by its id.
 */
@Composable
fun LiveCommentStream(comments: List<AudienceEvent.Comment>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(max = 300.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black), blendMode = BlendMode.DstIn)
            },
        verticalArrangement = Arrangement.spacedBy(Pulse.dimens.xs, Alignment.Bottom),
    ) {
        comments.forEach { comment -> key(comment.id) { CommentBubble(comment) } }
    }
}

@Composable
private fun CommentBubble(comment: AudienceEvent.Comment) {
    val colors = Pulse.colors
    val reduce = Pulse.reducedMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, Motion.enter()) }
    val critical = comment.tone == CommentTone.CRITICAL
    Row(
        Modifier
            .widthIn(max = 300.dp)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 24.dp.toPx()
            }
            .clip(Pulse.shapes.bubble)
            .background(if (critical) Color(0x553A1420) else Color(0x5C0B0F1C))
            .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = Pulse.dimens.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(Brush.linearGradient(listOf(comment.author.color(), comment.author.color().copy(alpha = 0.5f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(comment.author.handle.take(1).uppercase(), style = Pulse.type.meta, color = Color(0xFF080B14))
        }
        Spacer(Modifier.width(Pulse.dimens.sm))
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
                art = { GlossyIcon(Glyphs.Eye, Gloss.Cyan, size = 64.dp, glyphSize = 30.dp) },
                headline = "${compactCount(current.viewers)} watching",
                detail = "A new high for this live (simulated)",
            )
            is AudienceEvent.VipJoined -> Moment(
                art = { GlossyIcon(Glyphs.Crown, Gloss.Gold, size = 64.dp, glyphSize = 30.dp) },
                headline = "${current.member.handle} joined",
                detail = "A VIP regular is here",
            )
            is AudienceEvent.TrendingMoment -> Moment(
                art = { GlossyIcon(Glyphs.Flame, Gloss.Coral, size = 64.dp, glyphSize = 30.dp) },
                headline = "You're trending",
                detail = current.reason,
            )
            else -> Box(Modifier)
        }
    }
}

@Composable
private fun GiftMoment(event: AudienceEvent.GiftSent, style: LiveStyle) {
    val reduce = Pulse.reducedMotion
    val size = when (event.gift.tier) {
        GiftTier.SMALL -> 72.dp
        GiftTier.MEDIUM -> 104.dp
        GiftTier.MAJOR -> 148.dp
    }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduce && event.gift.tier != GiftTier.SMALL) burst.animateTo(1f, tween(1100, easing = Motion.decelerate)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size * 1.8f)
                .drawBehind {
                    // A light burst behind bigger gifts; rare gifts get the strongest moment.
                    val radius = this.size.minDimension / 2 * (0.5f + burst.value * 0.5f)
                    drawCircle(
                        Brush.radialGradient(listOf(style.glow.copy(alpha = 0.45f * (1f - burst.value * 0.6f)), Color.Transparent), center, radius),
                        radius,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            GiftArt(event.gift.id, size = size, name = event.gift.name)
        }
        MomentCaption(
            headline = "${event.from.handle} sent ${event.gift.name}",
            detail = event.reason ?: "Simulated gift · +${event.gift.famePoints} fame",
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
    Column(
        Modifier
            .clip(Pulse.shapes.control)
            .background(Color(0x99080B14))
            .border(1.dp, colors.border, Pulse.shapes.control)
            .padding(horizontal = Pulse.dimens.lg, vertical = Pulse.dimens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(headline, style = Pulse.type.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(detail, style = Pulse.type.meta, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

private fun ReactionKind.visual(): Pair<ImageVector, Color> = when (this) {
    ReactionKind.HEART -> Glyphs.HeartFilled to Color(0xFFFF4D6D)
    ReactionKind.FIRE -> Glyphs.Flame to Color(0xFFFF8A5B)
    ReactionKind.LAUGH -> Glyphs.Smile to Color(0xFFFFC66D)
    ReactionKind.WOW -> Glyphs.StarFilled to Color(0xFF55E5F2)
    ReactionKind.CLAP -> Glyphs.Sparkles to Color(0xFF945CFF)
}

/**
 * Reaction bursts drift up the right edge. With reduced motion they become a small static counter instead.
 * Particle count is capped so a busy room stays cheap to draw.
 */
@Composable
fun LiveReactions(reactions: List<AudienceEvent.Reaction>, modifier: Modifier = Modifier) {
    val reduce = Pulse.reducedMotion
    if (reduce) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(Pulse.dimens.xs), horizontalAlignment = Alignment.End) {
            reactions.takeLast(2).forEach { reaction ->
                key(reaction.id) {
                    val (glyph, tint) = reaction.kind.visual()
                    Row(
                        Modifier.clip(Pulse.shapes.pill).background(Color(0x66080B14)).padding(horizontal = Pulse.dimens.sm, vertical = 4.dp)
                            .semantics { contentDescription = "${reaction.count} reactions" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PulseIcon(glyph, tint, size = 14.dp)
                        Spacer(Modifier.width(4.dp))
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
                val particles = reaction.count.coerceIn(3, 7)
                repeat(particles) { index -> ReactionParticle(reaction.kind, index, reaction.id, height) }
            }
        }
    }
}

@Composable
private fun ReactionParticle(kind: ReactionKind, index: Int, seed: Long, travel: androidx.compose.ui.unit.Dp) {
    val progress = remember { Animatable(0f) }
    val phase = remember { ((seed * 31 + index * 17) % 100) / 100f }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 140L)
        progress.animateTo(1f, tween(2000, easing = LinearEasing))
    }
    val (glyph, tint) = kind.visual()
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                translationY = -p * travel.toPx() * 0.9f
                translationX = sin((p * 2f + phase) * PI.toFloat()) * 16.dp.toPx() - phase * 28.dp.toPx()
                alpha = if (p == 0f) 0f else (1f - p) * 1.4f
                val scale = 0.6f + p * 0.6f
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        PulseIcon(glyph, tint, size = 26.dp)
    }
}

/** The 3-2-1 before the room opens. The signature beat between the studio and the live scene. */
@Composable
fun CountdownOverlay(value: Int, style: LiveStyle) {
    val colors = Pulse.colors
    val reduce = Pulse.reducedMotion
    Box(Modifier.fillMaxSize().background(Color(0x99080B14)), contentAlignment = Alignment.Center) {
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
                    style = Pulse.type.display.copy(fontSize = Pulse.type.display.fontSize * 3),
                    color = colors.textPrimary,
                    modifier = Modifier.drawBehind {
                        drawCircle(Brush.radialGradient(listOf(style.accent.copy(alpha = 0.5f), Color.Transparent)), size.maxDimension, Offset(size.width / 2, size.height / 2))
                    },
                )
                Text("Opening the room", style = Pulse.type.label, color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, glyph: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(Pulse.shapes.pill).background(Color(0x66080B14)).border(1.dp, Color(0x22FFFFFF), Pulse.shapes.pill).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(glyph, tint, size = 13.dp)
        Spacer(Modifier.width(5.dp))
        Text(text, style = Pulse.type.meta, color = Pulse.colors.textPrimary, maxLines = 1)
    }
}

@Composable
fun TranscriptCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.textSecondary,
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.control)
            .background(Color(0x55080B14))
            .padding(horizontal = Pulse.dimens.md, vertical = 6.dp)
            .semantics { contentDescription = "What the audience heard: $text" },
    )
}
