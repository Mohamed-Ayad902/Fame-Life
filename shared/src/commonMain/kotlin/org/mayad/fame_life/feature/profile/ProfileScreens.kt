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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.badge_crowd
import famelife.shared.generated.resources.badge_crowd_detail
import famelife.shared.generated.resources.badge_first_live
import famelife.shared.generated.resources.badge_first_live_detail
import famelife.shared.generated.resources.badge_gifted
import famelife.shared.generated.resources.badge_gifted_detail
import famelife.shared.generated.resources.badge_regular
import famelife.shared.generated.resources.badge_regular_detail
import famelife.shared.generated.resources.badge_rising
import famelife.shared.generated.resources.badge_rising_detail
import famelife.shared.generated.resources.badge_storyteller
import famelife.shared.generated.resources.badge_storyteller_detail
import famelife.shared.generated.resources.common_back
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.post_go_back
import famelife.shared.generated.resources.profile_badges
import famelife.shared.generated.resources.profile_best_peak
import famelife.shared.generated.resources.profile_creator_not_found
import famelife.shared.generated.resources.profile_edit
import famelife.shared.generated.resources.profile_fame_points
import famelife.shared.generated.resources.profile_first_live_body
import famelife.shared.generated.resources.profile_first_live_title
import famelife.shared.generated.resources.profile_followers
import famelife.shared.generated.resources.profile_level
import famelife.shared.generated.resources.profile_likes
import famelife.shared.generated.resources.profile_live_now
import famelife.shared.generated.resources.profile_lives
import famelife.shared.generated.resources.profile_plan_free
import famelife.shared.generated.resources.profile_plan_pro
import famelife.shared.generated.resources.profile_posts
import famelife.shared.generated.resources.profile_session_meta
import famelife.shared.generated.resources.profile_settings
import famelife.shared.generated.resources.profile_simulated_creator
import famelife.shared.generated.resources.profile_unavailable
import famelife.shared.generated.resources.profile_untitled_live
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
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
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.headerScroll
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.durationText
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.title
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.progression.FameLevel
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.home.PostTile

private const val BADGES_PER_ROW = 3
private const val POSTS_PER_ROW = 2
private const val CROWD_PEAK = 1_000
private const val STORYTELLER_SECONDS = 600
private const val REGULAR_LIVES = 5

private enum class ProfileTab { LIVES, BADGES }

@Immutable
data class OwnProfileState(val profile: CreatorProfile, val sessions: List<SessionSummary>, val plan: Plan)

/** Tier 1: the signed-in creator's profile. Reads the account, logs the view, owns the tab choice. */
@Composable
fun OwnProfileScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val profile = container.profiles.profile.collectAsState().value ?: return
    val sessions by container.history.sessions.collectAsState()
    val entitlements by container.entitlements.entitlements.collectAsState()
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.ProfileViewed(own = true)) }
    OwnProfileContent(
        state = OwnProfileState(profile, sessions, entitlements.plan),
        onSettings = { navigator.push(Route.Settings) },
        onEdit = { navigator.push(Route.EditProfile) },
        onPlan = { navigator.push(Route.Paywall) },
        onGoLive = { navigator.push(Route.LiveSetup()) },
        onSession = { navigator.push(Route.Summary(it)) },
    )
}

/** Tier 2: stateless. Phones scroll one column; wide windows keep the identity pane beside the lives and badges. */
@Composable
fun OwnProfileContent(
    state: OwnProfileState,
    onSettings: () -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    onGoLive: () -> Unit,
    onSession: (String) -> Unit,
) {
    var tabName by rememberSaveable { mutableStateOf(ProfileTab.LIVES.name) }
    val tab = ProfileTab.valueOf(tabName)
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    val dimens = Pulse.dimens
    val badges = remember(state.profile.famePoints, state.sessions) { badgesFor(state.profile.famePoints, state.sessions) }
    val cover: @Composable () -> Unit = {
        ProfileCover(selfCover(state.profile.avatarPalette), selfAvatar(state.profile.avatarPalette), scroll = { list.headerScroll() }, ring = AvatarRing.SELF) {
            GlassIconButton(
                Glyphs.Sliders,
                contentDescription = stringResource(Res.string.profile_settings),
                onClick = onSettings,
                modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(dimens.lg),
            )
        }
    }
    val identity: @Composable () -> Unit = {
        OwnIdentity(state, tab, onTab = { tabName = it.name }, onEdit, onPlan, cascade)
    }
    if (Pulse.width.split) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                cover()
                identity()
            }
            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(top = dimens.lg, bottom = dimens.bottomBarClearance),
            ) { ownTabItems(tab, state.sessions, badges, cascade, onGoLive, onSession) }
        }
    } else {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dimens.bottomBarClearance)) {
            item(key = "cover") { cover() }
            item(key = "identity") { identity() }
            ownTabItems(tab, state.sessions, badges, cascade, onGoLive, onSession)
        }
    }
}

