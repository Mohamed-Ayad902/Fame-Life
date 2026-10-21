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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_posts_count
import famelife.shared.generated.resources.common_simulated_viewers
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.home_featured_live
import famelife.shared.generated.resources.home_story_you
import famelife.shared.generated.resources.home_top_room
import famelife.shared.generated.resources.home_view_creator
import famelife.shared.generated.resources.post_likes
import famelife.shared.generated.resources.room_description
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.GlossDisc
import org.mayad.fame_life.core.designsystem.components.GlossyPillButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.LiveRing
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
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
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.content.Topic

/** Hero art is drawn slightly larger than its frame so parallax never reveals an edge. */
private const val HERO_ART_SCALE = 1.12f
private const val HERO_ASPECT = 0.82f
private const val ROOM_CARD_ASPECT = 0.66f
/** Every fourth post lays its caption over the art, so the grid mixes poster tiles with card tiles. */
private const val POSTER_EVERY = 4

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

/** Fade from clear to the media scrim over the bottom [from]..1 of the art. */
@Composable
private fun scrimBrush(from: Float = 0.4f): Brush {
    val scrim = Pulse.colors.mediaScrim
    return remember(scrim, from) { Brush.verticalGradient(from to Color.Transparent, 1f to scrim) }
}

/** A light top fade so badges stay readable on bright art. */
@Composable
private fun topScrim(): Brush {
    val scrim = Pulse.colors.mediaScrimSoft
    return remember(scrim) { Brush.verticalGradient(0f to scrim, 0.3f to Color.Transparent) }
}

/** Glass viewer counter over artwork: a cyan eye disc and a bold compact number. Always announced as simulated. */
@Composable
fun ViewerCountPill(count: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.common_simulated_viewers, compactCount(count))
    Row(
        modifier
            .clip(Pulse.shapes.pill)
            .background(colors.glassStrong)
            .border(dimens.hairline, colors.glassBorder, Pulse.shapes.pill)
            .padding(start = dimens.xxs, end = dimens.sm, top = dimens.xxs, bottom = dimens.xxs)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossDisc(Gloss.Cyan, Glyphs.Eye, size = dimens.iconMedium)
        Spacer(Modifier.width(dimens.xs))
        Text(compactCount(count), style = Pulse.type.label, color = colors.onMedia, maxLines = 1)
    }
}

/**
 * Story rail. It opens with the creator's own "go live" portrait, then live creators inside a spinning live ring
 * with their simulated viewer count, then creators with fresh posts.
 */
