package org.mayad.fame_life.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing


import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Shapes
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthRules
import org.mayad.fame_life.domain.account.UnavailableAuthService

/**
 * Create an account or sign in with email and password. Success changes the auth state, and the app root moves on
 * by itself (to profile setup for a new account, or straight home for one that already has a profile).
 */
@Composable
internal fun AccountStep(step: Int, total: Int, startInSignIn: Boolean) {
    val container = LocalAppContainer.current
    val colors = Pulse.colors
    val scope = rememberCoroutineScope()
    val configured = container.auth !is UnavailableAuthService

    var signIn by rememberSaveable { mutableStateOf(startInSignIn) }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are deliberately not saveable: they should not land in saved instance state.
    var password by androidx.compose.runtime.remember { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var failure by rememberSaveable { mutableStateOf<String?>(null) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    val emailError = AuthRules.emailError(email)
    // Sign-in accepts any existing password; only new passwords get the strength rule.
    val passwordError = if (signIn) (if (password.isEmpty()) "Enter your password" else null) else AuthRules.passwordError(password)

    fun submit() {
        attempted = true
        failure = null
        notice = null
        if (emailError != null || passwordError != null || busy) return
        busy = true
        if (!signIn) container.analytics.log(AnalyticsEvent.SignupStarted)
        scope.launch {
            try {
                if (signIn) {
                    container.auth.signIn(email, password)
                    container.analytics.log(AnalyticsEvent.SignInCompleted)
                } else {
                    container.auth.signUp(email, password)
                    container.analytics.log(AnalyticsEvent.SignupCompleted)
                }
                password = ""
            } catch (e: AuthException) {
                container.analytics.log(AnalyticsEvent.AuthFailed(e.kind.name.lowercase()))
                failure = AuthRules.message(e.kind)
                if (e.kind == AuthErrorKind.EMAIL_IN_USE) signIn = true
            } finally {
                busy = false
            }
        }
    }

    fun resetPassword() {
        failure = null
        notice = null
        if (emailError != null) {
            attempted = true
            failure = "Enter your email above first, then tap reset."
            return
        }
        busy = true
        scope.launch {
            try {
                container.auth.sendPasswordReset(email)
                container.analytics.log(AnalyticsEvent.PasswordResetRequested)
                notice = "If an account exists for that email, a reset link is on its way."
            } catch (e: AuthException) {
                failure = AuthRules.message(e.kind)
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Space.screen),
    ) {
        StepDots(step, total)
        Spacer(Modifier.height(Space.xl))
        GlossyIcon(Glyphs.Shield, Gloss.Blue, size = 64.dp)
        Spacer(Modifier.height(Space.lg))
        Text(if (signIn) "Welcome back" else "Save your creator life", style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Space.sm))
        Text(
            if (signIn) "Sign in to pick up your profile, lives and fame level." else "An account keeps your profile and lives safe across devices. We never post anywhere for you.",
            style = Pulse.type.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(Space.xl))
        ModeSwitch(signIn = signIn, onChange = { signIn = it; failure = null; notice = null })
        Spacer(Modifier.height(Space.xl))
        PulseTextField(
            email,
            { email = it.trim() },
            "Email",
            placeholder = "you@example.com",
            error = if (attempted) emailError else null,
            keyboardType = KeyboardType.Email,
            maxLength = 120,
        )
        Spacer(Modifier.height(Space.lg))
        PulseTextField(
            password,
            { password = it },
            "Password",
            placeholder = if (signIn) "Your password" else "At least ${AuthRules.PASSWORD_MIN} characters",
            error = if (attempted) passwordError else null,
            keyboardType = KeyboardType.Password,
            secret = !showPassword,
            maxLength = 128,
            trailing = {
                Text(
                    if (showPassword) "Hide" else "Show",
                    style = Pulse.type.meta,
                    color = colors.cyan,
                    modifier = Modifier.pressable(role = Role.Button) { showPassword = !showPassword }.padding(Space.xs),
                )
            },
        )
        if (signIn) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "Forgot password?",
                style = Pulse.type.meta,
                color = colors.cyan,
                modifier = Modifier.pressable(enabled = configured && !busy, role = Role.Button, onClick = ::resetPassword).padding(vertical = Space.xs),
            )
        }
        val message = failure ?: notice ?: if (!configured) AuthRules.message(AuthErrorKind.NOT_CONFIGURED) else null
        if (message != null) {
            Spacer(Modifier.height(Space.lg))
            Text(
                message,
                style = Pulse.type.bodySmall,
                color = if (failure != null || !configured) colors.error else colors.success,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Space.xxl))
        PrimaryButton(
            when {
                busy -> if (signIn) "Signing in…" else "Creating account…"
                signIn -> "Sign in"
                else -> "Create account"
            },
            onClick = ::submit,
            enabled = configured && !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun ModeSwitch(signIn: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Pulse.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Shapes.pill)
            .background(colors.surface)
            .border(1.dp, colors.border, Shapes.pill)
            .padding(4.dp),
    ) {
        listOf(false to "Create account", true to "Sign in").forEach { (value, label) ->
            val selected = value == signIn
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(Shapes.pill)
                    .background(if (selected) colors.surfaceElevated else colors.surface)
                    .pressable(role = Role.Tab) { onChange(value) }
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Pulse.type.label, color = if (selected) colors.textPrimary else colors.textMuted)
            }
        }
    }
}

/** Signed in, but the account's data could not be fetched and nothing is cached yet. */
@Composable
internal fun SyncErrorScreen(onRetry: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    AtmosphereBackground(modifier) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Space.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlossyIcon(Glyphs.Globe, Gloss.Slate, size = 84.dp)
            Spacer(Modifier.height(Space.lg))
            Text("Can't reach your account", style = Pulse.type.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Space.sm))
            Text(
                "You're signed in, but your profile couldn't load. Check your connection and try again.",
                style = Pulse.type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xl))
            PrimaryButton("Try again", onRetry, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.sm))
            SecondaryButton("Sign out", onSignOut, Modifier.fillMaxWidth())
        }
    }
}
