package org.mayad.fame_life.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.home_featured_live
import famelife.shared.generated.resources.post_likes
import famelife.shared.generated.resources.room_description
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.timeAgoText
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent

/** Hero art is drawn slightly larger than its frame so parallax never reveals an edge. */
private const val HERO_ART_SCALE = 1.12f
private const val HERO_ASPECT = 0.86f
private const val ROOM_CARD_ASPECT = 0.68f

fun Category.glyph() = when (this) {
    Category.GAMING -> Glyphs.Gamepad
    Category.MUSIC -> Glyphs.Music
    Category.CHATTING -> Glyphs.Comment
    Category.STORYTELLING -> Glyphs.Book
    Category.LIFESTYLE -> Glyphs.Coffee
    Category.COMEDY -> Glyphs.Smile
    Category.ENTERTAINMENT -> Glyphs.Film
}

fun Category.gloss() = when (this) {
    Category.GAMING -> Gloss.Blue
    Category.MUSIC -> Gloss.Violet
    Category.CHATTING -> Gloss.Cyan
    Category.STORYTELLING -> Gloss.Gold
    Category.LIFESTYLE -> Gloss.Mint
    Category.COMEDY -> Gloss.Coral
    Category.ENTERTAINMENT -> Gloss.Rose
}

@Composable
private fun scrimBrush(): Brush {
    val scrim = Pulse.colors.mediaScrim
    return remember(scrim) { Brush.verticalGradient(0.45f to Color.Transparent, 1f to scrim) }
}

/** Story rail: live creators first with a live ring, then creators with fresh posts. */
@Composable
fun CreatorStoryRail(onCreator: (Creator) -> Unit, modifier: Modifier = Modifier, cascade: CascadeState = rememberCascadeState()) {
    val dimens = Pulse.dimens
    val (ordered, liveIds) = remember {
        val live = SeedContent.liveRooms.map { it.creatorId }.toSet()
        SeedContent.creators.sortedByDescending { it.id in live } to live
    }
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
        items(ordered, key = { it.id }) { creator ->
            val live = creator.id in liveIds
            Column(
                Modifier.width(dimens.storyItem).cascadeIn(cascade, "story-${creator.id}").pressable { onCreator(creator) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    CreatorAvatar(creator.avatar, dimens.storyAvatar, ring = if (live) AvatarRing.LIVE else AvatarRing.STORY)
                    if (live) LiveBadge()
                }
                Spacer(Modifier.height(dimens.xs))
                Text(
                    creator.displayName.substringBefore(' '),
                    style = Pulse.type.meta,
                    color = Pulse.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Large editorial hero for the most-watched simulated live. [parallax] is read inside the art's layer, so scrolling
 * shifts it without recomposing the card.
 */
@Composable
fun FeaturedLiveHero(room: LiveRoom, parallax: () -> Float, onOpen: () -> Unit, modifier: Modifier = Modifier, aspect: Float = HERO_ASPECT) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.room_description, creator.displayName, room.title)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .pressable(pressedScale = 0.98f, onClick = onOpen)
            .clip(Pulse.shapes.hero)
            .border(dimens.hairline, colors.border, Pulse.shapes.hero)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Artwork(
            creator.cover,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = parallax()
                    scaleX = HERO_ART_SCALE
                    scaleY = HERO_ART_SCALE
                },
        )
        Box(Modifier.fillMaxSize().background(scrimBrush()))
        Row(Modifier.padding(dimens.lg), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
            LiveBadge()
            ViewerPill(room.viewers)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(dimens.xl)) {
            Text(stringResource(Res.string.home_featured_live), style = Pulse.type.overline, color = colors.cyan)
            Spacer(Modifier.height(dimens.xs))
            Text(room.title, style = Pulse.type.headline, color = colors.onMedia, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(dimens.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(creator.avatar, dimens.avatarSm)
                Spacer(Modifier.width(dimens.sm))
                Column(Modifier.weight(1f)) {
                    Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.onMedia, maxLines = 1)
                    Text("@${creator.handle} · ${creator.category.label()}", style = Pulse.type.meta, color = colors.textSecondary, maxLines = 1)
                }
                Box(
                    Modifier.size(dimens.glassButton).clip(CircleShape).background(colors.onMedia),
                    contentAlignment = Alignment.Center,
                ) { PulseIcon(Glyphs.ChevronRight, colors.background, size = dimens.icon) }
            }
        }
    }
}

/** Portrait card for a simulated live room. */
@Composable
fun LiveRoomCard(room: LiveRoom, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.room_description, creator.displayName, room.title)
    Box(
        modifier
            .width(dimens.railCard)
            .aspectRatio(ROOM_CARD_ASPECT)
            .pressable(onClick = onOpen)
            .clip(Pulse.shapes.card)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Artwork(room.art, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(scrimBrush()))
        LiveBadge(Modifier.padding(dimens.sm))
        Column(Modifier.align(Alignment.BottomStart).padding(dimens.md)) {
            ViewerPill(room.viewers)
            Spacer(Modifier.height(dimens.xs))
            Text(room.title, style = Pulse.type.titleSmall, color = Pulse.colors.onMedia, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("@${creator.handle}", style = Pulse.type.meta, color = Pulse.colors.textSecondary, maxLines = 1)
        }
    }
}

/**
 * Masonry post tile. Every third tile uses the editorial shape so the grid reads as a magazine layout rather than
 * a wall of identical cards.
 */
@Composable
fun PostTile(post: Post, index: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(post.creatorId) ?: return
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val shape = if (index % 3 == 1) Pulse.shapes.editorial else Pulse.shapes.card
    val likes = stringResource(Res.string.post_likes, compactCount(post.likes))
    Column(modifier.pressable(pressedScale = 0.97f, onClick = onOpen)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / post.aspect)
                .clip(shape)
                .semantics { contentDescription = post.caption },
        ) {
            Artwork(post.art, Modifier.fillMaxSize())
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(dimens.sm)
                    .clip(Pulse.shapes.tag)
                    .background(colors.glassStrong)
                    .padding(horizontal = dimens.sm, vertical = dimens.xxs)
                    .semantics(mergeDescendants = true) { contentDescription = likes },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseIcon(Glyphs.HeartFilled, colors.live, size = dimens.iconTiny)
                Spacer(Modifier.width(dimens.xs))
                Text(compactCount(post.likes), style = Pulse.type.meta, color = colors.onMedia)
            }
        }
        Spacer(Modifier.height(dimens.sm))
        Text(post.caption, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(dimens.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CreatorAvatar(creator.avatar, dimens.avatarXs)
            Spacer(Modifier.width(dimens.xs))
            Text(
                "${creator.handle} · ${timeAgoText(post.hoursAgo)}",
                style = Pulse.type.meta,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ComponentPreviews
@Composable
private fun FeaturedLiveHeroPreview() = PreviewContainer {
    FeaturedLiveHero(SampleData.room, parallax = { 0f }, onOpen = {}, modifier = Modifier.padding(Pulse.dimens.gutter))
}

@ComponentPreviews
@Composable
private fun CardsPreview() = PreviewContainer {
    Row(Modifier.padding(Pulse.dimens.gutter), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        LiveRoomCard(SampleData.room, onOpen = {})
        PostTile(SampleData.post, 0, onOpen = {}, modifier = Modifier.width(Pulse.dimens.railCardWide))
    }
}

@ComponentPreviews
@Composable
private fun StoryRailPreview() = PreviewContainer { CreatorStoryRail(onCreator = {}) }