@Composable
private fun OwnIdentity(
    state: OwnProfileState,
    tab: ProfileTab,
    onTab: (ProfileTab) -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    cascade: CascadeState,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val profile = state.profile
    Column(Modifier.padding(horizontal = dimens.gutter)) {
        Text(profile.displayName, style = Pulse.type.display, color = colors.textPrimary, maxLines = 2)
        Text("@${profile.username}", style = Pulse.type.label, color = colors.textSecondary)
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(dimens.sm))
            Text(profile.bio, style = Pulse.type.body, color = colors.textSecondary)
        }
        Spacer(Modifier.height(dimens.lg))
        Row(Modifier.fillMaxWidth().cascadeIn(cascade, "stats"), horizontalArrangement = Arrangement.SpaceBetween) {
            StatColumn(compactCount(profile.famePoints), stringResource(Res.string.profile_fame_points), align = Alignment.Start)
            StatColumn(profile.sessionsCompleted.toString(), stringResource(Res.string.profile_lives))
            StatColumn(compactCount(state.sessions.maxOfOrNull { it.peakViewers } ?: 0), stringResource(Res.string.profile_best_peak), align = Alignment.End)
        }
        Spacer(Modifier.height(dimens.lg))
        LevelCard(FameProgression.progress(profile.famePoints), Modifier.cascadeIn(cascade, "level"))
        Spacer(Modifier.height(dimens.lg))
        Row(Modifier.cascadeIn(cascade, "actions"), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
            SecondaryButton(stringResource(Res.string.profile_edit), onEdit, Modifier.weight(1f), leading = Glyphs.Edit)
            SecondaryButton(
                stringResource(if (state.plan == Plan.PRO) Res.string.profile_plan_pro else Res.string.profile_plan_free),
                onPlan,
                Modifier.weight(1f),
                leading = Glyphs.Sparkles,
            )
        }
        Spacer(Modifier.height(dimens.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
            PulseChip(stringResource(Res.string.profile_lives), selected = tab == ProfileTab.LIVES, onClick = { onTab(ProfileTab.LIVES) })
            PulseChip(stringResource(Res.string.profile_badges), selected = tab == ProfileTab.BADGES, onClick = { onTab(ProfileTab.BADGES) })
        }
        Spacer(Modifier.height(dimens.md))
    }
}

