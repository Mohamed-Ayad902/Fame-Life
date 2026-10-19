package org.mayad.fame_life.feature.settings

import androidx.compose.runtime.Immutable
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.domain.entitlement.PlanLimits

/** The two plans side by side. Billing isn't wired, so there is nothing to buy yet and no intents. */
@Immutable
data class PaywallUiState(val free: PlanLimits = PlanLimits.Free, val pro: PlanLimits = PlanLimits.Pro)

class PaywallViewModel(analytics: Analytics) : MviViewModel<PaywallUiState, Nothing, NoEvent>(PaywallUiState()) {
    init {
        analytics.log(AnalyticsEvent.PaywallViewed)
    }

    override fun onIntent(intent: Nothing) = Unit
}
