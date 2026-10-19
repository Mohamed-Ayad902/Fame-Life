package org.mayad.fame_life.feature.shell

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.data.local.NotificationRepository

enum class Tab { HOME, DISCOVER, INBOX, PROFILE }

@Immutable
data class ShellUiState(val tab: Tab = Tab.HOME, val unread: Int = 0)

sealed interface ShellIntent {
    data class Select(val tab: Tab) : ShellIntent
    data object Create : ShellIntent
}

sealed interface ShellEvent {
    data object OpenLiveSetup : ShellEvent
}

/** The tab shell: which tab is showing and the inbox badge. */
class ShellViewModel(
    notifications: NotificationRepository,
    private val analytics: Analytics,
) : MviViewModel<ShellUiState, ShellIntent, ShellEvent>(ShellUiState()) {

    init {
        viewModelScope.launch {
            notifications.notifications.collect { list -> setState { copy(unread = list.count { !it.read }) } }
        }
    }

    override fun onIntent(intent: ShellIntent) {
        when (intent) {
            is ShellIntent.Select -> setState { copy(tab = intent.tab) }
            ShellIntent.Create -> {
                analytics.log(AnalyticsEvent.CreateLiveTapped)
                emit(ShellEvent.OpenLiveSetup)
            }
        }
    }
}
