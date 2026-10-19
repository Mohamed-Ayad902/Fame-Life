package org.mayad.fame_life.feature.post

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.post_share_text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.platform.ShareService
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.PostComment
import org.mayad.fame_life.data.content.SeedContent

@Immutable
data class PostDetailUiState(
    val post: Post,
    val creator: Creator,
    val comments: ImmutableList<PostComment>,
    val more: ImmutableList<Post>,
    /** A local, in-session like on simulated content. Nothing is sent anywhere. */
    val liked: Boolean = false,
) {
    companion object {
        fun load(postId: String): PostDetailUiState? {
            val post = SeedContent.post(postId) ?: return null
            val creator = SeedContent.creator(post.creatorId) ?: return null
            return PostDetailUiState(
                post = post,
                creator = creator,
                comments = SeedContent.commentsFor(post).toImmutableList(),
                more = SeedContent.postsBy(creator.id).filter { it.id != postId }.toImmutableList(),
            )
        }
    }
}

sealed interface PostDetailIntent {
    data object ToggleLike : PostDetailIntent
    data object Share : PostDetailIntent
}

/** One post. [state] is null when the post isn't in the feed (the screen shows an honest "not available"). */
class PostDetailViewModel(
    postId: String,
    analytics: Analytics,
    private val share: ShareService,
) : MviViewModel<PostDetailUiState?, PostDetailIntent, NoEvent>(PostDetailUiState.load(postId)) {

    init {
        currentState?.let { analytics.log(AnalyticsEvent.PostViewed(it.post.category.name.lowercase())) }
    }

    override fun onIntent(intent: PostDetailIntent) {
        val state = currentState ?: return
        when (intent) {
            PostDetailIntent.ToggleLike -> setState { this?.copy(liked = !liked) }
            PostDetailIntent.Share -> viewModelScope.launch {
                // The shared text always says the post is simulated.
                share.share(getString(Res.string.post_share_text, state.post.caption, state.creator.handle, AppConfig.APP_NAME))
            }
        }
    }
}