/** Tier 3: the lives or badges list, shared by both layouts. */
private fun LazyListScope.ownTabItems(
    tab: ProfileTab,
    sessions: List<SessionSummary>,
    badges: List<Badge>,
    cascade: CascadeState,
    onGoLive: () -> Unit,
    onSession: (String) -> Unit,
) {
    when (tab) {
        ProfileTab.LIVES -> if (sessions.isEmpty()) {
            item(key = "no-lives") {
                EmptyState(
                    Glyphs.Mic,
                    Gloss.Violet,
                    title = stringResource(Res.string.profile_first_live_title),
                    body = stringResource(Res.string.profile_first_live_body),
                    action = stringResource(Res.string.go_live),
                    onAction = onGoLive,
                )
            }
        } else {
            items(sessions, key = { it.id }) { summary ->
                SessionRow(
                    summary,
                    Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.xs).cascadeIn(cascade, "session-${summary.id}"),
                ) { onSession(summary.id) }
            }
        }
        ProfileTab.BADGES -> items(badges.chunked(BADGES_PER_ROW), key = { row -> "badges-${row.first().title.key}" }) { row ->
            Row(
                Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.xs).cascadeIn(cascade, "badges-${row.first().title.key}"),
                horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm),
            ) {
                row.forEach { BadgeTile(it, Modifier.weight(1f)) }
                repeat(BADGES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun badgesFor(points: Int, sessions: List<SessionSummary>): List<Badge> {
    val level = FameProgression.levelFor(points)
    return listOf(
        Badge(Res.string.badge_first_live, Res.string.badge_first_live_detail, Gloss.Violet, Glyphs.Mic, sessions.isNotEmpty()),
        Badge(Res.string.badge_crowd, Res.string.badge_crowd_detail, Gloss.Blue, Glyphs.Users, sessions.any { it.peakViewers >= CROWD_PEAK }),
        Badge(Res.string.badge_gifted, Res.string.badge_gifted_detail, Gloss.Gold, Glyphs.Gift, sessions.any { it.topGiftId == "crown" || it.topGiftId == "galaxy" }),
        Badge(Res.string.badge_storyteller, Res.string.badge_storyteller_detail, Gloss.Cyan, Glyphs.Book, sessions.any { it.durationSeconds >= STORYTELLER_SECONDS }),
        Badge(Res.string.badge_regular, Res.string.badge_regular_detail, Gloss.Mint, Glyphs.Bolt, sessions.size >= REGULAR_LIVES),
        Badge(Res.string.badge_rising, Res.string.badge_rising_detail, Gloss.Coral, Glyphs.Star, level >= FameLevel.RISING_CREATOR),
    )
}

@Composable
private fun SessionRow(summary: SessionSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .clip(Pulse.shapes.card)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.card)
            .padding(dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Glyphs.Wave, Gloss.Violet, size = dimens.tileMd)
        Spacer(Modifier.width(dimens.md))
        Column(Modifier.weight(1f)) {
            Text(
                summary.title.ifBlank { stringResource(Res.string.profile_untitled_live) },
                style = Pulse.type.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.profile_session_meta, durationText(summary.durationSeconds), compactCount(summary.peakViewers), summary.famePointsEarned),
                style = Pulse.type.meta,
                color = colors.textMuted,
            )
        }
        PulseIcon(Glyphs.ChevronRight, colors.textMuted, size = dimens.iconSmall)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Creator profile

@Immutable
data class CreatorProfileState(val creator: Creator, val posts: List<Post>, val live: LiveRoom?)

/** Tier 1: a fictional creator's profile. */
@Composable
fun CreatorProfileScreen(creatorId: String, navigator: Navigator) {
    val container = LocalAppContainer.current
    val state = remember(creatorId) {
        SeedContent.creator(creatorId)?.let { CreatorProfileState(it, SeedContent.postsBy(creatorId), SeedContent.liveFor(creatorId)) }
    }
    if (state == null) {
        AtmosphereBackground {
            EmptyState(
                Glyphs.User,
                Gloss.Slate,
                stringResource(Res.string.profile_creator_not_found),
                stringResource(Res.string.profile_unavailable),
                Modifier.align(Alignment.Center),
                stringResource(Res.string.post_go_back),
                { navigator.pop() },
            )
        }
        return
    }
    LaunchedEffect(creatorId) { container.analytics.log(AnalyticsEvent.ProfileViewed(own = false)) }
    CreatorProfileContent(state, onBack = { navigator.pop() }, onPost = { navigator.push(Route.PostDetail(it)) })
}

/** Tier 2: stateless. Wide windows put the identity beside a wider post grid. */
@Composable
fun CreatorProfileContent(state: CreatorProfileState, onBack: () -> Unit, onPost: (String) -> Unit) {
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    val dimens = Pulse.dimens
    val cover: @Composable () -> Unit = {
        ProfileCover(
            state.creator.cover,
            state.creator.avatar,
            scroll = { list.headerScroll() },
            ring = if (state.live != null) AvatarRing.LIVE else AvatarRing.STORY,
        )
    }
    Box(Modifier.fillMaxSize().background(Pulse.colors.background)) {
        if (Pulse.width.split) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    cover()
                    CreatorIdentity(state, cascade)
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1.2f).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars),
                    contentPadding = PaddingValues(top = dimens.lg),
                ) { creatorPosts(state.posts, POSTS_PER_ROW + 1, cascade, onPost) }
            }
        } else {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                item(key = "cover") { cover() }
                item(key = "identity") { CreatorIdentity(state, cascade) }
                creatorPosts(state.posts, POSTS_PER_ROW, cascade, onPost)
            }
        }
        GlassIconButton(
            Glyphs.ChevronLeft,
            contentDescription = stringResource(Res.string.common_back),
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(dimens.lg),
        )
    }
}

