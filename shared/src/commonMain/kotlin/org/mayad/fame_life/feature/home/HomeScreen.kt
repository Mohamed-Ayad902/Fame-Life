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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
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
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Shapes
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.feature.shell.BottomBarClearance

private const val HERO_INDEX = 2

@Composable
fun HomeScreen(navigator: Navigator, onOpenInbox: () -> Unit) {
    val container = LocalAppContainer.current
    val profile by container.profiles.profile.collectAsState()
    val unread = container.notifications.notifications.collectAsState().value.count { !it.read }
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.HomeViewed) }

    val grid = rememberLazyStaggeredGridState()
    val heroParallax by remember {
        derivedStateOf { if (grid.firstVisibleItemIndex == HERO_INDEX) grid.firstVisibleItemScrollOffset * 0.35f else 0f }
    }
    val featured = remember { SeedContent.liveRooms.maxBy { it.viewers } }
    val otherLives = remember { SeedContent.liveRooms.filter { it.id != featured.id } }
    val challenge = remember { SeedContent.challenges.first() }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.screen, end = Space.screen, bottom = BottomBarClearance.calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalItemSpacing = Space.lg,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "top") {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                HomeTopBar(
                    name = profile?.displayName.orEmpty(),
                    palette = profile?.avatarPalette ?: 0,
                    unread = unread,
                    onInbox = onOpenInbox,
                )
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "stories") {
            CreatorStoryRail(
                onCreator = { navigator.push(Route.CreatorProfile(it.id)) },
                modifier = Modifier.bleed(Space.screen),
            )
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "hero") {
            FeaturedLiveHero(featured, heroParallax, onOpen = { navigator.push(Route.CreatorProfile(featured.creatorId)) })
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "challenge") {
            ChallengeCard(
                title = challenge.title,
                body = challenge.description,
                onStart = { navigator.push(Route.LiveSetup(challenge.title)) },
            )
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "lives") {
            Column {
                SectionHeader("Live now", eyebrow = "Simulated rooms")
                Spacer(Modifier.height(Space.md))
                LazyRow(
                    modifier = Modifier.bleed(Space.screen),
                    contentPadding = PaddingValues(horizontal = Space.screen),
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    items(otherLives, key = { it.id }) { room ->
                        LiveRoomCard(room, onOpen = { navigator.push(Route.CreatorProfile(room.creatorId)) })
                    }
                }
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "topics") {
            Column {
                SectionHeader("Trending topics")
                Spacer(Modifier.height(Space.md))
                LazyRow(
                    modifier = Modifier.bleed(Space.screen),
                    contentPadding = PaddingValues(horizontal = Space.screen),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    items(SeedContent.topics, key = { it.tag }) { topic ->
                        Row(
                            Modifier
                                .clip(Shapes.control)
                                .background(Pulse.colors.surface)
                                .border(1.dp, Pulse.colors.border, Shapes.control)
                                .padding(horizontal = Space.md, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlossyIcon(topic.category.glyph(), topic.category.gloss(), size = 30.dp)
                            Spacer(Modifier.width(Space.sm))
                            Column {
                                Text("#${topic.tag}", style = Pulse.type.titleSmall, color = Pulse.colors.textPrimary, maxLines = 1)
                                Text("${compactCount(topic.posts)} posts", style = Pulse.type.meta, color = Pulse.colors.textMuted)
                            }
                        }
                    }
                }
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "foryou") {
            SectionHeader("For you", eyebrow = "Picked from your interests")
        }
        itemsIndexed(SeedContent.posts, key = { _, post -> post.id }) { index, post ->
            PostTile(post, index, onOpen = { navigator.push(Route.PostDetail(post.id)) })
        }
    }
}

@Composable
private fun HomeTopBar(name: String, palette: Int, unread: Int, onInbox: () -> Unit) {
    val colors = Pulse.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
        CreatorAvatar(ArtSpec(palette, ArtMotif.PORTRAIT, palette * 13 + 1), 44.dp, ring = AvatarRing.SELF)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text("Welcome back", style = Pulse.type.meta, color = colors.textMuted)
            Text(name, style = Pulse.type.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassIconButton(
            Glyphs.Bell,
            contentDescription = if (unread > 0) "Inbox, $unread new" else "Inbox",
            onClick = onInbox,
            tint = if (unread > 0) colors.cyan else colors.textPrimary,
        )
    }
}

@Composable
private fun ChallengeCard(title: String, body: String, onStart: () -> Unit) {
    val colors = Pulse.colors
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onStart)
            .clip(Shapes.panel)
            .background(colors.surface)
            .border(1.dp, colors.border, Shapes.panel)
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Trophy, Gloss.Gold, size = 56.dp)
        Spacer(Modifier.width(Space.lg))
        Column(Modifier.weight(1f)) {
            Text("DAILY CHALLENGE", style = Pulse.type.overline, color = colors.gold)
            Spacer(Modifier.height(2.dp))
            Text(title, style = Pulse.type.titleSmall, color = colors.textPrimary)
            Text(body, style = Pulse.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.sm))
        PulseIcon(Glyphs.ChevronRight, colors.textMuted, size = 20.dp)
    }
}
