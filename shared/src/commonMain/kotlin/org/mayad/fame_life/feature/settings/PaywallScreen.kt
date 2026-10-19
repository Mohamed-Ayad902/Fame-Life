package org.mayad.fame_life.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.paywall_all
import famelife.shared.generated.resources.paywall_audience_moods
import famelife.shared.generated.resources.paywall_audience_sizes
import famelife.shared.generated.resources.paywall_critic_mode
import famelife.shared.generated.resources.paywall_free
import famelife.shared.generated.resources.paywall_gift_tiers
import famelife.shared.generated.resources.paywall_gifts_never_bought
import famelife.shared.generated.resources.paywall_gifts_simulated
import famelife.shared.generated.resources.paywall_included
import famelife.shared.generated.resources.paywall_live_length
import famelife.shared.generated.resources.paywall_live_styles
import famelife.shared.generated.resources.paywall_lives_per_day
import famelife.shared.generated.resources.paywall_minutes
import famelife.shared.generated.resources.paywall_not_included
import famelife.shared.generated.resources.paywall_pro
import famelife.shared.generated.resources.paywall_saved_lives
import famelife.shared.generated.resources.paywall_some_of
import famelife.shared.generated.resources.paywall_subtitle
import famelife.shared.generated.resources.paywall_title
import famelife.shared.generated.resources.paywall_unavailable
import famelife.shared.generated.resources.paywall_unavailable_body
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.ScreenHeader
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.domain.simulation.GiftTier
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe


private const val SECONDS_PER_MINUTE = 60
private const val PANEL_ALPHA = 0.85f

/** Tier 1: the plan comparison. Billing isn't wired, so the screen is informational and the buy button is disabled. */
@Composable
fun PaywallScreen(onBack: () -> Unit, viewModel: PaywallViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PaywallContent(state.free, state.pro, onBack = onBack)
}

/** Tier 2: stateless comparison. Wide windows put the pitch beside the table. */
@Composable
fun PaywallContent(free: PlanLimits, pro: PlanLimits, onBack: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val pitch: @Composable ColumnScope.() -> Unit = {
        GlossyIcon(Glyphs.Crown, Gloss.Gold, size = dimens.tileHero, glyphSize = dimens.tileHero / 2)
        Spacer(Modifier.height(dimens.lg))
        Text(stringResource(Res.string.paywall_title, AppConfig.APP_NAME), style = Pulse.type.display, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimens.xs))
        Text(stringResource(Res.string.paywall_subtitle), style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
    val table: @Composable ColumnScope.() -> Unit = {
        CompareTable(free, pro)
        Spacer(Modifier.height(dimens.xl))
        PrimaryButton(stringResource(Res.string.paywall_unavailable), onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), brush = colors.goldGradient)
        Spacer(Modifier.height(dimens.sm))
        Text(stringResource(Res.string.paywall_unavailable_body), style = Pulse.type.meta, color = colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimens.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SimulatedTag(text = stringResource(Res.string.paywall_gifts_simulated))
            Spacer(Modifier.width(dimens.sm))
            Text(stringResource(Res.string.paywall_gifts_never_bought), style = Pulse.type.meta, color = colors.textMuted)
        }
        Spacer(Modifier.height(dimens.xxl))
    }
    AtmosphereBackground(accent = colors.gold) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("", onBack = onBack)
            if (Pulse.width.split) {
                SplitPanes(
                    Modifier.padding(horizontal = dimens.gutter),
                    leading = { Column(horizontalAlignment = Alignment.CenterHorizontally, content = pitch) },
                    trailing = { Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, content = table) },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = dimens.gutter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    pitch()
                    Spacer(Modifier.height(dimens.xl))
                    table()
                }
            }
        }
    }
}

@Composable
private fun CompareTable(free: PlanLimits, pro: PlanLimits) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    @Composable
    fun countOf(count: Int, total: Int) =
        if (count >= total) stringResource(Res.string.paywall_all, total) else stringResource(Res.string.paywall_some_of, count, total)
    val rows = listOf(
        Triple(Res.string.paywall_live_length, stringResource(Res.string.paywall_minutes, free.maxSessionSeconds / SECONDS_PER_MINUTE), stringResource(Res.string.paywall_minutes, pro.maxSessionSeconds / SECONDS_PER_MINUTE)),
        Triple(Res.string.paywall_lives_per_day, "${free.sessionsPerDay}", "${pro.sessionsPerDay}"),
        Triple(Res.string.paywall_audience_sizes, countOf(free.presets.size, AudiencePreset.entries.size), countOf(pro.presets.size, AudiencePreset.entries.size)),
        Triple(Res.string.paywall_audience_moods, countOf(free.vibes.size, AudienceVibe.entries.size), countOf(pro.vibes.size, AudienceVibe.entries.size)),
        Triple(Res.string.paywall_gift_tiers, countOf(free.giftTiers.size, GiftTier.entries.size), countOf(pro.giftTiers.size, GiftTier.entries.size)),
        Triple(Res.string.paywall_critic_mode, if (free.hatersMode) stringResource(Res.string.paywall_included) else null, stringResource(Res.string.paywall_included)),
        Triple(Res.string.paywall_live_styles, "${free.liveStyles.size}", "${pro.liveStyles.size}"),
        Triple(Res.string.paywall_saved_lives, "${free.historySize}", "${pro.historySize}"),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface.copy(alpha = PANEL_ALPHA))
            .border(dimens.hairline, colors.border, Pulse.shapes.panel)
            .padding(dimens.lg),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(LABEL_WEIGHT))
            Text(stringResource(Res.string.paywall_free), style = Pulse.type.label, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(stringResource(Res.string.paywall_pro), style = Pulse.type.label, color = colors.gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(dimens.sm))
        rows.forEach { (label, freeValue, proValue) ->
            CompareRow(stringResource(label), freeValue, proValue, Modifier.cascadeIn(cascade, label.key))
        }
    }
}

private const val LABEL_WEIGHT = 1.4f

@Composable
private fun CompareRow(label: String, free: String?, pro: String, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Row(modifier.fillMaxWidth().padding(vertical = Pulse.dimens.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Pulse.type.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(LABEL_WEIGHT))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (free == null) {
                PulseIcon(Glyphs.Lock, colors.textMuted, size = Pulse.dimens.iconSmall, contentDescription = stringResource(Res.string.paywall_not_included))
            } else {
                Text(free, style = Pulse.type.bodySmall, color = colors.textSecondary)
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(pro, style = Pulse.type.label, color = colors.textPrimary)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun PaywallPreview() = PreviewContainer { PaywallContent(PlanLimits.Free, PlanLimits.Pro, onBack = {}) }
