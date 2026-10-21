package org.mayad.fame_life.feature.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.badge_locked
import famelife.shared.generated.resources.badge_unlocked
import famelife.shared.generated.resources.home_badges_count
import famelife.shared.generated.resources.home_for_you
import famelife.shared.generated.resources.home_for_you_eyebrow
import famelife.shared.generated.resources.home_live_now
import famelife.shared.generated.resources.home_mission
import famelife.shared.generated.resources.home_mission_goal
import famelife.shared.generated.resources.home_mission_start
import famelife.shared.generated.resources.home_simulated_rooms
import famelife.shared.generated.resources.home_status_title
import famelife.shared.generated.resources.home_trending_topics
import famelife.shared.generated.resources.home_welcome_back
import famelife.shared.generated.resources.hud_level_description
import famelife.shared.generated.resources.hud_points
import famelife.shared.generated.resources.hud_streak
import famelife.shared.generated.resources.hud_wallet
import famelife.shared.generated.resources.level_card_points_to
import famelife.shared.generated.resources.level_card_top
import famelife.shared.generated.resources.profile_badges
import famelife.shared.generated.resources.profile_best_peak
import famelife.shared.generated.resources.profile_lives
import famelife.shared.generated.resources.tab_inbox
import famelife.shared.generated.resources.tab_with_new
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AchievementMedal
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.FlameEmblem
import org.mayad.fame_life.core.designsystem.components.GlossDisc
import org.mayad.fame_life.core.designsystem.components.GlossyPillButton
import org.mayad.fame_life.core.designsystem.components.HudIconButton
import org.mayad.fame_life.core.designsystem.components.HudPanel
import org.mayad.fame_life.core.designsystem.components.HudPill
import org.mayad.fame_life.core.designsystem.components.HudProgressBar
import org.mayad.fame_life.core.designsystem.components.LevelAvatar
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.bleed
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseAspect
import org.mayad.fame_life.core.designsystem.theme.PulseWidth
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.detailRes
import org.mayad.fame_life.core.ui.gloss
import org.mayad.fame_life.core.ui.glyph
import org.mayad.fame_life.core.ui.title
import org.mayad.fame_life.core.ui.titleRes
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.Challenge
import org.mayad.fame_life.domain.progression.Achievements
import org.mayad.fame_life.domain.progression.FameProgression
import kotlinx.collections.immutable.toImmutableList

private const val PARALLAX = 0.35f

/** Grid position of the hero row (stories, hero), used to limit parallax to when it's on top. */
private const val HERO_ROW = 1

