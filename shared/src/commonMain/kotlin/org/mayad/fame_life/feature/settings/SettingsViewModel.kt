package org.mayad.fame_life.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.cloud.AccountSync
import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.entitlement.Plan

enum class SettingsDialog { NONE, SIGN_OUT, DELETE }

@Immutable
data class SettingsUiState(
    val email: String? = null,
    val offline: Boolean = false,
    val plan: Plan = Plan.FREE,
    val dialog: SettingsDialog = SettingsDialog.NONE,
    val deleting: Boolean = false,
    /** Why the last delete attempt failed; shown inside the delete dialog. */
    val deleteError: AuthErrorKind? = null,
    val showSimulation: Boolean = false,
    val showData: Boolean = false,
)

sealed interface SettingsIntent {
    data object AskSignOut : SettingsIntent
    data object AskDelete : SettingsIntent
    data object Dismiss : SettingsIntent
    data object ConfirmSignOut : SettingsIntent
    data object ConfirmDelete : SettingsIntent
    data object ToggleSimulation : SettingsIntent
    data object ToggleData : SettingsIntent
}

/**
 * Account state and the two destructive actions. Neither needs an event: once the auth state flips to signed out,
 * the app root swaps this whole tree for the welcome screens.
 */
class SettingsViewModel(
    auth: AuthService,
    private val account: AccountSync,
    entitlements: EntitlementRepository,
    private val analytics: Analytics,
) : MviViewModel<SettingsUiState, SettingsIntent, NoEvent>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            combine(auth.state, account.state, entitlements.entitlements) { authState, sync, plan ->
                Triple((authState as? AuthState.SignedIn)?.user?.email, (sync as? SyncState.Ready)?.offline == true, plan.plan)
            }.collect { (email, offline, plan) -> setState { copy(email = email, offline = offline, plan = plan) } }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.AskSignOut -> setState { copy(dialog = SettingsDialog.SIGN_OUT) }
            SettingsIntent.AskDelete -> setState { copy(dialog = SettingsDialog.DELETE, deleteError = null) }
            SettingsIntent.Dismiss -> if (!currentState.deleting) setState { copy(dialog = SettingsDialog.NONE) }
            SettingsIntent.ConfirmSignOut -> signOut()
            SettingsIntent.ConfirmDelete -> deleteAccount()
            SettingsIntent.ToggleSimulation -> setState { copy(showSimulation = !showSimulation) }
            SettingsIntent.ToggleData -> setState { copy(showData = !showData) }
        }
    }

    private fun signOut() {
        setState { copy(dialog = SettingsDialog.NONE) }
        viewModelScope.launch {
            runCatching { account.signOut() }
                .onSuccess { analytics.log(AnalyticsEvent.SignedOut) }
                .onFailure { println("Pulse: sign-out failed (${it::class.simpleName})") }
        }
    }

    /** Re-confirms the Google account first: Firebase only deletes accounts with a recent sign-in. */
    private fun deleteAccount() {
        if (currentState.deleting) return
        setState { copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            try {
                account.deleteAccount()
                analytics.log(AnalyticsEvent.AccountDeleted)
            } catch (e: AuthException) {
                setState { copy(deleting = false, deleteError = e.kind) }
            }
        }
    }
}
