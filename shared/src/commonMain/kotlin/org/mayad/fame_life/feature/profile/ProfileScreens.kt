package org.mayad.fame_life.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.StatColumn
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Shapes
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.format.durationLabel
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.domain.progression.FameLevel
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.home.PostTile
import org.mayad.fame_life.feature.shell.BottomBarClearance

fun selfAvatar(palette: Int) = ArtSpec(palette, ArtMotif.PORTRAIT, palette * 13 + 1)
fun selfCover(palette: Int) = ArtSpec(palette, ArtMotif.AURORA, palette * 17 + 5)

@Composable
fun OwnProfileScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val profile = container.profiles.profile.collectAsState().value ?: return
    val sessions by container.history.sessions.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    val list = rememberLazyListState()
    val colors = Pulse.colors
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.ProfileViewed(own = true)) }
    val progress = FameProgression.progress(profile.famePoints)
    val badges = remember(profile, sessions) { badgesFor(profile.famePoints, sessions) }

    LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = BottomBarClearance) {
        item(key = "cover") {
            ProfileCover(
                selfCover(profile.avatarPalette),
                selfAvatar(profile.avatarPalette),
                scroll = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else 500f,
                ring = AvatarRing.SELF,
            ) {
                GlassIconButton(
                    Glyphs.Sliders,
                    contentDescription = "Settings",
                    onClick = { navigator.push(Route.Settings) },
                    modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(Space.lg),
                )
            }
        }
        item(key = "identity") {
            Column(Modifier.padding(horizontal = Space.screen)) {
                Text(profile.displayName, style = Pulse.type.display, color = colors.textPrimary, maxLines = 2)
                Text("@${profile.username}", style = Pulse.type.label, color = colors.textSecondary)
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(Space.sm))
                    Text(profile.bio, style = Pulse.type.body, color = colors.textSecondary)
                }
                Spacer(Modifier.height(Space.lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatColumn(compactCount(profile.famePoints), "Fame points", align = Alignment.Start)
                    StatColumn(profile.sessionsCompleted.toString(), "Lives")
                    StatColumn(compactCount(sessions.maxOfOrNull { it.peakViewers } ?: 0), "Best peak", align = Alignment.End)
                }
                Spacer(Modifier.height(Space.lg))
                LevelCard(progress)
                Spacer(Modifier.height(Space.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    SecondaryButton("Edit profile", { navigator.push(Route.EditProfile) }, Modifier.weight(1f), leading = Glyphs.Edit)
                    SecondaryButton("Plan: Free", { navigator.push(Route.Paywall) }, Modifier.weight(1f), leading = Glyphs.Sparkles)
                }
                Spacer(Modifier.height(Space.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    PulseChip("Lives", selected = tab == 0, onClick = { tab = 0 })
                    PulseChip("Badges", selected = tab == 1, onClick = { tab = 1 })
                }
                Spacer(Modifier.height(Space.md))
            }
        }
        if (tab == 0) {
            if (sessions.isEmpty()) {
                item(key = "no-lives") {
                    EmptyState(
                        Glyphs.Mic,
                        Gloss.Violet,
                        title = "Your first live is waiting",
                        body = "Go live, talk about anything, and your session results will collect here.",
                        action = "Go live",
                        onAction = { navigator.push(Route.LiveSetup()) },
                    )
                }
            } else {
                items(sessions, key = { it.id }) { summary ->
                    SessionRow(summary, Modifier.padding(horizontal = Space.screen, vertical = Space.xs)) {
                        navigator.push(Route.Summary(summary.id))
                    }
                }
            }
        } else {
            items(badges.chunked(3), key = { row -> row.first().title }) { row ->
                Row(Modifier.padding(horizontal = Space.screen, vertical = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    row.forEach { BadgeTile(it, Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun badgesFor(points: Int, sessions: List<SessionSummary>): List<Badge> {
    val level = FameProgression.levelFor(points)
    return listOf(
        Badge("First live", "Finish a live", Gloss.Violet, Glyphs.Mic, sessions.isNotEmpty()),
        Badge("Crowd pleaser", "Reach 1K peak", Gloss.Blue, Glyphs.Users, sessions.any { it.peakViewers >= 1_000 }),
        Badge("Gifted", "Get a major gift", Gloss.Gold, Glyphs.Gift, sessions.any { it.topGiftId == "crown" || it.topGiftId == "galaxy" }),
        Badge("Storyteller", "Talk 10 minutes", Gloss.Cyan, Glyphs.Book, sessions.any { it.durationSeconds >= 600 }),
        Badge("Regular", "Finish 5 lives", Gloss.Mint, Glyphs.Bolt, sessions.size >= 5),
        Badge("Rising", "Reach Rising Creator", Gloss.Coral, Glyphs.Star, level >= FameLevel.RISING_CREATOR),
    )
}

@Composable
private fun SessionRow(summary: SessionSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Pulse.colors
    Row(
        modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .clip(Shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, Shapes.card)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Wave, Gloss.Violet, size = 44.dp)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(summary.title.ifBlank { "Untitled live" }, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${durationLabel(summary.durationSeconds)} · ${compactCount(summary.peakViewers)} peak · +${summary.famePointsEarned} pts",
                style = Pulse.type.meta,
                color = colors.textMuted,
            )
        }
        PulseIcon(Glyphs.ChevronRight, colors.textMuted, size = 18.dp)
    }
}

@Composable
fun CreatorProfileScreen(creatorId: String, navigator: Navigator) {
    val creator = remember(creatorId) { SeedContent.creator(creatorId) }
    val container = LocalAppContainer.current
    if (creator == null) {
        AtmosphereBackground {
            EmptyState(Glyphs.User, Gloss.Slate, "Creator not found", "This profile isn't available.", Modifier.align(Alignment.Center), "Go back", { navigator.pop() })
        }
        return
    }
    LaunchedEffect(creatorId) { container.analytics.log(AnalyticsEvent.ProfileViewed(own = false)) }
    val posts = remember(creatorId) { SeedContent.postsBy(creatorId) }
    val live = remember(creatorId) { SeedContent.liveFor(creatorId) }
    val list = rememberLazyListState()
    val colors = Pulse.colors

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
            item(key = "cover") {
                ProfileCover(
                    creator.cover,
                    creator.avatar,
                    scroll = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else 500f,
                    ring = if (live != null) AvatarRing.LIVE else AvatarRing.STORY,
                )
            }
            item(key = "identity") {
                Column(Modifier.padding(horizontal = Space.screen)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(creator.displayName, style = Pulse.type.display, color = colors.textPrimary, modifier = Modifier.weight(1f, fill = false))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("@${creator.handle} · ${creator.category.label}", style = Pulse.type.label, color = colors.textSecondary)
                        Spacer(Modifier.width(Space.sm))
                        SimulatedTag(text = "Simulated creator")
                    }
                    Spacer(Modifier.height(Space.md))
                    Text(creator.bio, style = Pulse.type.body, color = colors.textSecondary)
                    Spacer(Modifier.height(Space.lg))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatColumn(compactCount(creator.followers), "Followers", align = Alignment.Start)
                        StatColumn(compactCount(creator.totalLikes), "Likes")
                        StatColumn(creator.levelTitle, "Level", align = Alignment.End)
                    }
                    if (live != null) {
                        Spacer(Modifier.height(Space.lg))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(Shapes.panel)
                                .background(colors.live.copy(alpha = 0.12f))
                                .border(1.dp, colors.live.copy(alpha = 0.35f), Shapes.panel)
                                .padding(Space.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                LiveBadge(label = "LIVE NOW")
                                Spacer(Modifier.height(Space.sm))
                                Text(live.title, style = Pulse.type.titleSmall, color = colors.textPrimary)
                            }
                            ViewerPill(live.viewers)
                        }
                    }
                    Spacer(Modifier.height(Space.xl))
                    Text("Posts", style = Pulse.type.title, color = colors.textPrimary)
                    Spacer(Modifier.height(Space.md))
                }
            }
            items(posts.chunked(2), key = { row -> row.first().id }) { row ->
                Row(Modifier.padding(horizontal = Space.screen, vertical = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    row.forEachIndexed { i, post ->
                        PostTile(post, i, onOpen = { navigator.push(Route.PostDetail(post.id)) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item(key = "nav-space") {
                Column {
                    Spacer(Modifier.height(Space.xxl))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
        GlassIconButton(
            Glyphs.ChevronLeft,
            contentDescription = "Back",
            onClick = { navigator.pop() },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(Space.lg),
        )
    }
}
