package org.mayad.fame_life.feature.points

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.points_amount
import famelife.shared.generated.resources.streak_body
import famelife.shared.generated.resources.streak_claim
import famelife.shared.generated.resources.streak_claimed
import famelife.shared.generated.resources.streak_day
import famelife.shared.generated.resources.streak_day_claimed
import famelife.shared.generated.resources.streak_day_today
import famelife.shared.generated.resources.streak_day_upcoming
import famelife.shared.generated.resources.streak_days
import famelife.shared.generated.resources.streak_done
import famelife.shared.generated.resources.streak_eyebrow
import famelife.shared.generated.resources.streak_later
import famelife.shared.generated.resources.streak_title
import famelife.shared.generated.resources.streak_title_claimed
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.domain.economy.EconomyConfig
import org.mayad.fame_life.domain.economy.StreakDay
import org.mayad.fame_life.domain.economy.StreakDayState

/**
 * Tier 1: mounted once in the main shell. Shows the daily check-in calendar when the ViewModel says so; the server
 * grants the points.
 */
@Composable
fun DailyRewardHost(viewModel: DailyRewardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    DailyRewardDialog(
        state = state,
        onClaim = { viewModel.onIntent(DailyRewardIntent.Claim) },
        onDismiss = { viewModel.onIntent(DailyRewardIntent.Dismiss) },
    )
}

@Composable
fun DailyRewardDialog(state: DailyRewardUiState, onClaim: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DailyRewardCard(state, onClaim, onDismiss)
    }
}

/** Tier 2: the calendar card itself, stateless so it previews without a dialog window. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyRewardCard(state: DailyRewardUiState, onClaim: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    Column(
        modifier
            .widthIn(max = dimens.contentMaxWidth)
            .clip(Pulse.shapes.panel)
            .background(Brush.verticalGradient(listOf(colors.surfaceElevated, colors.surface)))
            .border(dimens.hairline, Brush.verticalGradient(listOf(colors.gold.copy(alpha = 0.55f), colors.border)), Pulse.shapes.panel)
            .padding(dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlossyIcon(Glyphs.Flame, Gloss.Gold, size = dimens.tileLg)
        Spacer(Modifier.height(dimens.md))
        Text(stringResource(Res.string.streak_eyebrow), style = Pulse.type.overline, color = colors.gold)
        Text(
            stringResource(if (state.claimed != null || state.todayReward == null) Res.string.streak_title_claimed else Res.string.streak_title),
            style = Pulse.type.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (state.streakDays > 0) {
            Spacer(Modifier.height(dimens.xs))
            Text(stringResource(Res.string.streak_days, state.streakDays), style = Pulse.type.label, color = colors.textSecondary)
        }
        Spacer(Modifier.height(dimens.lg))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(dimens.sm),
        ) {
            state.days.forEach { day ->
                // A just-collected day reads as collected before the wallet document catches up.
                val shown = if (state.claimed != null && day.state == StreakDayState.TODAY) day.copy(state = StreakDayState.CLAIMED) else day
                StreakDayTile(shown, Modifier.cascadeIn(cascade, "day-${day.number}"))
            }
        }
        Spacer(Modifier.height(dimens.lg))
        AnimatedContent(
            targetState = state.claimed,
            transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
            label = "claim",
        ) { claimed ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val error = state.error
                Text(
                    when {
                        claimed != null -> stringResource(Res.string.streak_claimed, claimed)
                        error != null -> stringResource(error.messageRes)
                        else -> stringResource(Res.string.streak_body)
                    },
                    style = Pulse.type.bodySmall,
                    color = if (error != null && claimed == null) colors.error else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimens.lg))
                val reward = state.todayReward
                if (claimed == null && reward != null) {
                    PrimaryButton(
                        stringResource(Res.string.streak_claim, reward),
                        onClick = onClaim,
                        enabled = !state.claiming,
                        leading = Glyphs.Gift,
                        brush = Brush.horizontalGradient(listOf(colors.goldDeep, colors.gold, colors.goldLight)),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(dimens.sm))
                    SecondaryButton(stringResource(Res.string.streak_later), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                } else {
                    PrimaryButton(stringResource(Res.string.streak_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StreakDayTile(day: StreakDay, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(
        when (day.state) {
            StreakDayState.CLAIMED -> Res.string.streak_day_claimed
            StreakDayState.TODAY -> Res.string.streak_day_today
            StreakDayState.UPCOMING -> Res.string.streak_day_upcoming
        },
        day.number,
        day.reward,
    )
    val today = day.state == StreakDayState.TODAY
    val claimed = day.state == StreakDayState.CLAIMED
    Column(
        modifier
            .width(dimens.tileXxl)
            .clip(Pulse.shapes.control)
            .background(
                when {
                    today -> Brush.verticalGradient(listOf(colors.gold.copy(alpha = 0.32f), colors.goldDeep.copy(alpha = 0.12f)))
                    else -> Brush.verticalGradient(listOf(colors.surfaceElevated, colors.surface))
                },
            )
            .border(if (today) dimens.ring else dimens.hairline, if (today) colors.gold else colors.border, Pulse.shapes.control)
            .padding(vertical = dimens.sm)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.streak_day, day.number), style = Pulse.type.meta, color = if (today) colors.goldLight else colors.textMuted)
        Spacer(Modifier.height(dimens.xs))
        GlossyIcon(if (claimed) Glyphs.Check else Glyphs.Sparkles, if (claimed) Gloss.Mint else if (today) Gloss.Gold else Gloss.Slate, size = dimens.tileSm)
        Spacer(Modifier.height(dimens.xs))
        Text(
            stringResource(Res.string.points_amount, day.reward),
            style = Pulse.type.label,
            color = if (claimed) colors.textMuted else colors.textPrimary,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

private val previewDays = EconomyConfig.Default.streakRewards.mapIndexed { i, reward ->
    StreakDay(i + 1, reward, if (i < 2) StreakDayState.CLAIMED else if (i == 2) StreakDayState.TODAY else StreakDayState.UPCOMING)
}.toImmutableList()

@ComponentPreviews
@Composable
private fun DailyRewardWaitingPreview() = PreviewContainer {
    DailyRewardCard(DailyRewardUiState(visible = true, days = previewDays, streakDays = 2, todayReward = 30), onClaim = {}, onDismiss = {})
}

@ComponentPreviews
@Composable
private fun DailyRewardClaimedPreview() = PreviewContainer {
    DailyRewardCard(DailyRewardUiState(visible = true, days = previewDays, streakDays = 3, todayReward = 30, claimed = 30), onClaim = {}, onDismiss = {})
}
