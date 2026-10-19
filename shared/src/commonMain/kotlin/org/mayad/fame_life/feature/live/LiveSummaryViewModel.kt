package org.mayad.fame_life.feature.live

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.platform.ShareService
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.simulation.SessionSummary

/** [summary] is null when the session isn't in the saved history (deleted, or beyond the plan's history size). */
@Immutable
data class LiveSummaryUiState(val summary: SessionSummary? = null, val profile: CreatorProfile? = null)

sealed interface LiveSummaryIntent {
    /** [message] is the localized share text; it always says the audience was simulated. */
    data class Share(val message: String) : LiveSummaryIntent
    data object GoLiveAgain : LiveSummaryIntent
}

sealed interface LiveSummaryEvent {
    data class OpenSetup(val title: String) : LiveSummaryEvent
}

class LiveSummaryViewModel(
    sessionId: String,
    history: SessionHistoryRepository,
    profiles: ProfileRepository,
    private val analytics: Analytics,
    private val share: ShareService,
) : MviViewModel<LiveSummaryUiState, LiveSummaryIntent, LiveSummaryEvent>(LiveSummaryUiState()) {

    init {
        analytics.log(AnalyticsEvent.LiveSummaryViewed)
        viewModelScope.launch {
            combine(history.sessions, profiles.profile) { sessions, profile -> LiveSummaryUiState(sessions.firstOrNull { it.id == sessionId }, profile) }
                .collect { next -> setState { next } }
        }
    }

    override fun onIntent(intent: LiveSummaryIntent) {
        when (intent) {
            is LiveSummaryIntent.Share -> {
                analytics.log(AnalyticsEvent.SummaryShared)
                share.share(intent.message)
            }
            LiveSummaryIntent.GoLiveAgain -> {
                analytics.log(AnalyticsEvent.LiveRestarted)
                emit(LiveSummaryEvent.OpenSetup(currentState.summary?.title.orEmpty()))
            }
        }
    }
}
