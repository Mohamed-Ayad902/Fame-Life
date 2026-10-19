package org.mayad.fame_life.feature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.feature.shell.MainNavigation

/** Tier 1: marks the inbox read each time the tab opens and turns taps into navigation. */
@Composable
fun InboxScreen(navigation: MainNavigation, viewModel: InboxViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(InboxIntent.Opened) }
    InboxContent(
        state = state,
        onGoLive = { navigation.openLiveSetup("") },
        onOpenSummary = navigation.openSummary,
    )
}
