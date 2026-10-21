package org.mayad.fame_life.feature.points

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.domain.economy.ClaimResult
import org.mayad.fame_life.domain.economy.PointsError
import org.mayad.fame_life.domain.economy.PointsService
import org.mayad.fame_life.domain.economy.PointsState
import org.mayad.fame_life.domain.economy.StreakDay

@Immutable
data class DailyRewardUiState(
    val visible: Boolean = false,
    val days: ImmutableList<StreakDay> = persistentListOf(),
    val streakDays: Int = 0,
    /** Today's reward while it's waiting; null once collected. */
    val todayReward: Int? = null,
    val claiming: Boolean = false,
    /** Points just added by the server. */
    val claimed: Int? = null,
    val error: PointsError? = null,
)

sealed interface DailyRewardIntent {
    data object Claim : DailyRewardIntent
    data object Dismiss : DailyRewardIntent
    /** Opens the calendar on demand (for example from the Home points pill). */
    data object Open : DailyRewardIntent
}

/**
 * The daily check-in dialog. It opens by itself once per app session when today's reward is waiting, and the server
 * decides what is granted; the dialog only shows the calendar and the result.
 */
class DailyRewardViewModel(
    private val points: PointsService,
    private val analytics: Analytics,
) : MviViewModel<DailyRewardUiState, DailyRewardIntent, NoEvent>(DailyRewardUiState()) {

    /** The dialog opens by itself only once per ViewModel (one app session), even if dismissed unclaimed. */
    private var autoShown = false

    init {
        viewModelScope.launch {
            points.state.collect { state ->
                val ready = state as? PointsState.Ready
                if (ready == null) {
                    setState { copy(visible = false) }
                    return@collect
                }
                val open = ready.canClaimToday && !autoShown
                if (open) autoShown = true
                setState {
                    copy(
                        visible = visible || open,
                        days = ready.calendar.toImmutableList(),
                        streakDays = ready.streakDays,
                        todayReward = ready.todayReward,
                    )
                }
            }
        }
    }

    override fun onIntent(intent: DailyRewardIntent) {
        when (intent) {
            DailyRewardIntent.Claim -> claim()
            DailyRewardIntent.Dismiss -> setState { copy(visible = false, claimed = null, error = null) }
            DailyRewardIntent.Open -> if (points.state.value is PointsState.Ready) setState { copy(visible = true) }
        }
    }

    private fun claim() {
        if (currentState.claiming) return
        setState { copy(claiming = true, error = null) }
        viewModelScope.launch {
            when (val result = points.claimDailyReward()) {
                is ClaimResult.Claimed -> {
                    analytics.log(AnalyticsEvent.StreakClaimed(result.streakDays, result.reward))
                    setState { copy(claiming = false, claimed = result.reward) }
                }
                ClaimResult.AlreadyClaimed -> setState { copy(claiming = false) }
                is ClaimResult.Failed -> setState { copy(claiming = false, error = result.error) }
            }
        }
    }
}
