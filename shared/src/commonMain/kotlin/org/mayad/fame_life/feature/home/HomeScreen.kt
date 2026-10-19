package org.mayad.fame_life.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.feature.shell.MainNavigation

/** Tier 1: connects Home's state to its content and turns taps into navigation. */
@Composable
fun HomeScreen(navigation: MainNavigation, onOpenInbox: () -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onInbox = onOpenInbox,
        onCreator = navigation.openCreator,
        onPost = navigation.openPost,
        onChallenge = navigation.openLiveSetup,
    )
}
