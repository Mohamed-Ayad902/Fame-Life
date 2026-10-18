package org.mayad.fame_life.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_posts_count
import famelife.shared.generated.resources.home_daily_challenge
import famelife.shared.generated.resources.home_for_you
import famelife.shared.generated.resources.home_for_you_eyebrow
import famelife.shared.generated.resources.home_live_now
import famelife.shared.generated.resources.home_simulated_rooms
import famelife.shared.generated.resources.home_trending_topics
import famelife.shared.generated.resources.home_welcome_back
import famelife.shared.generated.resources.tab_inbox
import famelife.shared.generated.resources.tab_with_new
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.SectionHeader
import org.mayad.fame_life.core.designsystem.components.bleed
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.headerScroll
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.motion.scrollLinkedHeader
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseAspect
import org.mayad.fame_life.core.designsystem.theme.PulseWidth
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.Challenge
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.content.Topic

private const val PARALLAX = 0.35f

/** Grid position of the hero row (top bar, stories, hero), used to limit parallax to when it's on top. */
private const val HERO_ROW = 2

/** Everything Home shows. Built from the seed content once; only the profile and unread count change. */
@Immutable
data class HomeState(
    val name: String,
    val palette: Int,
    val unread: Int,
    val featured: LiveRoom,
    val otherLives: List<LiveRoom>,
    val challenge: Challenge,
    val topics: List<Topic>,
    val posts: List<Post>,
)

/** Tier 1: reads app state, logs the view, turns taps into navigation. */
@Composable
fun HomeScreen(navigator: Navigator, onOpenInbox: () -> Unit) {
    val container = LocalAppContainer.current
    val profile by container.profiles.profile.collectAsState()
    val unread = container.notifications.notifications.collectAsState().value.count { !it.read }
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.HomeViewed) }
    val seed = remember { homeSeed() }
    HomeContent(
        state = seed.copy(name = profile?.displayName.orEmpty(), palette = profile?.avatarPalette ?: 0, unread = unread),
        onInbox = onOpenInbox,
        onCreator = { navigator.push(Route.CreatorProfile(it)) },
        onPost = { navigator.push(Route.PostDetail(it)) },
        onChallenge = { navigator.push(Route.LiveSetup(it)) },
    )
}

internal fun homeSeed(): HomeState {
    val featured = SeedContent.liveRooms.maxBy { it.viewers }
    return HomeState(
        name = "",
        palette = 0,
        unread = 0,
        featured = featured,
        otherLives = SeedContent.liveRooms.filter { it.id != featured.id },
        challenge = SeedContent.challenges.first(),
        topics = SeedContent.topics,
        posts = SeedContent.posts,
    )
}

/**
 * Tier 2: stateless. The feed is one staggered grid whose column count follows the window width; on wide windows the
 * featured live and the daily challenge sit side by side instead of stacking.
 */
@Composable
fun HomeContent(
    state: HomeState,
    onInbox: () -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
    onChallenge: (String) -> Unit,
) {
    val dimens = Pulse.dimens
    val grid = rememberLazyStaggeredGridState()
    val cascade = rememberCascadeState()
    val columns = when (Pulse.width) {
        PulseWidth.Compact -> 2
        PulseWidth.Medium -> 3
        PulseWidth.Expanded -> 4
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimens.gutter, end = dimens.gutter, bottom = dimens.bottomBarClearance),
        horizontalArrangement = Arrangement.spacedBy(dimens.md),
        verticalItemSpacing = dimens.lg,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "top") {
            Column(Modifier.scrollLinkedHeader { grid.headerScroll() }) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                HomeTopBar(state.name, state.palette, state.unread, onInbox)
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "stories") {
            CreatorStoryRail(onCreator = { onCreator(it.id) }, modifier = Modifier.bleed(dimens.gutter), cascade = cascade)
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "hero") {
            HeroBlock(state, grid, cascade, onCreator, onChallenge)
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "lives") {
            Column(Modifier.cascadeIn(cascade, "lives")) {
                SectionHeader(stringResource(Res.string.home_live_now), eyebrow = stringResource(Res.string.home_simulated_rooms))
                Spacer(Modifier.height(dimens.md))
                LazyRow(
                    modifier = Modifier.bleed(dimens.gutter),
                    contentPadding = PaddingValues(horizontal = dimens.gutter),
                    horizontalArrangement = Arrangement.spacedBy(dimens.md),
                ) {
                    items(state.otherLives, key = { it.id }) { room ->
                        LiveRoomCard(room, onOpen = { onCreator(room.creatorId) }, modifier = Modifier.cascadeIn(cascade, "room-${room.id}"))
                    }
                }
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "topics") {
            Column(Modifier.cascadeIn(cascade, "topics")) {
                SectionHeader(stringResource(Res.string.home_trending_topics))
                Spacer(Modifier.height(dimens.md))
                LazyRow(
                    modifier = Modifier.bleed(dimens.gutter),
                    contentPadding = PaddingValues(horizontal = dimens.gutter),
                    horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                ) {
                    items(state.topics, key = { it.tag }) { topic -> TopicChip(topic, Modifier.cascadeIn(cascade, "topic-${topic.tag}")) }
                }
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "foryou") {
            SectionHeader(
                stringResource(Res.string.home_for_you),
                eyebrow = stringResource(Res.string.home_for_you_eyebrow),
                modifier = Modifier.cascadeIn(cascade, "foryou"),
            )
        }
        itemsIndexed(state.posts, key = { _, post -> post.id }) { index, post ->
            PostTile(post, index, onOpen = { onPost(post.id) }, modifier = Modifier.cascadeIn(cascade, post.id))
        }
    }
}

