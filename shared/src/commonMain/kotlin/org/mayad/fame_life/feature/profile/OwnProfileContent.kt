package org.mayad.fame_life.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.profile_badges
import famelife.shared.generated.resources.profile_best_peak
import famelife.shared.generated.resources.profile_edit
import famelife.shared.generated.resources.profile_fame_points
import famelife.shared.generated.resources.profile_first_live_body
import famelife.shared.generated.resources.profile_first_live_title
import famelife.shared.generated.resources.profile_lives
import famelife.shared.generated.resources.profile_plan_free
import famelife.shared.generated.resources.profile_plan_pro
import famelife.shared.generated.resources.profile_session_meta
import famelife.shared.generated.resources.profile_settings
import famelife.shared.generated.resources.profile_untitled_live
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.StatColumn
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
import org.mayad.fame_life.core.ui.detailRes
import org.mayad.fame_life.core.ui.durationText
import org.mayad.fame_life.core.ui.gloss
import org.mayad.fame_life.core.ui.glyph
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.title
import org.mayad.fame_life.core.ui.titleRes
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.progression.Achievements
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.SessionSummary

private const val BADGES_PER_ROW = 3

/** Tier 2: stateless. Phones scroll one column; wide windows keep the identity pane beside the lives and badges. */
@Composable
fun OwnProfileContent(
    state: OwnProfileUiState,
    onTab: (ProfileTab) -> Unit,
    onSettings: () -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    onGoLive: () -> Unit,
    onSession: (String) -> Unit,
) {
    val profile = state.profile ?: return
    val tab = state.tab
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    val dimens = Pulse.dimens
    val badges = remember(profile.famePoints, state.sessions) { badgesFor(profile.famePoints, state.sessions) }
    val cover: @Composable () -> Unit = {
        ProfileCover(selfCover(profile.avatarPalette), selfAvatar(profile.avatarPalette), scroll = { list.headerScroll() }, ring = AvatarRing.SELF) {
            GlassIconButton(
                Glyphs.Sliders,
                contentDescription = stringResource(Res.string.profile_settings),
                onClick = onSettings,
                modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(dimens.lg),
            )
        }
    }
    val identity: @Composable () -> Unit = {
        OwnIdentity(profile, state, onTab, onEdit, onPlan, cascade)
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
    profile: CreatorProfile,
    state: OwnProfileUiState,
    onTab: (ProfileTab) -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    cascade: CascadeState,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val tab = state.tab
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
            StatColumn(compactCount(state.bestPeak), stringResource(Res.string.profile_best_peak), align = Alignment.End)
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
    sessions: ImmutableList<SessionSummary>,
    badges: ImmutableList<Badge>,
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

/** Badges are derived from progress on the device; they unlock nothing paid. */
private fun badgesFor(points: Int, sessions: List<SessionSummary>): ImmutableList<Badge> =
    Achievements.evaluate(points, sessions).map { Badge(it.achievement.titleRes, it.achievement.detailRes, it.achievement.gloss, it.achievement.glyph, it.unlocked) }
        .toImmutableList()

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
// Previews

@ScreenPreviews
@Composable
private fun OwnProfilePreview() = PreviewContainer {
    AtmosphereBackground { OwnProfileContent(OwnProfileUiState(SampleData.profile, SampleData.sessions.toImmutableList(), Plan.FREE), {}, {}, {}, {}, {}, {}) }
}

@ScreenPreviews
@Composable
private fun OwnProfileEmptyPreview() = PreviewContainer {
    AtmosphereBackground { OwnProfileContent(OwnProfileUiState(SampleData.newProfile), {}, {}, {}, {}, {}, {}) }
}
