package org.mayad.fame_life.feature.profile

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent

/** A fictional creator from the seed, their posts and their simulated live room if they have one. */
@Immutable
data class CreatorProfileUiState(val creator: Creator, val posts: ImmutableList<Post>, val live: LiveRoom?) {
    companion object {
        fun load(creatorId: String): CreatorProfileUiState? = SeedContent.creator(creatorId)?.let {
            CreatorProfileUiState(it, SeedContent.postsBy(creatorId).toImmutableList(), SeedContent.liveFor(creatorId))
        }
    }
}

/** Read-only: [state] is null when the creator isn't in the seed (the screen says so honestly). */
class CreatorProfileViewModel(
    creatorId: String,
    analytics: Analytics,
) : MviViewModel<CreatorProfileUiState?, Nothing, NoEvent>(CreatorProfileUiState.load(creatorId)) {

    init {
        if (currentState != null) analytics.log(AnalyticsEvent.ProfileViewed(own = false))
    }

    override fun onIntent(intent: Nothing) = Unit
}
