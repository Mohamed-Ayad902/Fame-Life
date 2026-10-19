package org.mayad.fame_life.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_sign_out
import famelife.shared.generated.resources.profile_edit
import famelife.shared.generated.resources.settings_camera
import famelife.shared.generated.resources.settings_camera_detail
import famelife.shared.generated.resources.settings_data
import famelife.shared.generated.resources.settings_data_body
import famelife.shared.generated.resources.settings_data_detail
import famelife.shared.generated.resources.settings_delete
import famelife.shared.generated.resources.settings_delete_detail
import famelife.shared.generated.resources.settings_edit_detail
import famelife.shared.generated.resources.settings_fonts
import famelife.shared.generated.resources.settings_group_about
import famelife.shared.generated.resources.settings_group_account
import famelife.shared.generated.resources.settings_group_live
import famelife.shared.generated.resources.settings_group_privacy
import famelife.shared.generated.resources.settings_motion
import famelife.shared.generated.resources.settings_motion_full
import famelife.shared.generated.resources.settings_motion_reduced
import famelife.shared.generated.resources.settings_offline
import famelife.shared.generated.resources.settings_plan
import famelife.shared.generated.resources.settings_plan_free
import famelife.shared.generated.resources.settings_plan_pro
import famelife.shared.generated.resources.settings_saved_to_account
import famelife.shared.generated.resources.settings_sign_out_detail
import famelife.shared.generated.resources.settings_signed_in
import famelife.shared.generated.resources.settings_simulated
import famelife.shared.generated.resources.settings_simulated_body
import famelife.shared.generated.resources.settings_simulated_detail
import famelife.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.ListRow
import org.mayad.fame_life.core.designsystem.components.ScreenHeader
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.entitlement.Plan


private const val SECONDS_PER_MINUTE = 60
private const val PANEL_ALPHA = 0.85f

/** Tier 2: stateless settings. Wide windows place the groups in two columns. */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
    onToggleSimulation: () -> Unit,
    onToggleData: () -> Unit,
) {
    val dimens = Pulse.dimens
    val showSimulation = state.showSimulation
    val showData = state.showData
    val cascade = rememberCascadeState()
    val account: @Composable ColumnScope.() -> Unit = {
        SettingsGroup(stringResource(Res.string.settings_group_account), Modifier.cascadeIn(cascade, "account")) {
            ListRow(
                stringResource(Res.string.profile_edit),
                detail = stringResource(Res.string.settings_edit_detail),
                leading = { RowIcon(Glyphs.User, Gloss.Blue) },
                trailing = { Chevron() },
                onClick = onEdit,
            )
            ListRow(
                stringResource(Res.string.settings_plan),
                detail = stringResource(if (state.plan == Plan.PRO) Res.string.settings_plan_pro else Res.string.settings_plan_free),
                leading = { RowIcon(Glyphs.Crown, Gloss.Gold) },
                trailing = { Chevron() },
                onClick = onPlan,
            )
            ListRow(
                state.email ?: stringResource(Res.string.settings_signed_in),
                detail = stringResource(if (state.offline) Res.string.settings_offline else Res.string.settings_saved_to_account),
                leading = { RowIcon(Glyphs.Shield, Gloss.Blue) },
            )
            ListRow(
                stringResource(Res.string.common_sign_out),
                detail = stringResource(Res.string.settings_sign_out_detail),
                leading = { RowIcon(Glyphs.Logout, Gloss.Slate) },
                onClick = onSignOut,
            )
        }
        SettingsGroup(stringResource(Res.string.settings_group_live), Modifier.cascadeIn(cascade, "live")) {
            ListRow(
                stringResource(Res.string.settings_camera),
                detail = stringResource(Res.string.settings_camera_detail),
                leading = { RowIcon(Glyphs.Mic, Gloss.Violet) },
            )
            ListRow(
                stringResource(Res.string.settings_motion),
                detail = stringResource(if (Pulse.reducedMotion) Res.string.settings_motion_reduced else Res.string.settings_motion_full),
                leading = { RowIcon(Glyphs.Sparkles, Gloss.Cyan) },
            )
        }
    }
    val privacy: @Composable ColumnScope.() -> Unit = {
        SettingsGroup(stringResource(Res.string.settings_group_privacy), Modifier.cascadeIn(cascade, "privacy")) {
            ListRow(
                stringResource(Res.string.settings_simulated),
                detail = stringResource(Res.string.settings_simulated_detail),
                leading = { RowIcon(Glyphs.Info, Gloss.Cyan) },
                trailing = { Chevron(expanded = showSimulation) },
                onClick = onToggleSimulation,
            )
            AnimatedVisibility(showSimulation) { Explainer(stringResource(Res.string.settings_simulated_body, AppConfig.APP_NAME)) }
            ListRow(
                stringResource(Res.string.settings_data),
                detail = stringResource(Res.string.settings_data_detail),
                leading = { RowIcon(Glyphs.Shield, Gloss.Mint) },
                trailing = { Chevron(expanded = showData) },
                onClick = onToggleData,
            )
            AnimatedVisibility(showData) { Explainer(stringResource(Res.string.settings_data_body)) }
            ListRow(
                stringResource(Res.string.settings_delete),
                detail = stringResource(Res.string.settings_delete_detail),
                leading = { RowIcon(Glyphs.Trash, Gloss.Rose) },
                onClick = onDelete,
            )
        }
        SettingsGroup(stringResource(Res.string.settings_group_about), Modifier.cascadeIn(cascade, "about")) {
            ListRow(AppConfig.APP_NAME, detail = stringResource(Res.string.settings_fonts), leading = { RowIcon(Glyphs.Star, Gloss.Violet) })
        }
    }
    AtmosphereBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader(stringResource(Res.string.settings_title), onBack = onBack)
            if (Pulse.width.split) {
                Row(Modifier.fillMaxSize().padding(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.xl)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = account)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = privacy)
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = dimens.gutter)) {
                    account()
                    privacy()
                    Spacer(Modifier.height(dimens.xxl))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun RowIcon(glyph: ImageVector, gloss: Gloss) = GlossyIcon(glyph, gloss, size = Pulse.dimens.tileSm)

@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(modifier) {
        Spacer(Modifier.height(dimens.lg))
        Text(title.uppercase(), style = Pulse.type.overline, color = colors.textMuted)
        Spacer(Modifier.height(dimens.sm))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(Pulse.shapes.panel)
                .background(colors.surface.copy(alpha = PANEL_ALPHA))
                .border(dimens.hairline, colors.border, Pulse.shapes.panel)
                .padding(horizontal = dimens.lg, vertical = dimens.xs),
            content = content,
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean? = null) {
    val glyph = if (expanded == true) Glyphs.Close else Glyphs.ChevronRight
    PulseIcon(glyph, Pulse.colors.textMuted, size = Pulse.dimens.iconSmall)
}

@Composable
private fun Explainer(text: String) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.textSecondary,
        // Aligns with the row titles, past the leading icon.
        modifier = Modifier.padding(start = Pulse.dimens.tileSm + Pulse.dimens.md, bottom = Pulse.dimens.md),
    )
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun SettingsPreview() = PreviewContainer {
    SettingsContent(SettingsUiState("maya@example.com", showData = true), {}, {}, {}, {}, {}, {}, {})
}

@ComponentPreviews
@Composable
private fun DeleteErrorPreview() = PreviewContainer {
    DeleteAccountDialog(busy = false, failure = AuthErrorKind.NETWORK.messageRes, onConfirm = {}, onDismiss = {})
}
