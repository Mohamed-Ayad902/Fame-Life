package org.mayad.fame_life.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.content.Challenge
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.content.Topic
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository

/** Everything Home shows. The feed comes from the fictional seed once; only the profile and unread count change. */
@Immutable
data class HomeUiState(
    val name: String,
    val palette: Int,
    val unread: Int,
    val featured: LiveRoom,
    val otherLives: ImmutableList<LiveRoom>,
    val challenge: Challenge,
    val topics: ImmutableList<Topic>,
    val posts: ImmutableList<Post>,
) {
    companion object {
        /** The seeded feed with no account details yet; also used by previews. */
        fun seeded(): HomeUiState {
            val featured = SeedContent.liveRooms.maxBy { it.viewers }
            return HomeUiState(
                name = "",
                palette = 0,
                unread = 0,
                featured = featured,
                otherLives = SeedContent.liveRooms.filter { it.id != featured.id }.toImmutableList(),
                challenge = SeedContent.challenges.first(),
                topics = SeedContent.topics.toImmutableList(),
                posts = SeedContent.posts.toImmutableList(),
            )
        }
    }
}

/** Home has no actions of its own: every tap is navigation, handled by the screen's callbacks. */
class HomeViewModel(
    profiles: ProfileRepository,
    notifications: NotificationRepository,
    analytics: Analytics,
) : MviViewModel<HomeUiState, Nothing, NoEvent>(HomeUiState.seeded()) {

    init {
        analytics.log(AnalyticsEvent.HomeViewed)
        viewModelScope.launch {
            combine(profiles.profile, notifications.notifications) { profile, inbox -> profile to inbox.count { !it.read } }
                .collect { (profile, unread) ->
                    setState { copy(name = profile?.displayName.orEmpty(), palette = profile?.avatarPalette ?: 0, unread = unread) }
                }
        }
    }

    override fun onIntent(intent: Nothing) = Unit
}