/**
 * Tier 2: stateless. A pinned game HUD (blurred where the platform can) floats over one staggered grid whose column
 * count follows the window width. On wide windows the featured live sits beside the status and mission panels.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onInbox: () -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
    onChallenge: (String) -> Unit,
) {
    val dimens = Pulse.dimens
    val grid = rememberLazyStaggeredGridState()
    val cascade = rememberCascadeState()
    val haze = rememberHazeState()
    val density = LocalDensity.current
    var hudHeight by remember { mutableStateOf(0.dp) }
    val columns = when (Pulse.width) {
        PulseWidth.Compact -> 2
        PulseWidth.Medium -> 3
        PulseWidth.Expanded -> 4
    }
    val goLive = { onChallenge("") }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = grid,
            modifier = Modifier.fillMaxSize().hazeSource(haze),
            contentPadding = PaddingValues(start = dimens.gutter, end = dimens.gutter, top = hudHeight + dimens.sm, bottom = dimens.bottomBarClearance),
            horizontalArrangement = Arrangement.spacedBy(dimens.md),
            verticalItemSpacing = dimens.xl,
        ) {
            item(span = StaggeredGridItemSpan.FullLine, key = "stories") {
                CreatorStoryRail(
                    onCreator = { onCreator(it.id) },
                    modifier = Modifier.bleed(dimens.gutter),
                    cascade = cascade,
                    self = selfArt(state.palette),
                    onGoLive = goLive,
                )
            }
            item(span = StaggeredGridItemSpan.FullLine, key = "hero") {
                HeroBlock(state, grid, cascade, onCreator, onChallenge)
            }
            item(span = StaggeredGridItemSpan.FullLine, key = "lives") {
                Column(Modifier.cascadeIn(cascade, "lives")) {
                    HudSectionHeader(stringResource(Res.string.home_live_now), stringResource(Res.string.home_simulated_rooms), Glyphs.Bolt, Gloss.Rose)
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
                    HudSectionHeader(stringResource(Res.string.home_trending_topics), null, Glyphs.Flame, Gloss.Coral)
                    Spacer(Modifier.height(dimens.md))
                    LazyRow(
                        modifier = Modifier.bleed(dimens.gutter),
                        contentPadding = PaddingValues(horizontal = dimens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                    ) {
                        itemsIndexed(state.topics, key = { _, topic -> topic.tag }) { index, topic ->
                            TopicCard(topic, rank = index + 1, modifier = Modifier.cascadeIn(cascade, "topic-${topic.tag}"))
                        }
                    }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine, key = "foryou") {
                HudSectionHeader(
                    stringResource(Res.string.home_for_you),
                    stringResource(Res.string.home_for_you_eyebrow),
                    Glyphs.Sparkles,
                    Gloss.Violet,
                    Modifier.cascadeIn(cascade, "foryou"),
                )
            }
            items(state.posts.size, key = { state.posts[it].id }) { index ->
                val post = state.posts[index]
                PostTile(post, index, onOpen = { onPost(post.id) }, modifier = Modifier.cascadeIn(cascade, post.id))
            }
        }
        HomeHud(
            state = state,
            haze = haze,
            grid = grid,
            onInbox = onInbox,
            modifier = Modifier.align(Alignment.TopCenter).onSizeChanged { hudHeight = with(density) { it.height.toDp() } },
        )
    }
}

/** The signed-in creator's avatar recipe; the same look the profile uses. */
private fun selfArt(palette: Int) = ArtSpec(palette, ArtMotif.PORTRAIT, palette * 13 + 1)

/** Tier 3: the hero row. Stacked on phones; on wide windows the featured live sits beside the status and mission. */
@Composable
private fun HeroBlock(
    state: HomeUiState,
    grid: LazyStaggeredGridState,
    cascade: CascadeState,
    onCreator: (String) -> Unit,
    onChallenge: (String) -> Unit,
) {
    val dimens = Pulse.dimens
    val still = Pulse.reducedMotion
    // Parallax only while the hero row is the first visible item; read inside the art's layer, never in composition.
    val parallax = remember(grid, still) {
        { if (!still && grid.firstVisibleItemIndex == HERO_ROW) grid.firstVisibleItemScrollOffset * PARALLAX else 0f }
    }
    val openFeatured = { onCreator(state.featured.creatorId) }
    val startChallenge = { onChallenge(state.challenge.title) }
    if (Pulse.width.split) {
        Row(Modifier.cascadeIn(cascade, "hero"), horizontalArrangement = Arrangement.spacedBy(dimens.lg)) {
            FeaturedLiveHero(state.featured, parallax, openFeatured, Modifier.weight(1.3f), aspect = PulseAspect.Wide)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.lg)) {
                CreatorStatusPanel(state.hud, state.palette, state.name)
                MissionPanel(state.challenge, startChallenge)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.xl)) {
            FeaturedLiveHero(state.featured, parallax, openFeatured, Modifier.cascadeIn(cascade, "hero"))
            CreatorStatusPanel(state.hud, state.palette, state.name, Modifier.cascadeIn(cascade, "status"))
            MissionPanel(state.challenge, startChallenge, Modifier.cascadeIn(cascade, "mission"))
        }
    }
}

/**
 * The pinned game HUD: level ring around the creator's avatar, name and level, the coin wallet, the streak when one
 * exists, and the inbox. The grid scrolls under it through a blur; a hairline appears once content is underneath.
 */
