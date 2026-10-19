package org.mayad.fame_life.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.domain.account.AuthErrorKind

/**
 * Signed out: welcome → how a live works → what's simulated → Google sign-in. Signing in ends this screen from
 * the outside (the root gate switches), so it has no completion callback.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reduceMotion = Pulse.reducedMotion
    val total = IntroStep.entries.size
    BackHandler(enabled = state.step != IntroStep.WELCOME) { viewModel.onIntent(OnboardingIntent.Back) }
    AtmosphereBackground(modifier) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { stepTransition(forward = targetState.ordinal > initialState.ordinal, reduceMotion) },
            label = "onboarding",
        ) { step ->
            when (step) {
                IntroStep.WELCOME -> WelcomeContent(
                    onContinue = { viewModel.onIntent(OnboardingIntent.GetStarted) },
                    onSignIn = { viewModel.onIntent(OnboardingIntent.HaveAccount) },
                )
                IntroStep.HOW_IT_WORKS -> HowItWorksStep(step.ordinal, total) { viewModel.onIntent(OnboardingIntent.Next) }
                IntroStep.HONESTY -> HonestyStep(step.ordinal, total) { viewModel.onIntent(OnboardingIntent.Next) }
                IntroStep.ACCOUNT -> AccountStepContent(
                    step = step.ordinal,
                    total = total,
                    returning = state.returning,
                    busy = state.signingIn,
                    message = (state.error ?: AuthErrorKind.NOT_CONFIGURED.takeIf { !state.accountsAvailable })?.messageRes,
                    enabled = state.accountsAvailable,
                    onContinue = { viewModel.onIntent(OnboardingIntent.SignInWithGoogle) },
                )
            }
        }
    }
}

/** After a new account is created. Saving the profile ends this screen from the outside (the root gate switches). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfileSetupScreen(modifier: Modifier = Modifier, viewModel: ProfileSetupViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reduceMotion = Pulse.reducedMotion
    BackHandler(enabled = state.step == SetupStep.INTERESTS) { viewModel.onIntent(ProfileSetupIntent.Back) }
    AtmosphereBackground(modifier) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { stepTransition(forward = targetState.ordinal > initialState.ordinal, reduceMotion) },
            label = "profile-setup",
        ) { step ->
            when (step) {
                SetupStep.PROFILE -> ProfileStepContent(state, viewModel::onIntent)
                SetupStep.INTERESTS -> InterestsStepContent(state, viewModel::onIntent)
            }
        }
    }
}

/** Forward steps slide in from the trailing edge; going back reverses it. Reduced motion only cross-fades. */
private fun stepTransition(forward: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(Motion.slide) { it * direction / 5 } + fadeIn(Motion.enter())) togetherWith fadeOut(Motion.exit())
}
