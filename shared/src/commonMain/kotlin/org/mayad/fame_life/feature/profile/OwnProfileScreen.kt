package org.mayad.fame_life.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.feature.shell.MainNavigation

/** Tier 1: connects the signed-in creator's profile to its content. */
@Composable
fun OwnProfileScreen(navigation: MainNavigation, viewModel: OwnProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Without a profile the app shows onboarding instead; this only covers the first frame while the cache loads.
    if (state.profile == null) return
    OwnProfileContent(
        state = state,
        onTab = { viewModel.onIntent(OwnProfileIntent.SelectTab(it)) },
        onSettings = navigation.openSettings,
        onEdit = navigation.openEditProfile,
        onPlan = navigation.openPaywall,
        onGoLive = { navigation.openLiveSetup("") },
        onSession = navigation.openSummary,
    )
}
