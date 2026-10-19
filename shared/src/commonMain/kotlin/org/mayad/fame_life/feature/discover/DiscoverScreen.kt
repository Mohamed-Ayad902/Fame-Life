package org.mayad.fame_life.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.feature.shell.MainNavigation

/** Tier 1: connects Discover's state and intents to its content. */
@Composable
fun DiscoverScreen(navigation: MainNavigation, viewModel: DiscoverViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DiscoverContent(
        state = state,
        onQuery = { viewModel.onIntent(DiscoverIntent.Query(it)) },
        onCategory = { viewModel.onIntent(DiscoverIntent.SelectCategory(it)) },
        onCreator = navigation.openCreator,
        onPost = navigation.openPost,
    )
}
