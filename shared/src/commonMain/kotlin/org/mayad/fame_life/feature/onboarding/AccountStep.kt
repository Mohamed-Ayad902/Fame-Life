package org.mayad.fame_life.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthRules
import org.mayad.fame_life.domain.account.UnavailableAuthService

/**
 * Sign in with Google. A first sign-in creates the account. Success changes the auth state and the app root moves on
 * by itself (to profile setup for a new account, or straight home for one that already has a profile).
 */
@Composable
internal fun AccountStep(step: Int, total: Int, returning: Boolean) {
    val container = LocalAppContainer.current
    val colors = Pulse.colors
    val scope = rememberCoroutineScope()
    val configured = container.auth !is UnavailableAuthService
    var busy by rememberSaveable { mutableStateOf(false) }
    var failure by rememberSaveable { mutableStateOf<String?>(null) }

    fun signIn() {
        if (busy) return
        busy = true
        failure = null
        container.analytics.log(AnalyticsEvent.SignupStarted)
        scope.launch {
            try {
                container.auth.signInWithGoogle()
                container.analytics.log(AnalyticsEvent.SignInCompleted)
            } catch (e: AuthException) {
                if (e.kind != AuthErrorKind.CANCELLED) container.analytics.log(AnalyticsEvent.AuthFailed(e.kind.name.lowercase()))
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
            .verticalScroll(rememberScrollState())
            .padding(Pulse.dimens.gutter),
    ) {
        StepDots(step, total)
        Spacer(Modifier.height(Pulse.dimens.xl))
        GlossyIcon(Glyphs.Shield, Gloss.Blue, size = 64.dp)
        Spacer(Modifier.height(Pulse.dimens.lg))
        Text(if (returning) "Welcome back" else "Save your creator life", style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(
            if (returning) {
                "Sign in with the Google account you used before to pick up your profile, lives and fame level."
            } else {
                "Sign in with Google to keep your profile and lives safe across devices. A new account is created the first time."
            },
            style = Pulse.type.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(Pulse.dimens.xl))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(Pulse.shapes.card)
                .background(colors.surface.copy(alpha = 0.72f))
                .border(1.dp, colors.border, Pulse.shapes.card)
                .padding(Pulse.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Pulse.dimens.sm),
        ) {
            Text("What we use", style = Pulse.type.titleSmall, color = colors.textPrimary)
            Text(
                "Your Google name, email and account id, only to sign you in and keep your data yours. " +
                    "We never post to Google or anywhere else for you.",
                style = Pulse.type.bodySmall,
                color = colors.textSecondary,
            )
        }
        val message = failure ?: if (!configured) AuthRules.message(AuthErrorKind.NOT_CONFIGURED) else null
        if (message != null) {
            Spacer(Modifier.height(Pulse.dimens.lg))
            Text(
                message,
                style = Pulse.type.bodySmall,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Pulse.dimens.xxl))
        PrimaryButton(
            if (busy) "Opening Google…" else "Continue with Google",
            onClick = ::signIn,
            enabled = configured && !busy,
            leading = Glyphs.User,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Pulse.dimens.sm))
    }
}

/** Signed in, but the account's data could not be fetched and nothing is cached yet. */
@Composable
internal fun SyncErrorScreen(onRetry: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    AtmosphereBackground(modifier) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlossyIcon(Glyphs.Globe, Gloss.Slate, size = 84.dp)
            Spacer(Modifier.height(Pulse.dimens.lg))
            Text("Can't reach your account", style = Pulse.type.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Pulse.dimens.sm))
            Text(
                "You're signed in, but your profile couldn't load. Check your connection and try again.",
                style = Pulse.type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Pulse.dimens.xl))
            PrimaryButton("Try again", onRetry, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Pulse.dimens.sm))
            SecondaryButton("Sign out", onSignOut, Modifier.fillMaxWidth())
        }
    }
}