/** Tier 3: the hero row. Stacked on phones, side by side when the window is wide. */
@Composable
private fun HeroBlock(
    state: HomeState,
    grid: LazyStaggeredGridState,
    cascade: CascadeState,
    onCreator: (String) -> Unit,
    onChallenge: (String) -> Unit,
) {
    val dimens = Pulse.dimens
    // Parallax only while the hero row is the first visible item; read inside the art's layer, never in composition.
    val parallax = remember(grid) { { if (grid.firstVisibleItemIndex == HERO_ROW) grid.firstVisibleItemScrollOffset * PARALLAX else 0f } }
    val openFeatured = { onCreator(state.featured.creatorId) }
    val startChallenge = { onChallenge(state.challenge.title) }
    if (Pulse.width.split) {
        Row(Modifier.cascadeIn(cascade, "hero"), horizontalArrangement = Arrangement.spacedBy(dimens.lg), verticalAlignment = Alignment.CenterVertically) {
            FeaturedLiveHero(state.featured, parallax, openFeatured, Modifier.weight(1.4f), aspect = PulseAspect.Wide)
            ChallengeCard(state.challenge.title, state.challenge.description, startChallenge, Modifier.weight(1f))
        }
    } else {
        Column(Modifier.cascadeIn(cascade, "hero"), verticalArrangement = Arrangement.spacedBy(dimens.lg)) {
            FeaturedLiveHero(state.featured, parallax, openFeatured)
            ChallengeCard(state.challenge.title, state.challenge.description, startChallenge)
        }
    }
}

@Composable
private fun HomeTopBar(name: String, palette: Int, unread: Int, onInbox: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val inbox = stringResource(Res.string.tab_inbox)
    Row(Modifier.fillMaxWidth().padding(vertical = dimens.md), verticalAlignment = Alignment.CenterVertically) {
        CreatorAvatar(ArtSpec(palette, ArtMotif.PORTRAIT, palette * 13 + 1), dimens.avatarMd, ring = AvatarRing.SELF)
        Spacer(Modifier.width(dimens.md))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.home_welcome_back), style = Pulse.type.meta, color = colors.textMuted)
            Text(name, style = Pulse.type.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassIconButton(
            Glyphs.Bell,
            contentDescription = if (unread > 0) stringResource(Res.string.tab_with_new, inbox, unread) else inbox,
            onClick = onInbox,
            tint = if (unread > 0) colors.cyan else colors.textPrimary,
        )
    }
}

@Composable
private fun TopicChip(topic: Topic, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .clip(Pulse.shapes.control)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.control)
            .padding(horizontal = dimens.md, vertical = dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(topic.category.glyph(), topic.category.gloss(), size = dimens.tileXs)
        Spacer(Modifier.width(dimens.sm))
        Column {
            Text("#${topic.tag}", style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1)
            Text(stringResource(Res.string.common_posts_count, compactCount(topic.posts)), style = Pulse.type.meta, color = colors.textMuted)
        }
    }
}

@Composable
private fun ChallengeCard(title: String, body: String, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onStart)
            .clip(Pulse.shapes.panel)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.panel)
            .padding(dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Trophy, Gloss.Gold, size = dimens.tileLg)
        Spacer(Modifier.width(dimens.lg))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.home_daily_challenge), style = Pulse.type.overline, color = colors.gold)
            Spacer(Modifier.height(dimens.xxs))
            Text(title, style = Pulse.type.titleSmall, color = colors.textPrimary)
            Text(body, style = Pulse.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(dimens.sm))
        PulseIcon(Glyphs.ChevronRight, colors.textMuted, size = dimens.icon)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun HomePreview() = PreviewContainer {
    AtmosphereBackground {
        HomeContent(homeSeed().copy(name = "Maya Noor", palette = 3, unread = 2), {}, {}, {}, {})
    }
}
