package org.mayad.fame_life.feature.points

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.points_ad_credited
import famelife.shared.generated.resources.points_ad_dismissed
import famelife.shared.generated.resources.points_ad_failed
import famelife.shared.generated.resources.points_ad_pending
import famelife.shared.generated.resources.points_ad_showing
import famelife.shared.generated.resources.points_ad_verifying
import famelife.shared.generated.resources.points_ads_left
import famelife.shared.generated.resources.points_ads_unavailable
import famelife.shared.generated.resources.points_ads_used_up
import famelife.shared.generated.resources.points_balance_description
import famelife.shared.generated.resources.points_claim_today
import famelife.shared.generated.resources.points_close
import famelife.shared.generated.resources.points_short_body
import famelife.shared.generated.resources.points_short_title
import famelife.shared.generated.resources.points_watch_ad
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AnimatedCount
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.feature.live.AdStatus

/** The coin mark next to point amounts: a small gold glossy tile. */
@Composable
fun PointsCoin(modifier: Modifier = Modifier) {
    GlossyIcon(Glyphs.Sparkles, Gloss.Gold, modifier, size = Pulse.dimens.iconMedium)
}

/** A compact glass pill with the coin and the balance. The number animates when the server changes it. */
@Composable
fun PointsBalancePill(balance: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.points_balance_description, balance)
    Row(
        modifier
            .height(dimens.glassButtonSmall)
            .clip(Pulse.shapes.pill)
            .background(colors.glassStrong)
            .border(dimens.hairline, Brush.verticalGradient(listOf(colors.gold.copy(alpha = 0.6f), colors.glassBorder)), Pulse.shapes.pill)
            .padding(start = dimens.xxs, end = dimens.md)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PointsCoin()
        Spacer(Modifier.width(dimens.xs))
        AnimatedCount(balance, style = Pulse.type.metricSmall, color = colors.textPrimary, compact = false)
    }
}

/**
 * Shown when going live was refused for points: how many are missing, and the ways to get them now (today's streak
 * reward, a reward ad). When neither is possible it says why instead of offering a dead button.
 */
@Composable
fun PointsShortPanel(
    cost: Int,
    balance: Int,
    adReward: Int,
    adsLeft: Int,
    adsAvailable: Boolean,
    todayReward: Int?,
    adStatus: AdStatus,
    adCredited: Int,
    onWatchAd: () -> Unit,
    onClaimDaily: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(
        modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface.copy(alpha = 0.94f))
            .border(dimens.hairline, colors.gold.copy(alpha = 0.4f), Pulse.shapes.panel)
            .padding(dimens.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(Glyphs.Sparkles, Gloss.Gold, size = dimens.tileMd)
            Spacer(Modifier.width(dimens.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.points_short_title, (cost - balance).coerceAtLeast(0)), style = Pulse.type.titleSmall, color = colors.textPrimary)
                Text(stringResource(Res.string.points_short_body, cost, balance), style = Pulse.type.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(dimens.md))
        if (todayReward != null) {
            PrimaryButton(
                stringResource(Res.string.points_claim_today, todayReward),
                onClick = onClaimDaily,
                leading = Glyphs.Gift,
                brush = Brush.horizontalGradient(listOf(colors.goldDeep, colors.gold, colors.goldLight)),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimens.sm))
        }
        val busy = adStatus == AdStatus.SHOWING || adStatus == AdStatus.VERIFYING
        when {
            !adsAvailable -> Text(stringResource(Res.string.points_ads_unavailable), style = Pulse.type.meta, color = colors.textMuted)
            adsLeft <= 0 -> Text(stringResource(Res.string.points_ads_used_up), style = Pulse.type.meta, color = colors.textMuted)
            else -> {
                SecondaryButton(
                    stringResource(Res.string.points_watch_ad, adReward),
                    onClick = onWatchAd,
                    enabled = !busy,
                    leading = Glyphs.Play,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(dimens.xs))
                Text(stringResource(Res.string.points_ads_left, adsLeft), style = Pulse.type.meta, color = colors.textMuted)
            }
        }
        adStatusText(adStatus, adCredited)?.let { (text, positive) ->
            Spacer(Modifier.height(dimens.sm))
            Text(
                text,
                style = Pulse.type.bodySmall,
                color = if (positive) colors.success else colors.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(dimens.sm))
        Text(
            stringResource(Res.string.points_close),
            style = Pulse.type.label,
            color = colors.textMuted,
            modifier = Modifier.align(Alignment.End).pressable(onClick = onDismiss).padding(horizontal = dimens.md, vertical = dimens.sm),
        )
    }
}

@Composable
private fun adStatusText(status: AdStatus, credited: Int): Pair<String, Boolean>? = when (status) {
    AdStatus.IDLE -> null
    AdStatus.SHOWING -> stringResource(Res.string.points_ad_showing) to false
    AdStatus.VERIFYING -> stringResource(Res.string.points_ad_verifying) to false
    AdStatus.CREDITED -> stringResource(Res.string.points_ad_credited, credited) to true
    AdStatus.PENDING -> stringResource(Res.string.points_ad_pending) to false
    AdStatus.DISMISSED -> stringResource(Res.string.points_ad_dismissed) to false
    AdStatus.FAILED -> stringResource(Res.string.points_ad_failed) to false
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ComponentPreviews
@Composable
private fun PointsBalancePillPreview() = PreviewContainer(Modifier.padding(Pulse.dimens.lg)) {
    PointsBalancePill(1240)
}

@ComponentPreviews
@Composable
private fun PointsShortPanelPreview() = PreviewContainer(Modifier.padding(Pulse.dimens.lg)) {
    PointsShortPanel(
        cost = 50, balance = 20, adReward = 25, adsLeft = 4, adsAvailable = true, todayReward = 30,
        adStatus = AdStatus.VERIFYING, adCredited = 0, onWatchAd = {}, onClaimDaily = {}, onDismiss = {},
    )
}

@ComponentPreviews
@Composable
private fun PointsShortPanelNoAdsPreview() = PreviewContainer(Modifier.padding(Pulse.dimens.lg)) {
    PointsShortPanel(
        cost = 50, balance = 20, adReward = 25, adsLeft = 4, adsAvailable = false, todayReward = null,
        adStatus = AdStatus.IDLE, adCredited = 0, onWatchAd = {}, onClaimDaily = {}, onDismiss = {},
    )
}