@Composable
fun CreatorStoryRail(
    onCreator: (Creator) -> Unit,
    modifier: Modifier = Modifier,
    cascade: CascadeState = rememberCascadeState(),
    self: ArtSpec? = null,
    onGoLive: (() -> Unit)? = null,
) {
    val dimens = Pulse.dimens
    val (ordered, viewers) = remember {
        val live = SeedContent.liveRooms.associate { it.creatorId to it.viewers }
        SeedContent.creators.sortedByDescending { it.id in live } to live
    }
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
        if (self != null && onGoLive != null) {
            item(key = "self") { SelfStory(self, onGoLive, Modifier.cascadeIn(cascade, "story-self")) }
        }
        items(ordered, key = { it.id }) { creator ->
            val count = viewers[creator.id]
            Column(
                Modifier.width(dimens.storyItem).cascadeIn(cascade, "story-${creator.id}").pressable { onCreator(creator) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(dimens.storyAvatar), contentAlignment = Alignment.Center) {
                    if (count != null) {
                        LiveRing(Modifier.fillMaxSize()) { CreatorAvatar(creator.avatar, dimens.storyAvatar - dimens.sm) }
                        LiveBadge(Modifier.align(Alignment.BottomCenter).offset(y = dimens.xs))
                    } else {
                        CreatorAvatar(creator.avatar, dimens.storyAvatar, ring = AvatarRing.STORY)
                    }
                }
                Spacer(Modifier.height(dimens.sm))
                Text(
                    creator.displayName.substringBefore(' '),
                    style = Pulse.type.meta,
                    color = Pulse.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseIcon(Glyphs.Eye, Pulse.colors.cyan, size = dimens.iconTiny)
                        Spacer(Modifier.width(dimens.xxs))
                        Text(compactCount(count), style = Pulse.type.overline, color = Pulse.colors.cyan, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfStory(self: ArtSpec, onGoLive: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val label = stringResource(Res.string.go_live)
    Column(
        modifier.width(dimens.storyItem).pressable(onClick = onGoLive).semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(dimens.storyAvatar)) {
            CreatorAvatar(self, dimens.storyAvatar, ring = AvatarRing.SELF)
            GlossDisc(Gloss.Blue, Glyphs.Plus, Modifier.align(Alignment.BottomEnd), size = dimens.iconLarge)
        }
        Spacer(Modifier.height(dimens.sm))
        Text(stringResource(Res.string.home_story_you), style = Pulse.type.meta, color = colors.textPrimary, maxLines = 1)
        Text(label, style = Pulse.type.overline, color = colors.primaryLight, maxLines = 1)
    }
}

/**
 * The centerpiece: the most-watched simulated live as a lit event poster. Full-bleed art with parallax, a soft
 * category glow behind the frame, a rim light, LIVE and viewer badges, a rank ribbon and a glossy action. [parallax]
 * is read inside the art's layer, so scrolling never recomposes the card.
 */
@Composable
fun FeaturedLiveHero(room: LiveRoom, parallax: () -> Float, onOpen: () -> Unit, modifier: Modifier = Modifier, aspect: Float = HERO_ASPECT) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val glow = room.category.gloss().glow
    val description = stringResource(Res.string.room_description, creator.displayName, room.title)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), Offset(size.width / 2, size.height * 0.85f), size.width * 0.8f),
                    size.width * 0.8f,
                    Offset(size.width / 2, size.height * 0.85f),
                )
            }
            .pressable(pressedScale = 0.98f, onClick = onOpen)
            .clip(Pulse.shapes.hero)
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
        Box(Modifier.fillMaxSize().background(topScrim()))
        Box(Modifier.fillMaxSize().background(scrimBrush(0.35f)))
        Box(Modifier.fillMaxSize().border(dimens.hairline, colors.hudRimBrush, Pulse.shapes.hero))
        Row(Modifier.fillMaxWidth().padding(dimens.lg), verticalAlignment = Alignment.CenterVertically) {
            LiveBadge()
            Spacer(Modifier.width(dimens.sm))
            ViewerCountPill(room.viewers)
            Spacer(Modifier.weight(1f))
            RankRibbon(stringResource(Res.string.home_top_room))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(dimens.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlossDisc(room.category.gloss(), room.category.glyph(), size = dimens.iconMedium)
                Spacer(Modifier.width(dimens.sm))
                Text(stringResource(Res.string.home_featured_live), style = Pulse.type.overline, color = colors.cyan)
            }
            Spacer(Modifier.height(dimens.sm))
            Text(room.title, style = Pulse.type.headline, color = colors.onMedia, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(dimens.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(creator.avatar, dimens.avatarSm, ring = AvatarRing.LIVE)
                Spacer(Modifier.width(dimens.sm))
                Column(Modifier.weight(1f)) {
                    Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.onMedia, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${creator.handle} · ${creator.category.label()}", style = Pulse.type.meta, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(dimens.sm))
                GlossyPillButton(stringResource(Res.string.home_view_creator), onOpen, gloss = Gloss.Rose, leading = Glyphs.Play, ink = colors.onMedia)
            }
        }
    }
}

/** Gold rank tag, for facts the feed can stand behind (the featured room is the most-watched simulated room). */
@Composable
private fun RankRibbon(text: String, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .clip(Pulse.shapes.pill)
            .background(colors.glassStrong)
            .border(dimens.hairline, colors.gold.copy(alpha = 0.6f), Pulse.shapes.pill)
            .padding(start = dimens.xxs, end = dimens.sm, top = dimens.xxs, bottom = dimens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossDisc(Gloss.Gold, Glyphs.Crown, size = dimens.iconMedium)
        Spacer(Modifier.width(dimens.xs))
        Text(text, style = Pulse.type.overline, color = colors.goldLight, maxLines = 1)
    }
}

