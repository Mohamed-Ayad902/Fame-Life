package org.mayad.fame_life.app

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.platform.MotionPreference
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.cloud.AccountSync
import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState

/** Which top-level surface the app shows. */
enum class Gate { SPLASH, WELCOME, SETUP, SYNC_ERROR, MAIN }

@Immutable
data class RootUiState(val gate: Gate = Gate.SPLASH)

sealed interface RootIntent {
    data object Retry : RootIntent
    data object SignOut : RootIntent
}

/** How long the in-app splash stays at least, so it reads as a moment rather than a flicker. */
const val BRAND_SPLASH_MIN_MS = 900L

/**
 * Decides the top-level surface from sign-in, account sync and profile state. The splash stays up for a minimum
 * time and until the saved sign-in is known, so onboarding never flashes for someone already signed in.
 */
class RootViewModel(
    private val auth: AuthService,
    private val account: AccountSync,
    profiles: ProfileRepository,
    analytics: Analytics,
    motion: MotionPreference,
) : MviViewModel<RootUiState, RootIntent, NoEvent>(RootUiState()) {

    init {
        analytics.log(AnalyticsEvent.AppOpened)
        val splashDone = flow {
            emit(false)
            delay(if (motion.reduced()) BRAND_SPLASH_MIN_MS / 2 else BRAND_SPLASH_MIN_MS)
            emit(true)
        }
        viewModelScope.launch {
            combine(splashDone, auth.state, account.state, profiles.profile, ::gateFor).collect { gate -> setState { copy(gate = gate) } }
        }
    }

    override fun onIntent(intent: RootIntent) {
        when (intent) {
            RootIntent.Retry -> account.retry()
            RootIntent.SignOut -> viewModelScope.launch { runCatching { account.signOut() } }
        }
    }

    companion object {
        internal fun gateFor(splashDone: Boolean, auth: AuthState, sync: SyncState, profile: CreatorProfile?): Gate = when {
            !splashDone || auth is AuthState.Checking -> Gate.SPLASH
            auth is AuthState.SignedOut -> Gate.WELCOME
            profile != null -> Gate.MAIN
            sync is SyncState.Failed -> Gate.SYNC_ERROR
            sync is SyncState.Ready -> Gate.SETUP
            else -> Gate.SPLASH // Signed in; still fetching this account's profile.
        }
    }
}
