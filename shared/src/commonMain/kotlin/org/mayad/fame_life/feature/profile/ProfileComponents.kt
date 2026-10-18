package org.mayad.fame_life.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.domain.progression.LevelProgress

/** Cover art with parallax and a bottom fade, with the avatar overlapping its lower edge. */
@Composable
fun ProfileCover(cover: ArtSpec, avatar: ArtSpec, scroll: Float, ring: AvatarRing, overlay: @Composable BoxScope.() -> Unit = {}) {
    val colors = Pulse.colors
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        Artwork(
            cover,
            Modifier.fillMaxWidth().height(240.dp).graphicsLayer {
                translationY = scroll * 0.5f
                alpha = (1f - scroll / 500f).coerceIn(0.3f, 1f)
            },
        )
        Box(Modifier.fillMaxWidth().height(240.dp).background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to colors.background)))
        CreatorAvatar(
            avatar,
            112.dp,
            Modifier.align(Alignment.BottomStart).padding(start = Pulse.dimens.gutter).offset(y = (-4).dp),
            ring = ring,
        )
        overlay()
    }
}

/** Fame level card: glossy crown, title and an animated bar toward the next level. */
@Composable
fun LevelCard(progress: LevelProgress, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val fraction by animateFloatAsState(progress.fraction, Motion.enter(), label = "level")
    Row(
        modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface)
            .border(1.dp, colors.border, Pulse.shapes.panel)
            .padding(Pulse.dimens.lg)
            .semantics(mergeDescendants = true) {
                contentDescription = "Fame level ${progress.level.title}, ${progress.points} points" +
                    (progress.pointsToNext?.let { ", $it to next level" } ?: "")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Crown, Gloss.Gold, size = 52.dp)
        Spacer(Modifier.width(Pulse.dimens.lg))
        Column(Modifier.weight(1f)) {
            Text("FAME LEVEL", style = Pulse.type.overline, color = colors.gold)
            Text(progress.level.title, style = Pulse.type.title, color = colors.textPrimary)
            Spacer(Modifier.height(Pulse.dimens.sm))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(Pulse.shapes.pill).background(colors.surfaceElevated)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.02f)).clip(Pulse.shapes.pill).background(colors.goldGradient))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                progress.pointsToNext?.let { "$it points to ${progress.level.next?.title}" } ?: "Top level reached",
                style = Pulse.type.meta,
                color = colors.textMuted,
            )
        }
    }
}

data class Badge(val title: String, val detail: String, val gloss: Gloss, val glyph: androidx.compose.ui.graphics.vector.ImageVector, val unlocked: Boolean)

@Composable
fun BadgeTile(badge: Badge, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Column(
        modifier
            .clip(Pulse.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, Pulse.shapes.card)
            .padding(Pulse.dimens.md)
            .graphicsLayer { alpha = if (badge.unlocked) 1f else 0.45f }
            .semantics(mergeDescendants = true) {
                contentDescription = "${badge.title}, ${if (badge.unlocked) "unlocked" else "locked"}. ${badge.detail}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlossyIcon(if (badge.unlocked) badge.glyph else Glyphs.Lock, if (badge.unlocked) badge.gloss else Gloss.Slate, size = 52.dp)
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(badge.title, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1)
        Text(badge.detail, style = Pulse.type.meta, color = colors.textMuted, maxLines = 2)
    }
}

/** The eight avatar looks. Used by onboarding and profile editing. */
@Composable
fun AvatarPalettePicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(8) { index ->
            Box(
                Modifier
                    .size(36.dp)
                    .pressable(role = Role.RadioButton) { onSelect(index) }
                    .semantics { this.selected = index == selected; contentDescription = "Avatar style ${index + 1}" }
                    .clip(CircleShape)
                    .border(2.dp, if (index == selected) colors.textPrimary else Color.Transparent, CircleShape),
            ) {
                Artwork(selfAvatar(index), Modifier.fillMaxSize().padding(3.dp).clip(CircleShape))
            }
        }
    }
}
