package org.mayad.fame_life

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.app.Gate
import org.mayad.fame_life.app.RootIntent
import org.mayad.fame_life.app.RootViewModel
import org.mayad.fame_life.app.navigation.AppNavHost
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseTheme
import org.mayad.fame_life.feature.onboarding.BrandSplash
import org.mayad.fame_life.feature.onboarding.OnboardingScreen
import org.mayad.fame_life.feature.onboarding.ProfileSetupScreen
import org.mayad.fame_life.feature.onboarding.SyncErrorScreen

/** The app root: theme, then the surface the [RootViewModel] chooses. Koin is started by the platform host. */
@Composable
fun App() {
    PulseTheme {
        val root: RootViewModel = koinViewModel()
        val state by root.state.collectAsStateWithLifecycle()
        AnimatedContent(
            targetState = state.gate,
            transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.standard()) },
            modifier = Modifier.fillMaxSize().background(Pulse.colors.background),
            label = "gate",
        ) { gate ->
            val fill = Modifier.fillMaxSize().background(Pulse.colors.background)
            when (gate) {
                Gate.SPLASH -> BrandSplash(fill)
                Gate.WELCOME -> OnboardingScreen(fill)
                Gate.SETUP -> ProfileSetupScreen(fill)
                Gate.SYNC_ERROR -> SyncErrorScreen(
                    onRetry = { root.onIntent(RootIntent.Retry) },
                    onSignOut = { root.onIntent(RootIntent.SignOut) },
                    modifier = fill,
                )
                Gate.MAIN -> AppNavHost()
            }
        }
    }
}
