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
import kotlinx.coroutines.launch
import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthRules
import org.mayad.fame_life.domain.account.AuthState
import androidx.compose.ui.unit.dp
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
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.entitlement.PlanLimits

@Composable
fun SettingsScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val entitlements by container.entitlements.entitlements.collectAsState()
    val reduceMotion = Pulse.reducedMotion
    var showSimulation by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val auth by container.auth.state.collectAsState()
    val sync by container.account.state.collectAsState()
    val email = (auth as? AuthState.SignedIn)?.user?.email
    val scope = rememberCoroutineScope()
    val colors = Pulse.colors

    AtmosphereBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Settings", onBack = { navigator.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Pulse.dimens.gutter)) {
                SettingsGroup("Account") {
                    ListRow(
                        "Edit profile",
                        detail = "Name, username, bio and avatar",
                        leading = { GlossyIcon(Glyphs.User, Gloss.Blue, size = 36.dp) },
                        trailing = { Chevron() },
                        onClick = { navigator.push(Route.EditProfile) },
                    )
                    ListRow(
                        "Plan",
                        detail = if (entitlements.plan == Plan.PRO) "Pro" else "Free · see what Pro adds",
                        leading = { GlossyIcon(Glyphs.Crown, Gloss.Gold, size = 36.dp) },
                        trailing = { Chevron() },
                        onClick = { navigator.push(Route.Paywall) },
                    )
                    ListRow(
                        email ?: "Signed in",
                        detail = if ((sync as? SyncState.Ready)?.offline == true) "Offline · changes sync when you reconnect" else "Profile and lives are saved to your account",
                        leading = { GlossyIcon(Glyphs.Shield, Gloss.Blue, size = 36.dp) },
                    )
                    ListRow(
                        "Sign out",
                        detail = "Your data stays in your account",
                        leading = { GlossyIcon(Glyphs.Logout, Gloss.Slate, size = 36.dp) },
                        onClick = { confirmSignOut = true },
                    )
                }
                SettingsGroup("Live") {
                    ListRow(
                        "Camera and microphone",
                        detail = "Asked only when you go live. Video stays on your phone and is never uploaded.",
                        leading = { GlossyIcon(Glyphs.Mic, Gloss.Violet, size = 36.dp) },
                    )
                    ListRow(
                        "Motion",
                        detail = if (reduceMotion) "Reduced, following your system setting" else "Full. Turn on Reduce motion in system settings to calm it down.",
                        leading = { GlossyIcon(Glyphs.Sparkles, Gloss.Cyan, size = 36.dp) },
                    )
                }
                SettingsGroup("Privacy") {
                    ListRow(
                        "What's simulated",
                        detail = "Viewers, comments, gifts and followers",
                        leading = { GlossyIcon(Glyphs.Info, Gloss.Cyan, size = 36.dp) },
                        trailing = { Chevron(expanded = showSimulation) },
                        onClick = { showSimulation = !showSimulation },
                    )
                    AnimatedVisibility(showSimulation) {
                        Explainer(
                            "Everything about your audience is generated by ${AppConfig.APP_NAME}. No real people watch your lives, " +
                                "and viewer counts, comments, gifts and followers are not real engagement. Creators in the feed are fictional.",
                        )
                    }
                    ListRow(
                        "What leaves your device",
                        detail = "Speech text, never video or audio",
                        leading = { GlossyIcon(Glyphs.Shield, Gloss.Mint, size = 36.dp) },
                        trailing = { Chevron(expanded = showData) },
                        onClick = { showData = !showData },
                    )
                    AnimatedVisibility(showData) {
                        Explainer(
                            "Camera frames are shown only on your screen. Speech is turned into text by your phone's recognizer; " +
                                "raw audio is not stored. Your profile, live summaries and inbox are saved to your account so they " +
                                "follow you between devices; transcripts are not. When the cloud audience is connected, short pieces " +
                                "of speech text are sent to our server to write comments. Today the audience runs fully on your device.",
                        )
                    }
                    ListRow(
                        "Delete account",
                        detail = "Your account, profile, lives and inbox, everywhere",
                        leading = { GlossyIcon(Glyphs.Trash, Gloss.Rose, size = 36.dp) },
                        onClick = { confirmDelete = true },
                    )
                }
                SettingsGroup("About") {
                    ListRow(
                        AppConfig.APP_NAME,
                        detail = "Fonts: Sora and Manrope, SIL Open Font License 1.1",
                        leading = { GlossyIcon(Glyphs.Star, Gloss.Violet, size = 36.dp) },
                    )
                }
                Spacer(Modifier.height(Pulse.dimens.xxl))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Sign out?", style = Pulse.type.title, color = colors.textPrimary) },
            text = { Text("Your profile and lives stay in your account. Sign in again to pick them up.", style = Pulse.type.body, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    scope.launch {
                        // The app root returns to the welcome screens once the auth state changes.
                        runCatching { container.account.signOut() }.onSuccess { container.analytics.log(AnalyticsEvent.SignedOut) }
                    }
                }) { Text("Sign out", style = Pulse.type.label, color = colors.textPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Stay", style = Pulse.type.label, color = colors.textSecondary) }
            },
        )
    }

    if (confirmDelete) DeleteAccountDialog(onDismiss = { confirmDelete = false })
}