@Composable
private fun HomeHud(state: HomeUiState, haze: HazeState, grid: LazyStaggeredGridState, onInbox: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val hud = state.hud
    val inbox = stringResource(Res.string.tab_inbox)
    val levelNumber = hud.level.level.ordinal + 1
    val levelTitle = hud.level.level.title()
    val blur = remember(colors, dimens) {
        HazeBlurStyle {
            blurRadius(dimens.hudBlur)
            colorEffects(listOf(HazeColorEffect.tint(colors.hudGlass)))
            fallbackColorEffect(HazeColorEffect.tint(colors.hudGlassFallback))
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .hazeBlur(HazeInput.Sources(haze), blur)
            .drawBehind {
                // Read in draw: the divider fades in as soon as anything scrolls beneath the HUD.
                val scrolled = grid.firstVisibleItemIndex > 0 || grid.firstVisibleItemScrollOffset > 0
                if (scrolled) drawRect(colors.hudRimBrush, topLeft = Offset(0f, size.height - dimens.hairline.toPx()), size = size.copy(height = dimens.hairline.toPx()))
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dimens.gutter, vertical = dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelAvatar(
            selfArt(state.palette),
            level = levelNumber,
            fraction = hud.level.fraction,
            contentDescription = stringResource(Res.string.hud_level_description, levelNumber, levelTitle),
        )
        Spacer(Modifier.width(dimens.sm))
        Column(Modifier.weight(1f)) {
            Text(
                state.name.ifEmpty { stringResource(Res.string.home_welcome_back) },
                style = Pulse.type.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(levelTitle.uppercase(), style = Pulse.type.overline, color = colors.gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(dimens.xs))
        HudPill(hud.points, stringResource(if (hud.wallet) Res.string.hud_wallet else Res.string.hud_points, compactCount(hud.points)))
        hud.streakDays?.let { days ->
            Spacer(Modifier.width(dimens.sm))
            HudPill(days, stringResource(Res.string.hud_streak, days), accent = colors.coral, emblem = { FlameEmblem() })
        }
        HudIconButton(
            Glyphs.Bell,
            contentDescription = if (state.unread > 0) stringResource(Res.string.tab_with_new, inbox, state.unread) else inbox,
            onClick = onInbox,
            badge = state.unread,
        )
    }
}

/**
 * The creator's own simulated progress, laid out like a game "team status" card: the avatar large and centered in
 * its level ring, flanked by lives and best peak, then the level bar and the badge collection.
 */
@Composable
private fun CreatorStatusPanel(hud: CreatorHud, palette: Int, name: String, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val progress = hud.level
    val levelNumber = progress.level.ordinal + 1
    val levelTitle = progress.level.title()
    val next = progress.level.next?.title()
    HudPanel(modifier.fillMaxWidth(), title = stringResource(Res.string.home_status_title), trailing = { SimulatedTag() }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlankStat(hud.lives.toString(), stringResource(Res.string.profile_lives), Glyphs.Mic, Gloss.Violet, Modifier.weight(1f))
            LevelAvatar(
                selfArt(palette),
                level = levelNumber,
                fraction = progress.fraction,
                contentDescription = stringResource(Res.string.hud_level_description, levelNumber, levelTitle),
                size = dimens.avatarLg * 1.5f,
            )
            FlankStat(compactCount(hud.bestPeak), stringResource(Res.string.profile_best_peak), Glyphs.Eye, Gloss.Cyan, Modifier.weight(1f))
        }
        Spacer(Modifier.height(dimens.md))
        if (name.isNotEmpty()) {
            Text(name, style = Pulse.type.title, color = colors.gold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Text(levelTitle.uppercase(), style = Pulse.type.overline, color = colors.textSecondary, maxLines = 1, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(dimens.md))
        HudProgressBar(progress.fraction)
        Spacer(Modifier.height(dimens.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (progress.pointsToNext != null && next != null) stringResource(Res.string.level_card_points_to, progress.pointsToNext, next)
                else stringResource(Res.string.level_card_top),
                style = Pulse.type.meta,
                color = colors.textMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.home_badges_count, hud.earned, hud.achievements.size),
                style = Pulse.type.label,
                color = colors.gold,
                maxLines = 1,
            )
            Spacer(Modifier.width(dimens.xs))
            Text(stringResource(Res.string.profile_badges).uppercase(), style = Pulse.type.overline, color = colors.textMuted, maxLines = 1)
        }
        Spacer(Modifier.height(dimens.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hud.achievements.forEach { state ->
                val title = stringResource(state.achievement.titleRes)
                val detail = stringResource(state.achievement.detailRes)
                AchievementMedal(
                    state.achievement.glyph,
                    state.achievement.gloss,
                    state.unlocked,
                    contentDescription = stringResource(if (state.unlocked) Res.string.badge_unlocked else Res.string.badge_locked, title, detail),
                    size = dimens.tileMd,
                )
            }
        }
    }
}

/** A round glossy stat beside the status emblem: halo-ringed disc, bold number, small caps label. */
@Composable
private fun FlankStat(value: String, label: String, glyph: ImageVector, gloss: Gloss, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(dimens.tileMd)
                .drawBehind { drawCircle(gloss.glow.copy(alpha = 0.6f), size.minDimension / 2, style = Stroke(dimens.stroke.toPx())) }
                .padding(dimens.xs),
        ) { GlossDisc(gloss, glyph, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(dimens.xs))
        Text(value, style = Pulse.type.metricSmall, color = colors.textPrimary, maxLines = 1)
        Text(label.uppercase(), style = Pulse.type.overline, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The daily challenge as a quest: gold tab, trophy emblem, the brief, its time goal and a glossy start. */
@Composable
private fun MissionPanel(challenge: Challenge, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    HudPanel(modifier.fillMaxWidth(), title = stringResource(Res.string.home_mission), tab = colors.goldGradient) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossDisc(Gloss.Gold, Glyphs.Trophy, size = dimens.emblemLarge, glyphScale = 0.48f)
            Spacer(Modifier.width(dimens.lg))
            Column(Modifier.weight(1f)) {
                Text(challenge.title, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(dimens.xxs))
                Text(challenge.description, style = Pulse.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(dimens.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .clip(Pulse.shapes.pill)
                    .background(colors.background.copy(alpha = 0.45f))
                    .border(dimens.hairline, colors.hudRim, Pulse.shapes.pill)
                    .padding(horizontal = dimens.sm, vertical = dimens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseIcon(Glyphs.Clock, colors.cyan, size = dimens.iconSmall)
                Spacer(Modifier.width(dimens.xs))
                Text(stringResource(Res.string.home_mission_goal, challenge.goalMinutes), style = Pulse.type.meta, color = colors.textPrimary, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            GlossyPillButton(stringResource(Res.string.home_mission_start), onStart, leading = Glyphs.Play)
        }
    }
}

/** Game-style section header: a glossy emblem beside the eyebrow and title. */
@Composable
private fun HudSectionHeader(title: String, eyebrow: String?, glyph: ImageVector, gloss: Gloss, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        GlossDisc(gloss, glyph, size = dimens.tileXs)
        Spacer(Modifier.width(dimens.sm))
        Column {
            if (eyebrow != null) Text(eyebrow.uppercase(), style = Pulse.type.overline, color = colors.cyan, maxLines = 1)
            Text(title, style = Pulse.type.title, color = colors.textPrimary, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun HomePreview() = PreviewContainer {
    val profile = SampleData.profile
    AtmosphereBackground {
        HomeContent(
            HomeUiState.seeded().copy(
                name = profile.displayName,
                palette = profile.avatarPalette,
                unread = 2,
                hud = CreatorHud(
                    points = profile.famePoints,
                    level = FameProgression.progress(profile.famePoints),
                    lives = profile.sessionsCompleted,
                    bestPeak = SampleData.sessions.maxOf { it.peakViewers },
                    achievements = Achievements.evaluate(profile.famePoints, SampleData.sessions).toImmutableList(),
                ),
            ),
            {}, {}, {}, {},
        )
    }
}

@ScreenPreviews
@Composable
private fun HomeNewCreatorPreview() = PreviewContainer {
    AtmosphereBackground {
        HomeContent(HomeUiState.seeded().copy(name = SampleData.newProfile.displayName, palette = SampleData.newProfile.avatarPalette), {}, {}, {}, {})
    }
}
