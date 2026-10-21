package org.mayad.fame_life.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.economy.PointsService
import org.mayad.fame_life.domain.economy.PointsState
import org.mayad.fame_life.domain.progression.AchievementState
import org.mayad.fame_life.domain.progression.Achievements
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.progression.LevelProgress

/**
 * The signed-in creator's progress for the HUD. Every number is the user's own simulated progress from their
 * profile and session history; nothing here is real-world reach.
 */
@Immutable
data class CreatorHud(
    /** The coin pill: the points wallet when the economy is on, otherwise the creator's fame points. */
    val points: Int = 0,
    /** True when [points] is the spendable wallet rather than fame points (changes how it's announced). */
    val wallet: Boolean = false,
    /** Consecutive daily check-ins, or null while the economy is off (the pill is hidden). */
    val streakDays: Int? = null,
    val level: LevelProgress = FameProgression.progress(0),
    val lives: Int = 0,
    val bestPeak: Int = 0,
    val achievements: ImmutableList<AchievementState> = persistentListOf(),
) {
    val earned: Int get() = achievements.count { it.unlocked }
}

/** Everything Home shows. The feed comes from the fictional seed once; the profile, progress and unread count change. */
@Immutable
data class HomeUiState(
    val name: String,
    val palette: Int,
    val unread: Int,
    val hud: CreatorHud,
    val featured: LiveRoom,
    val otherLives: ImmutableList<LiveRoom>,
    val liveCreatorIds: ImmutableList<String>,
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
                hud = CreatorHud(achievements = Achievements.evaluate(0, emptyList()).toImmutableList()),
                featured = featured,
                otherLives = SeedContent.liveRooms.filter { it.id != featured.id }.toImmutableList(),
                liveCreatorIds = SeedContent.liveRooms.map { it.creatorId }.toImmutableList(),
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
    history: SessionHistoryRepository,
    notifications: NotificationRepository,
    economy: PointsService,
    analytics: Analytics,
) : MviViewModel<HomeUiState, Nothing, NoEvent>(HomeUiState.seeded()) {

    init {
        analytics.log(AnalyticsEvent.HomeViewed)
        viewModelScope.launch {
            combine(profiles.profile, history.sessions, notifications.notifications, economy.state) { profile, sessions, inbox, wallet ->
                val points = profile?.famePoints ?: 0
                val ready = wallet as? PointsState.Ready
                Triple(
                    profile,
                    CreatorHud(
                        points = ready?.balance ?: points,
                        wallet = ready != null,
                        streakDays = ready?.streakDays,
                        level = FameProgression.progress(points),
                        lives = profile?.sessionsCompleted ?: sessions.size,
                        bestPeak = sessions.maxOfOrNull { it.peakViewers } ?: 0,
                        achievements = Achievements.evaluate(points, sessions).toImmutableList(),
                    ),
                    inbox.count { !it.read },
                )
            }.collect { (profile, hud, unread) ->
                setState { copy(name = profile?.displayName.orEmpty(), palette = profile?.avatarPalette ?: 0, unread = unread, hud = hud) }
            }
        }
    }

    override fun onIntent(intent: Nothing) = Unit
}