/** Account deletion re-confirms the Google account: Firebase only deletes accounts with a recent sign-in. */
@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val colors = Pulse.colors
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = colors.surfaceElevated,
        title = { Text("Delete your account?", style = Pulse.type.title, color = colors.textPrimary) },
        text = {
            Column {
                Text(
                    "This permanently removes your account, profile, live history and inbox from our servers and this device. " +
                        "It can't be undone. You'll pick your Google account once more to confirm.",
                    style = Pulse.type.body,
                    color = colors.textSecondary,
                )
                if (failure != null) {
                    Spacer(Modifier.height(Pulse.dimens.md))
                    Text(failure!!, style = Pulse.type.bodySmall, color = colors.error)
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
                            failure = AuthRules.message(e.kind)
                            busy = false
                        }
                    }
                },
            ) { Text(if (busy) "Deleting…" else "Confirm with Google", style = Pulse.type.label, color = colors.error) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Keep", style = Pulse.type.label, color = colors.textPrimary) }
        },
    )
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = Pulse.colors
    Spacer(Modifier.height(Pulse.dimens.lg))
    Text(title.uppercase(), style = Pulse.type.overline, color = colors.textMuted)
    Spacer(Modifier.height(Pulse.dimens.sm))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface.copy(alpha = 0.85f))
            .border(1.dp, colors.border, Pulse.shapes.panel)
            .padding(horizontal = Pulse.dimens.lg, vertical = Pulse.dimens.xs),
        content = content,
    )
}

@Composable
private fun Chevron(expanded: Boolean? = null) {
    val glyph = if (expanded == true) Glyphs.Close else Glyphs.ChevronRight
    PulseIcon(glyph, Pulse.colors.textMuted, size = 18.dp)
}

@Composable
private fun Explainer(text: String) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.textSecondary,
        modifier = Modifier.padding(start = 48.dp, bottom = Pulse.dimens.md),
    )
}

@Composable
fun PaywallScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.PaywallViewed) }
    val colors = Pulse.colors
    val free = PlanLimits.Free
    val pro = PlanLimits.Pro

    AtmosphereBackground(accent = colors.gold) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("", onBack = { navigator.pop() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Pulse.dimens.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlossyIcon(Glyphs.Crown, Gloss.Gold, size = 96.dp, glyphSize = 46.dp)
                Spacer(Modifier.height(Pulse.dimens.lg))
                Text("${AppConfig.APP_NAME} Pro", style = Pulse.type.display, color = colors.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Pulse.dimens.xs))
                Text(
                    "Bigger crowds, longer lives and every audience mood.",
                    style = Pulse.type.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Pulse.dimens.xl))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(Pulse.shapes.panel)
                        .background(colors.surface.copy(alpha = 0.9f))
                        .border(1.dp, colors.border, Pulse.shapes.panel)
                        .padding(Pulse.dimens.lg),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1.4f))
                        Text("Free", style = Pulse.type.label, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Pro", style = Pulse.type.label, color = colors.gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(Pulse.dimens.sm))
                    CompareRow("Live length", "${free.maxSessionSeconds / 60} min", "${pro.maxSessionSeconds / 60} min")
                    CompareRow("Lives per day", "${free.sessionsPerDay}", "${pro.sessionsPerDay}")
                    CompareRow("Audience sizes", "${free.presets.size} of 4", "All 4")
                    CompareRow("Audience moods", "${free.vibes.size} of 5", "All 5")
                    CompareRow("Gift tiers", "${free.giftTiers.size} of 3", "All 3")
                    CompareRow("Critic mode", null, "Included")
                    CompareRow("Live styles", "${free.liveStyles.size}", "${pro.liveStyles.size}")
                    CompareRow("Saved lives", "${free.historySize}", "${pro.historySize}")
                }
                Spacer(Modifier.height(Pulse.dimens.xl))
                PrimaryButton("Pro isn't available yet", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), brush = colors.goldGradient)
                Spacer(Modifier.height(Pulse.dimens.sm))
                Text(
                    "Store billing isn't set up in this build, so nothing can be bought and no price is shown. You're on Free.",
                    style = Pulse.type.meta,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Pulse.dimens.lg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimulatedTag(text = "Gifts are simulated")
                    Spacer(Modifier.width(Pulse.dimens.sm))
                    Text("They're never bought or paid out.", style = Pulse.type.meta, color = colors.textMuted)
                }
                Spacer(Modifier.height(Pulse.dimens.xxl))
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, free: String?, pro: String) {
    val colors = Pulse.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Pulse.type.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1.4f))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (free == null) CellGlyph(Glyphs.Lock, "Not included") else Text(free, style = Pulse.type.bodySmall, color = colors.textSecondary)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(pro, style = Pulse.type.label, color = colors.textPrimary)
        }
    }
}

@Composable
private fun CellGlyph(glyph: ImageVector, description: String) {
    PulseIcon(glyph, Pulse.colors.textMuted, size = 16.dp, contentDescription = description)
}