@Composable
private fun CreatorIdentity(state: CreatorProfileState, cascade: CascadeState) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val creator = state.creator
    Column(Modifier.padding(horizontal = dimens.gutter)) {
        Text(creator.displayName, style = Pulse.type.display, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("@${creator.handle} · ${creator.category.label()}", style = Pulse.type.label, color = colors.textSecondary, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(dimens.sm))
            SimulatedTag(text = stringResource(Res.string.profile_simulated_creator))
        }
        Spacer(Modifier.height(dimens.md))
        Text(creator.bio, style = Pulse.type.body, color = colors.textSecondary)
        Spacer(Modifier.height(dimens.lg))
        Row(Modifier.fillMaxWidth().cascadeIn(cascade, "stats"), horizontalArrangement = Arrangement.SpaceBetween) {
            StatColumn(compactCount(creator.followers), stringResource(Res.string.profile_followers), align = Alignment.Start)
            StatColumn(compactCount(creator.totalLikes), stringResource(Res.string.profile_likes))
            StatColumn(creator.level.title(), stringResource(Res.string.profile_level), align = Alignment.End)
        }
        val live = state.live
        if (live != null) {
            Spacer(Modifier.height(dimens.lg))
            Row(
                Modifier
                    .fillMaxWidth()
                    .cascadeIn(cascade, "live")
                    .clip(Pulse.shapes.panel)
                    .background(colors.live.copy(alpha = 0.12f))
                    .border(dimens.hairline, colors.live.copy(alpha = 0.35f), Pulse.shapes.panel)
                    .padding(dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    LiveBadge(label = stringResource(Res.string.profile_live_now))
                    Spacer(Modifier.height(dimens.sm))
                    Text(live.title, style = Pulse.type.titleSmall, color = colors.textPrimary)
                }
                ViewerPill(live.viewers)
            }
        }
        Spacer(Modifier.height(dimens.xl))
        Text(stringResource(Res.string.profile_posts), style = Pulse.type.title, color = colors.textPrimary)
        Spacer(Modifier.height(dimens.md))
    }
}

private fun LazyListScope.creatorPosts(posts: List<Post>, perRow: Int, cascade: CascadeState, onPost: (String) -> Unit) {
    items(posts.chunked(perRow), key = { row -> row.first().id }) { row ->
        Row(
            Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.sm),
            horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md),
        ) {
            row.forEachIndexed { i, post ->
                PostTile(post, i, onOpen = { onPost(post.id) }, modifier = Modifier.weight(1f).cascadeIn(cascade, post.id))
            }
            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    item(key = "nav-space") {
        Column {
            Spacer(Modifier.height(Pulse.dimens.xxl))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun OwnProfilePreview() = PreviewContainer {
    AtmosphereBackground { OwnProfileContent(OwnProfileState(SampleData.profile, SampleData.sessions, Plan.FREE), {}, {}, {}, {}, {}) }
}

@ScreenPreviews
@Composable
private fun OwnProfileEmptyPreview() = PreviewContainer {
    AtmosphereBackground { OwnProfileContent(OwnProfileState(SampleData.newProfile, emptyList(), Plan.FREE), {}, {}, {}, {}, {}) }
}

@ScreenPreviews
@Composable
private fun CreatorProfilePreview() = PreviewContainer {
    val creator = SampleData.creator
    CreatorProfileContent(CreatorProfileState(creator, SeedContent.postsBy(creator.id), SeedContent.liveFor(creator.id)), {}, {})
}
