package org.mayad.fame_life

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import org.mayad.fame_life.app.AppContainer
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.app.rememberNavigator
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseTheme
import org.mayad.fame_life.feature.live.LiveSessionScreen
import org.mayad.fame_life.feature.live.LiveSetupScreen
import org.mayad.fame_life.feature.live.LiveSummaryScreen
import org.mayad.fame_life.feature.onboarding.BRAND_SPLASH_MIN_MS
import org.mayad.fame_life.feature.onboarding.BrandSplash
import org.mayad.fame_life.feature.onboarding.OnboardingFlow
import org.mayad.fame_life.feature.onboarding.ProfileSetupFlow
import org.mayad.fame_life.feature.onboarding.SyncErrorScreen
import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.domain.account.AuthState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mayad.fame_life.feature.post.PostDetailScreen
import org.mayad.fame_life.feature.profile.CreatorProfileScreen
import org.mayad.fame_life.feature.profile.EditProfileScreen
import org.mayad.fame_life.feature.settings.PaywallScreen
import org.mayad.fame_life.feature.settings.SettingsScreen
import org.mayad.fame_life.feature.shell.MainShell

private enum class Gate { SPLASH, WELCOME, SETUP, SYNC_ERROR, MAIN }

@Composable
fun App() {
    val container = remember { AppContainer() }
    DisposableEffect(container) { onDispose { container.close() } }
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.AppOpened) }
    PulseTheme {
        CompositionLocalProvider(LocalAppContainer provides container) {
            val reduceMotion = Pulse.reducedMotion
            val auth by container.auth.state.collectAsState()
            val sync by container.account.state.collectAsState()
            val profile by container.profiles.profile.collectAsState()
            var splashShown by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(if (reduceMotion) BRAND_SPLASH_MIN_MS / 2 else BRAND_SPLASH_MIN_MS)
                splashShown = true
            }
            val gate = when {
                !splashShown || auth is AuthState.Checking -> Gate.SPLASH
                auth is AuthState.SignedOut -> Gate.WELCOME
                profile != null -> Gate.MAIN
                sync is SyncState.Failed -> Gate.SYNC_ERROR
                sync is SyncState.Ready -> Gate.SETUP
                else -> Gate.SPLASH // Signed in; still fetching this account's profile.
            }
            val scope = rememberCoroutineScope()
            AnimatedContent(
                targetState = gate,
                transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.standard()) },
                modifier = Modifier.fillMaxSize().background(Pulse.colors.background),
                label = "gate",
            ) { current ->
                val fill = Modifier.fillMaxSize().background(Pulse.colors.background)
                when (current) {
                    Gate.SPLASH -> BrandSplash(fill)
                    Gate.WELCOME -> OnboardingFlow(fill)
                    Gate.SETUP -> ProfileSetupFlow(fill)
                    Gate.SYNC_ERROR -> SyncErrorScreen(
                        onRetry = container.account::retry,
                        onSignOut = { scope.launch { runCatching { container.account.signOut() } } },
                        modifier = fill,
                    )
                    Gate.MAIN -> PulseNavHost(rememberNavigator())
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PulseNavHost(navigator: Navigator) {
    val saveable = rememberSaveableStateHolder()
    val reduceMotion = Pulse.reducedMotion
    BackHandler(enabled = navigator.canPop) { navigator.pop() }
    AnimatedContent(
        targetState = navigator.current,
        contentKey = { it.key },
        transitionSpec = { routeTransition(initialState, targetState, navigator.lastWasPop, reduceMotion) },
        modifier = Modifier.fillMaxSize().background(Pulse.colors.background),
        label = "nav",
    ) { route ->
        saveable.SaveableStateProvider(route.key) {
            when (route) {
                Route.Main -> MainShell(navigator)
                is Route.PostDetail -> PostDetailScreen(route.postId, navigator)
                is Route.CreatorProfile -> CreatorProfileScreen(route.creatorId, navigator)
                is Route.LiveSetup -> LiveSetupScreen(route.title, navigator)
                is Route.Live -> LiveSessionScreen(route.sessionId, route.config, navigator)
                is Route.Summary -> LiveSummaryScreen(route.sessionId, navigator)
                Route.Settings -> SettingsScreen(navigator)
                Route.EditProfile -> EditProfileScreen(navigator)
                Route.Paywall -> PaywallScreen(navigator)
            }
        }
    }
}

private fun routeTransition(from: Route, to: Route, pop: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())
    // Entering a live session is its own moment: the scene scales up out of the setup screen.
    if (to is Route.Live) return (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 1.08f)) togetherWith fadeOut(Motion.exit())
    if (from is Route.Live || to is Route.Summary) return fadeIn(Motion.enter()) togetherWith fadeOut(Motion.standard())
    val direction = if (pop) -1 else 1
    return (slideInHorizontally(Motion.slide) { it * direction / 4 } + fadeIn(Motion.standard())) togetherWith
        (slideOutHorizontally(Motion.slide) { -it * direction / 6 } + fadeOut(Motion.exit()))
}
