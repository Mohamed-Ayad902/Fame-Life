package org.mayad.fame_life.feature.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.UnavailableAuthService

enum class IntroStep { WELCOME, HOW_IT_WORKS, HONESTY, ACCOUNT }

@Immutable
data class OnboardingUiState(
    val step: IntroStep = IntroStep.WELCOME,
    /** True when the user said they already have an account; the account step greets them back. */
    val returning: Boolean = false,
    val signingIn: Boolean = false,
    /** Why the last sign-in failed; null when it succeeded, was cancelled, or none was tried. */
    val error: AuthErrorKind? = null,
    /** False on builds without Firebase configuration; the account step explains and disables the button. */
    val accountsAvailable: Boolean = true,
)

sealed interface OnboardingIntent {
    data object GetStarted : OnboardingIntent
    data object HaveAccount : OnboardingIntent
    data object Next : OnboardingIntent
    data object Back : OnboardingIntent
    data object SignInWithGoogle : OnboardingIntent
}

/**
 * The signed-out intro: welcome → how a live works → what's simulated → Google sign-in. A successful sign-in needs
 * no event: the root gate sees the new auth state and moves on.
 */
class OnboardingViewModel(
    private val auth: AuthService,
    private val analytics: Analytics,
) : MviViewModel<OnboardingUiState, OnboardingIntent, NoEvent>(
    OnboardingUiState(accountsAvailable = auth !is UnavailableAuthService),
) {
    init {
        analytics.log(AnalyticsEvent.OnboardingStarted)
    }

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.GetStarted -> setState { copy(step = IntroStep.HOW_IT_WORKS, returning = false) }
            OnboardingIntent.HaveAccount -> setState { copy(step = IntroStep.ACCOUNT, returning = true) }
            OnboardingIntent.Next -> setState { copy(step = IntroStep.entries[(step.ordinal + 1).coerceAtMost(IntroStep.entries.lastIndex)]) }
            OnboardingIntent.Back -> setState { copy(step = IntroStep.entries[(step.ordinal - 1).coerceAtLeast(0)], error = null) }
            OnboardingIntent.SignInWithGoogle -> signIn()
        }
    }

    private fun signIn() {
        if (currentState.signingIn) return
        setState { copy(signingIn = true, error = null) }
        analytics.log(AnalyticsEvent.SignupStarted)
        viewModelScope.launch {
            try {
                auth.signInWithGoogle()
                analytics.log(AnalyticsEvent.SignInCompleted)
                setState { copy(signingIn = false) }
            } catch (e: AuthException) {
                if (e.kind != AuthErrorKind.CANCELLED) analytics.log(AnalyticsEvent.AuthFailed(e.kind.name.lowercase()))
                setState { copy(signingIn = false, error = e.kind.takeIf { it != AuthErrorKind.CANCELLED }) }
            }
        }
    }
}
