package org.mayad.fame_life.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.post_go_back
import famelife.shared.generated.resources.profile_creator_not_found
import famelife.shared.generated.resources.profile_unavailable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs

/** Tier 1: a fictional creator's profile, or an honest "not available" when the id is unknown. */
@Composable
fun CreatorProfileScreen(
    creatorId: String,
    onBack: () -> Unit,
    onPost: (String) -> Unit,
    viewModel: CreatorProfileViewModel = koinViewModel(key = creatorId) { parametersOf(creatorId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        AtmosphereBackground {
            EmptyState(
                Glyphs.User,
                Gloss.Slate,
                stringResource(Res.string.profile_creator_not_found),
                stringResource(Res.string.profile_unavailable),
                Modifier.align(Alignment.Center),
                stringResource(Res.string.post_go_back),
                onBack,
            )
        }
        return
    }
    CreatorProfileContent(current, onBack = onBack, onPost = onPost)
}