/** Portrait card for a simulated live room: art, rim light, live and viewer badges up top, identity at the foot. */
@Composable
fun LiveRoomCard(room: LiveRoom, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(room.creatorId) ?: return
    val colors = Pulse.colors
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
        Box(Modifier.fillMaxSize().background(topScrim()))
        Box(Modifier.fillMaxSize().background(scrimBrush()))
        Box(Modifier.fillMaxSize().border(dimens.hairline, colors.hudRimBrush, Pulse.shapes.card))
        Column(Modifier.padding(dimens.sm), verticalArrangement = Arrangement.spacedBy(dimens.xs)) {
            LiveBadge()
            ViewerCountPill(room.viewers)
        }
        GlossDisc(room.category.gloss(), room.category.glyph(), Modifier.align(Alignment.TopEnd).padding(dimens.sm), size = dimens.iconLarge)
        Column(Modifier.align(Alignment.BottomStart).padding(dimens.md)) {
            Text(room.title, style = Pulse.type.titleSmall, color = colors.onMedia, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(dimens.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(creator.avatar, dimens.avatarXs)
                Spacer(Modifier.width(dimens.xs))
                Text("@${creator.handle}", style = Pulse.type.meta, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Trending topic as a game-category card: rank numeral, glossy category tile and a strip of the category's light
 * along the top edge.
 */
@Composable
fun TopicCard(topic: Topic, rank: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val gloss = topic.category.gloss()
    val shape = Pulse.shapes.control
    Row(
        modifier
            .width(dimens.topicCard)
            .clip(shape)
            .background(colors.hudPanelBrush)
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(gloss.glow, gloss.glow.copy(alpha = 0f))), size = size.copy(height = dimens.xxs.toPx()))
                drawCircle(Brush.radialGradient(listOf(gloss.glow.copy(alpha = 0.22f), Color.Transparent), Offset.Zero, size.height), size.height, Offset.Zero)
            }
            .border(dimens.hairline, colors.hudRimBrush, shape)
            .padding(horizontal = dimens.md, vertical = dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rank.toString().padStart(2, '0'), style = Pulse.type.metricSmall, color = colors.gold, maxLines = 1)
        Spacer(Modifier.width(dimens.sm))
        GlossyIcon(topic.category.glyph(), gloss, size = dimens.tileXs)
        Spacer(Modifier.width(dimens.sm))
        Column(Modifier.weight(1f)) {
            Text("#${topic.tag}", style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.common_posts_count, compactCount(topic.posts)), style = Pulse.type.meta, color = colors.textMuted, maxLines = 1)
        }
    }
}

/**
 * Masonry post tile. Two treatments alternate so the grid reads like a magazine: poster tiles carry the caption over
 * the art, card tiles put it below. Shapes vary too (every third tile uses the editorial corner).
 */
@Composable
fun PostTile(post: Post, index: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val creator = SeedContent.creator(post.creatorId) ?: return
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val shape = if (index % 3 == 1) Pulse.shapes.editorial else Pulse.shapes.card
    val poster = index % POSTER_EVERY == 0
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
            if (poster) Box(Modifier.fillMaxSize().background(scrimBrush(0.3f)))
            Box(Modifier.fillMaxSize().border(dimens.hairline, colors.hudRimBrush, shape))
            GlossDisc(post.category.gloss(), post.category.glyph(), Modifier.align(Alignment.TopEnd).padding(dimens.sm), size = dimens.iconMedium)
            Column(Modifier.align(Alignment.BottomStart).padding(dimens.sm)) {
                if (poster) {
                    Text(post.caption, style = Pulse.type.titleSmall, color = colors.onMedia, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(dimens.sm))
                }
                Row(
                    Modifier
                        .clip(Pulse.shapes.pill)
                        .background(colors.glassStrong)
                        .border(dimens.hairline, colors.glassBorder, Pulse.shapes.pill)
                        .padding(horizontal = dimens.sm, vertical = dimens.xxs)
                        .semantics(mergeDescendants = true) { contentDescription = likes },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PulseIcon(Glyphs.HeartFilled, colors.live, size = dimens.iconTiny)
                    Spacer(Modifier.width(dimens.xs))
                    Text(compactCount(post.likes), style = Pulse.type.meta, color = colors.onMedia)
                    Spacer(Modifier.width(dimens.sm))
                    PulseIcon(Glyphs.Comment, colors.cyan, size = dimens.iconTiny)
                    Spacer(Modifier.width(dimens.xs))
                    Text(compactCount(post.comments), style = Pulse.type.meta, color = colors.onMedia)
                }
            }
        }
        if (!poster) {
            Spacer(Modifier.height(dimens.sm))
            Text(post.caption, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
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
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
            LiveRoomCard(SampleData.room, onOpen = {})
            PostTile(SampleData.post, 0, onOpen = {}, modifier = Modifier.width(Pulse.dimens.railCardWide))
            PostTile(SampleData.post, 1, onOpen = {}, modifier = Modifier.width(Pulse.dimens.railCardWide))
        }
        TopicCard(SeedContent.topics.first(), rank = 1)
    }
}

@ComponentPreviews
@Composable
private fun StoryRailPreview() = PreviewContainer {
    CreatorStoryRail(onCreator = {}, self = ArtSpec(3, ArtMotif.PORTRAIT, 40), onGoLive = {})
}
