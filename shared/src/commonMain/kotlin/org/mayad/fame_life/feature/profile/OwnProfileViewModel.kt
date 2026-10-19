package org.mayad.fame_life.feature.profile

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
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.simulation.SessionSummary

enum class ProfileTab { LIVES, BADGES }

/** The signed-in creator's profile. [profile] is null only for the moment before the account cache loads. */
@Immutable
data class OwnProfileUiState(
    val profile: CreatorProfile? = null,
    val sessions: ImmutableList<SessionSummary> = persistentListOf(),
    val plan: Plan = Plan.FREE,
    val tab: ProfileTab = ProfileTab.LIVES,
) {
    val bestPeak: Int get() = sessions.maxOfOrNull { it.peakViewers } ?: 0
}

sealed interface OwnProfileIntent {
    data class SelectTab(val tab: ProfileTab) : OwnProfileIntent
}

class OwnProfileViewModel(
    profiles: ProfileRepository,
    history: SessionHistoryRepository,
    entitlements: EntitlementRepository,
    analytics: Analytics,
) : MviViewModel<OwnProfileUiState, OwnProfileIntent, NoEvent>(OwnProfileUiState()) {

    init {
        analytics.log(AnalyticsEvent.ProfileViewed(own = true))
        viewModelScope.launch {
            combine(profiles.profile, history.sessions, entitlements.entitlements) { profile, sessions, plan ->
                Triple(profile, sessions.toImmutableList(), plan.plan)
            }.collect { (profile, sessions, plan) -> setState { copy(profile = profile, sessions = sessions, plan = plan) } }
        }
    }

    override fun onIntent(intent: OwnProfileIntent) {
        when (intent) {
            is OwnProfileIntent.SelectTab -> setState { copy(tab = intent.tab) }
        }
    }
}
