package org.mayad.fame_life.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_keep
import famelife.shared.generated.resources.common_sign_out
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
import famelife.shared.generated.resources.profile_edit
import famelife.shared.generated.resources.settings_camera
import famelife.shared.generated.resources.settings_camera_detail
import famelife.shared.generated.resources.settings_confirm_google
import famelife.shared.generated.resources.settings_data
import famelife.shared.generated.resources.settings_data_body
import famelife.shared.generated.resources.settings_data_detail
import famelife.shared.generated.resources.settings_delete
import famelife.shared.generated.resources.settings_delete_body
import famelife.shared.generated.resources.settings_delete_detail
import famelife.shared.generated.resources.settings_delete_title
import famelife.shared.generated.resources.settings_deleting
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
import famelife.shared.generated.resources.settings_sign_out_body
import famelife.shared.generated.resources.settings_sign_out_detail
import famelife.shared.generated.resources.settings_sign_out_title
import famelife.shared.generated.resources.settings_signed_in
import famelife.shared.generated.resources.settings_simulated
import famelife.shared.generated.resources.settings_simulated_body
import famelife.shared.generated.resources.settings_simulated_detail
import famelife.shared.generated.resources.settings_stay
import famelife.shared.generated.resources.settings_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.ListRow
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
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.simulation.GiftTier
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe

private const val SECONDS_PER_MINUTE = 60
private const val PANEL_ALPHA = 0.85f

@Immutable
data class SettingsState(val email: String?, val offline: Boolean, val plan: Plan, val reducedMotion: Boolean)

/** Tier 1: account state, sign-out and deletion dialogs, navigation. */
@Composable
fun SettingsScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val entitlements by container.entitlements.entitlements.collectAsState()
    val auth by container.auth.state.collectAsState()
    val sync by container.account.state.collectAsState()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsContent(
        state = SettingsState(
            email = (auth as? AuthState.SignedIn)?.user?.email,
            offline = (sync as? SyncState.Ready)?.offline == true,
            plan = entitlements.plan,
            reducedMotion = Pulse.reducedMotion,
        ),
        onBack = { navigator.pop() },
        onEdit = { navigator.push(Route.EditProfile) },
        onPlan = { navigator.push(Route.Paywall) },
        onSignOut = { confirmSignOut = true },
        onDelete = { confirmDelete = true },
    )

    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_sign_out_title),
            body = stringResource(Res.string.settings_sign_out_body),
            confirm = stringResource(Res.string.common_sign_out),
            dismiss = stringResource(Res.string.settings_stay),
            onConfirm = {
                confirmSignOut = false
                scope.launch {
                    // The app root returns to the welcome screens once the auth state changes.
                    runCatching { container.account.signOut() }.onSuccess { container.analytics.log(AnalyticsEvent.SignedOut) }
                }
            },
            onDismiss = { confirmSignOut = false },
        )
    }
    if (confirmDelete) DeleteAccountDialog(onDismiss = { confirmDelete = false })
}

/** Tier 2: stateless settings. Wide windows place the groups in two columns. */
@Composable
fun SettingsContent(
    state: SettingsState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
) {
    val dimens = Pulse.dimens
    var showSimulation by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }
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
                detail = stringResource(if (state.reducedMotion) Res.string.settings_motion_reduced else Res.string.settings_motion_full),
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
                onClick = { showSimulation = !showSimulation },
            )
            AnimatedVisibility(showSimulation) { Explainer(stringResource(Res.string.settings_simulated_body, AppConfig.APP_NAME)) }
            ListRow(
                stringResource(Res.string.settings_data),
                detail = stringResource(Res.string.settings_data_detail),
                leading = { RowIcon(Glyphs.Shield, Gloss.Mint) },
                trailing = { Chevron(expanded = showData) },
                onClick = { showData = !showData },
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
private fun ConfirmDialog(title: String, body: String, confirm: String, dismiss: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = Pulse.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        title = { Text(title, style = Pulse.type.title, color = colors.textPrimary) },
        text = { Text(body, style = Pulse.type.body, color = colors.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, style = Pulse.type.label, color = colors.textPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss, style = Pulse.type.label, color = colors.textSecondary) } },
    )
}

/** Account deletion re-confirms the Google account: Firebase only deletes accounts with a recent sign-in. */
@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val colors = Pulse.colors
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<StringResource?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = colors.surfaceElevated,
        title = { Text(stringResource(Res.string.settings_delete_title), style = Pulse.type.title, color = colors.textPrimary) },
        text = {
            Column {
                Text(stringResource(Res.string.settings_delete_body), style = Pulse.type.body, color = colors.textSecondary)
                failure?.let {
                    Spacer(Modifier.height(Pulse.dimens.md))
                    Text(stringResource(it), style = Pulse.type.bodySmall, color = colors.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    failure = null
                    scope.launch {
                        try {
                            // On success the auth state flips to signed out and the app returns to the welcome screens.
                            container.deleteAccount()
                        } catch (e: AuthException) {
                            failure = e.kind.messageRes
                            busy = false
                        }
                    }
                },
            ) { Text(stringResource(if (busy) Res.string.settings_deleting else Res.string.settings_confirm_google), style = Pulse.type.label, color = colors.error) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(Res.string.common_keep), style = Pulse.type.label, color = colors.textPrimary) }
        },
    )
}

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
// Paywall

/** Tier 1: logs the view. Billing isn't wired, so the screen is informational and the buy button is disabled. */
@Composable
fun PaywallScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.PaywallViewed) }
    PaywallContent(PlanLimits.Free, PlanLimits.Pro, onBack = { navigator.pop() })
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
private fun SettingsPreview() = PreviewContainer {
    SettingsContent(SettingsState("maya@example.com", offline = false, plan = Plan.FREE, reducedMotion = false), {}, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun PaywallPreview() = PreviewContainer { PaywallContent(PlanLimits.Free, PlanLimits.Pro, onBack = {}) }

@ComponentPreviews
@Composable
private fun DeleteErrorPreview() = PreviewContainer {
    Text(stringResource(AuthErrorKind.NETWORK.messageRes!!), style = Pulse.type.bodySmall, color = Pulse.colors.error, modifier = Modifier.padding(Pulse.dimens.gutter))
}
