package org.mayad.fame_life.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.badge_first_live
import famelife.shared.generated.resources.badge_first_live_detail
import famelife.shared.generated.resources.badge_locked
import famelife.shared.generated.resources.badge_unlocked
import famelife.shared.generated.resources.level_card_description
import famelife.shared.generated.resources.level_card_description_next
import famelife.shared.generated.resources.level_card_eyebrow
import famelife.shared.generated.resources.level_card_points_to
import famelife.shared.generated.resources.level_card_top
import famelife.shared.generated.resources.setup_avatar_style
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.title
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.progression.LevelProgress

/** The eight avatar looks the signed-in creator can pick from. */
const val AVATAR_STYLES = 8
private const val COVER_PARALLAX = 0.5f
private const val LOCKED_ALPHA = 0.45f
private const val MIN_BAR = 0.02f

fun selfAvatar(palette: Int) = ArtSpec(palette, ArtMotif.PORTRAIT, palette * 13 + 1)
fun selfCover(palette: Int) = ArtSpec(palette, ArtMotif.AURORA, palette * 17 + 5)

/**
 * Cover art with parallax and a bottom fade, with the avatar overlapping its lower edge. [scroll] is read inside the
 * cover's layer, so scrolling the profile never recomposes it.
 */
@Composable
fun ProfileCover(cover: ArtSpec, avatar: ArtSpec, scroll: () -> Float, ring: AvatarRing, overlay: @Composable BoxScope.() -> Unit = {}) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(Modifier.fillMaxWidth().height(dimens.profileHeroHeight)) {
        Artwork(
            cover,
            Modifier.fillMaxWidth().height(dimens.profileCoverHeight).graphicsLayer {
                val offset = scroll().coerceAtMost(size.height)
                translationY = offset * COVER_PARALLAX
                alpha = (1f - offset / size.height).coerceIn(0.3f, 1f)
            },
        )
        Box(Modifier.fillMaxWidth().height(dimens.profileCoverHeight).background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to colors.background)))
        CreatorAvatar(avatar, dimens.avatarXl, Modifier.align(Alignment.BottomStart).padding(start = dimens.gutter), ring = ring)
        overlay()
    }
}

/** Fame level card: glossy crown, title and a bar that fills toward the next level (drawn, not re-laid out). */
@Composable
fun LevelCard(progress: LevelProgress, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val fraction = animateFloatAsState(progress.fraction, Motion.spec(Motion.reveal()), label = "level")
    val level = progress.level.title()
    val next = progress.level.next?.title()
    val description = progress.pointsToNext?.let { stringResource(Res.string.level_card_description_next, level, progress.points, it) }
        ?: stringResource(Res.string.level_card_description, level, progress.points)
    val track = colors.surfaceElevated
    val fill = colors.goldGradient
    Row(
        modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.panel)
            .padding(dimens.lg)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Crown, Gloss.Gold, size = dimens.tileLg)
        Spacer(Modifier.width(dimens.lg))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.level_card_eyebrow), style = Pulse.type.overline, color = colors.gold)
            Text(level, style = Pulse.type.title, color = colors.textPrimary)
            Spacer(Modifier.height(dimens.sm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimens.track)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(track, cornerRadius = radius)
                        // The animated fraction is read here, in the draw phase.
                        drawRoundRect(fill, size = Size(size.width * fraction.value.coerceAtLeast(MIN_BAR), size.height), cornerRadius = radius)
                    },
            )
            Spacer(Modifier.height(dimens.xs))
            Text(
                if (progress.pointsToNext != null && next != null) stringResource(Res.string.level_card_points_to, progress.pointsToNext, next)
                else stringResource(Res.string.level_card_top),
                style = Pulse.type.meta,
                color = colors.textMuted,
            )
        }
    }
}

@Immutable
data class Badge(val title: StringResource, val detail: StringResource, val gloss: Gloss, val glyph: ImageVector, val unlocked: Boolean)

@Composable
fun BadgeTile(badge: Badge, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val title = stringResource(badge.title)
    val detail = stringResource(badge.detail)
    val description = stringResource(if (badge.unlocked) Res.string.badge_unlocked else Res.string.badge_locked, title, detail)
    Column(
        modifier
            .clip(Pulse.shapes.card)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.card)
            .padding(dimens.md)
            .graphicsLayer { alpha = if (badge.unlocked) 1f else LOCKED_ALPHA }
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlossyIcon(if (badge.unlocked) badge.glyph else Glyphs.Lock, if (badge.unlocked) badge.gloss else Gloss.Slate, size = dimens.tileLg)
        Spacer(Modifier.height(dimens.sm))
        Text(title, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1)
        Text(detail, style = Pulse.type.meta, color = colors.textMuted, maxLines = 2)
    }
}

/** The avatar looks. Used by onboarding and profile editing. */
@Composable
fun AvatarPalettePicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(AVATAR_STYLES) { index ->
            val description = stringResource(Res.string.setup_avatar_style, index + 1)
            Box(
                Modifier
                    .size(dimens.tileSm)
                    .pressable(role = Role.RadioButton) { onSelect(index) }
                    .semantics { this.selected = index == selected; contentDescription = description }
                    .clip(CircleShape)
                    .border(dimens.ring, if (index == selected) colors.textPrimary else Color.Transparent, CircleShape),
            ) {
                Artwork(selfAvatar(index), Modifier.fillMaxSize().padding(dimens.xxs).clip(CircleShape))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ComponentPreviews
@Composable
private fun LevelCardPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        LevelCard(FameProgression.progress(SampleData.profile.famePoints))
        LevelCard(FameProgression.progress(1_000_000))
    }
}

@ComponentPreviews
@Composable
private fun BadgesAndPickerPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            BadgeTile(Badge(Res.string.badge_first_live, Res.string.badge_first_live_detail, Gloss.Violet, Glyphs.Mic, true), Modifier.weight(1f))
            BadgeTile(Badge(Res.string.badge_first_live, Res.string.badge_first_live_detail, Gloss.Violet, Glyphs.Mic, false), Modifier.weight(1f))
        }
        AvatarPalettePicker(3, {})
    }
}

@ComponentPreviews
@Composable
private fun ProfileCoverPreview() = PreviewContainer {
    ProfileCover(selfCover(3), selfAvatar(3), scroll = { 0f }, ring = AvatarRing.SELF)
}
