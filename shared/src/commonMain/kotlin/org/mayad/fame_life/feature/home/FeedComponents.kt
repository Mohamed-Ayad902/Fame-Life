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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.format.hoursAgoLabel
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent

private val scrimBrush = Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE6070A14))

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

/** Story rail: live creators first with a live ring, then creators with fresh posts. */
@Composable
fun CreatorStoryRail(onCreator: (Creator) -> Unit, modifier: Modifier = Modifier) {
    val liveIds = SeedContent.liveRooms.map { it.creatorId }.toSet()
    val ordered = SeedContent.creators.sortedByDescending { it.id in liveIds }
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = Pulse.dimens.gutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(ordered, key = { it.id }) { creator ->
            val live = creator.id in liveIds
            Column(
                Modifier.width(68.dp).pressable { onCreator(creator) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    CreatorAvatar(creator.avatar, 66.dp, ring = if (live) AvatarRing.LIVE else AvatarRing.STORY)
                    if (live) LiveBadge(Modifier.padding(bottom = 0.dp))
                }
                Spacer(Modifier.height(6.dp))
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

/** Large editorial hero for the most-watched simulated live. [parallax] shifts the art for depth while scrolling. */
@Composable
fun FeaturedLiveHero(room: LiveRoom, parallax: Float, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    val colors = Pulse.colors
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .pressable(pressedScale = 0.98f, onClick = onOpen)
            .clip(Pulse.shapes.hero)
            .border(1.dp, colors.border, Pulse.shapes.hero)
            .semantics(mergeDescendants = true) { contentDescription = "${creator.displayName}, simulated live: ${room.title}" },
    ) {
        Artwork(
            creator.cover,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = parallax
                    scaleX = 1.12f
                    scaleY = 1.12f
                },
        )
        Box(Modifier.fillMaxSize().background(scrimBrush))
        Row(Modifier.padding(Pulse.dimens.lg), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            LiveBadge()
            ViewerPill(room.viewers)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(Pulse.dimens.xl)) {
            Text("FEATURED LIVE", style = Pulse.type.overline, color = colors.cyan)
            Spacer(Modifier.height(Pulse.dimens.xs))
            Text(room.title, style = Pulse.type.headline, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Pulse.dimens.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(creator.avatar, 32.dp)
                Spacer(Modifier.width(Pulse.dimens.sm))
                Column(Modifier.weight(1f)) {
                    Text(creator.displayName, style = Pulse.type.titleSmall, color = Color.White, maxLines = 1)
                    Text("@${creator.handle} · ${creator.category.label}", style = Pulse.type.meta, color = colors.textSecondary, maxLines = 1)
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(percent = 50)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { PulseIcon(Glyphs.ChevronRight, colors.background, size = 20.dp) }
            }
        }
    }
}

/** Portrait card for a simulated live room. */
@Composable
fun LiveRoomCard(room: LiveRoom, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    Box(
        modifier
            .width(148.dp)
            .aspectRatio(0.68f)
            .pressable(onClick = onOpen)
            .clip(Pulse.shapes.card)
            .semantics(mergeDescendants = true) { contentDescription = "${creator.displayName}, simulated live: ${room.title}" },
    ) {
        Artwork(room.art, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(scrimBrush))
        Row(Modifier.padding(Pulse.dimens.sm), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LiveBadge()
        }
        Column(Modifier.align(Alignment.BottomStart).padding(Pulse.dimens.md)) {
            ViewerPill(room.viewers)
            Spacer(Modifier.height(6.dp))
            Text(room.title, style = Pulse.type.titleSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("@${creator.handle}", style = Pulse.type.meta, color = Pulse.colors.textSecondary, maxLines = 1)
        }
    }
}

/**
 * Masonry post tile. Corner radius alternates by position so the grid reads as an editorial layout rather than
 * a wall of identical cards.
 */
@Composable
fun PostTile(post: Post, index: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(post.creatorId) ?: return
    val colors = Pulse.colors
    val shape = if (index % 3 == 1) RoundedCornerShape(28.dp, 10.dp, 28.dp, 28.dp) else Pulse.shapes.card
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
                    .padding(Pulse.dimens.sm)
                    .clip(Pulse.shapes.tag)
                    .background(Color(0x8C080B14))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseIcon(Glyphs.HeartFilled, colors.live, size = 12.dp)
                Spacer(Modifier.width(4.dp))
                Text(compactCount(post.likes), style = Pulse.type.meta, color = Color.White)
            }
        }
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(post.caption, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CreatorAvatar(creator.avatar, 18.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                "${creator.handle} · ${hoursAgoLabel(post.hoursAgo)}",
                style = Pulse.type.meta,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
