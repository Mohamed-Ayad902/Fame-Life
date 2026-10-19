package org.mayad.fame_life.feature.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.post_go_back
import famelife.shared.generated.resources.post_unavailable_body
import famelife.shared.generated.resources.post_unavailable_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs

/** Tier 1: connects one post's state and intents to its content. */
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
    viewModel: PostDetailViewModel = koinViewModel(key = postId) { parametersOf(postId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        AtmosphereBackground {
            EmptyState(
                Glyphs.Info,
                Gloss.Slate,
                stringResource(Res.string.post_unavailable_title),
                stringResource(Res.string.post_unavailable_body),
                Modifier.align(Alignment.Center),
                stringResource(Res.string.post_go_back),
                onBack,
            )
        }
        return
    }
    PostDetailContent(
        state = current,
        onLike = { viewModel.onIntent(PostDetailIntent.ToggleLike) },
        onShare = { viewModel.onIntent(PostDetailIntent.Share) },
        onBack = onBack,
        onCreator = onCreator,
        onPost = onPost,
    )
}
