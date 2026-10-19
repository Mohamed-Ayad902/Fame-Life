package org.mayad.fame_life.app.navigation

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.feature.live.LiveSessionScreen
import org.mayad.fame_life.feature.live.LiveSetupScreen
import org.mayad.fame_life.feature.live.LiveSummaryScreen
import org.mayad.fame_life.feature.post.PostDetailScreen
import org.mayad.fame_life.feature.profile.CreatorProfileScreen
import org.mayad.fame_life.feature.profile.EditProfileScreen
import org.mayad.fame_life.feature.settings.PaywallScreen
import org.mayad.fame_life.feature.settings.SettingsScreen
import org.mayad.fame_life.feature.shell.MainNavigation
import org.mayad.fame_life.feature.shell.MainShell

/** Entering a live scales the scene up out of the studio; the slight overscale reads as stepping on stage. */
private const val LIVE_ENTER_SCALE = 1.08f

/**
 * The signed-in app's navigation. Each destination gets its own saved state and ViewModel store, both cleared when
 * it leaves the back stack. Screens receive plain callbacks; only this file knows the stack.
 */
@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppRouteSavedState, AppRoute.Main)
    val navigator: AppNavigator = remember(backStack) { BackStackNavigator(backStack) }
    val reduceMotion = Pulse.reducedMotion
    val mainNavigation = remember(navigator) {
        MainNavigation(
            openPost = { navigator.push(AppRoute.PostDetail(it)) },
            openCreator = { navigator.push(AppRoute.CreatorProfile(it)) },
            openLiveSetup = { navigator.push(AppRoute.LiveSetup(it)) },
            openSummary = { navigator.push(AppRoute.Summary(it)) },
            openSettings = { navigator.push(AppRoute.Settings) },
            openEditProfile = { navigator.push(AppRoute.EditProfile) },
            openPaywall = { navigator.push(AppRoute.Paywall) },
        )
    }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize().background(Pulse.colors.background),
        onBack = navigator::pop,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        transitionSpec = { if (reduceMotion) fade() else slide(forward = true) },
        popTransitionSpec = { if (reduceMotion) fade() else slide(forward = false) },
        predictivePopTransitionSpec = { if (reduceMotion) fade() else slide(forward = false) },
        entryProvider = entryProvider {
            entry<AppRoute.Main> { MainShell(mainNavigation) }
            entry<AppRoute.PostDetail> { route ->
                PostDetailScreen(route.postId, onBack = navigator::pop, onCreator = mainNavigation.openCreator, onPost = mainNavigation.openPost)
            }
            entry<AppRoute.CreatorProfile> { route ->
                CreatorProfileScreen(route.creatorId, onBack = navigator::pop, onPost = mainNavigation.openPost)
            }
            entry<AppRoute.LiveSetup> { route ->
                LiveSetupScreen(
                    route.title,
                    onClose = navigator::pop,
                    onSeePro = mainNavigation.openPaywall,
                    onStartLive = { sessionId, config -> navigator.push(AppRoute.Live(sessionId, config)) },
                )
            }
            entry<AppRoute.Live>(
                metadata = NavDisplay.transitionSpec {
                    if (reduceMotion) fade() else (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = LIVE_ENTER_SCALE)) togetherWith fadeOut(Motion.exit())
                },
            ) { route ->
                LiveSessionScreen(
                    route.sessionId,
                    route.config,
                    onCancelled = navigator::pop,
                    onFinished = { summaryId -> navigator.replaceTop(AppRoute.Summary(summaryId)) },
                )
            }
            entry<AppRoute.Summary>(metadata = NavDisplay.transitionSpec { fade() }) { route ->
                LiveSummaryScreen(
                    route.sessionId,
                    onDone = navigator::popToRoot,
                    onGoLiveAgain = { title ->
                        navigator.popToRoot()
                        navigator.push(AppRoute.LiveSetup(title))
                    },
                )
            }
            entry<AppRoute.Settings> {
                SettingsScreen(onBack = navigator::pop, onEditProfile = mainNavigation.openEditProfile, onPaywall = mainNavigation.openPaywall)
            }
            entry<AppRoute.EditProfile> { EditProfileScreen(onDone = navigator::pop) }
            entry<AppRoute.Paywall> { PaywallScreen(onBack = navigator::pop) }
        },
    )
}

private fun fade(): ContentTransform = fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())

/** New screens slide in from the trailing edge a quarter of the way, while the old one drifts back and fades. */
private fun slide(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(Motion.slide) { it * direction / 4 } + fadeIn(Motion.standard())) togetherWith
        (slideOutHorizontally(Motion.slide) { -it * direction / 6 } + fadeOut(Motion.exit()))
}
