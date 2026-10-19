package org.mayad.fame_life.data.cloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.platform.TextStore
import org.mayad.fame_life.data.local.CloudSink
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.simulation.SessionSummary

sealed interface SyncState {
    /** Nobody is signed in, or auth is still being restored. */
    data object Idle : SyncState
    data object Loading : SyncState

    /** The device cache matches the account. [offline] means the cloud could not be reached and the cache is shown. */
    data class Ready(val offline: Boolean = false) : SyncState

    /** Signed in, the cloud could not be reached and there is nothing cached for this account yet. */
    data object Failed : SyncState
}

/**
 * Keeps the device cache (the local repositories screens read from) in step with the signed-in account:
 * - on sign-in it pulls the profile, recent sessions and inbox from [cloud];
 * - every local change arrives through [CloudSink] and is written to the user's own documents;
 * - on sign-out, or when a different account signs in, the cache is wiped so accounts never mix.
 *
 * Firestore queues writes made offline and retries them itself, so a write here is fire-and-forget; failures are
 * reported through [onError] and never shown as success.
 */
class AccountSync(
    private val auth: AuthService,
    private val cloud: CloudStore?,
    private val store: TextStore,
    private val profiles: ProfileRepository,
    private val history: SessionHistoryRepository,
    private val notifications: NotificationRepository,
    private val scope: CoroutineScope,
    private val historyLimit: () -> Int,
    private val onError: (String, Throwable) -> Unit,
) : CloudSink {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()

    private var uid: String? = null
    private var loadJob: Job? = null

    init {
        scope.launch { auth.state.collect(::onAuthState) }
    }

    private fun onAuthState(state: AuthState) {
        when (state) {
            AuthState.Checking -> mutableState.value = SyncState.Idle
            AuthState.SignedOut -> {
                loadJob?.cancel()
                uid = null
                clearCache()
                mutableState.value = SyncState.Idle
            }
            is AuthState.SignedIn -> {
                if (uid == state.user.uid && mutableState.value !is SyncState.Idle) return
                uid = state.user.uid
                if (store.read(OWNER_KEY) != state.user.uid) {
                    clearCache()
                    store.write(OWNER_KEY, state.user.uid)
                }
                load(state.user.uid)
            }
        }
    }

    /** Tries the cloud again after [SyncState.Failed] or while offline. */
    fun retry() {
        uid?.let(::load)
    }

    private fun load(uid: String) {
        loadJob?.cancel()
        val cloud = cloud ?: run {
            mutableState.value = SyncState.Ready(offline = true)
            return
        }
        mutableState.value = if (profiles.profile.value != null) SyncState.Ready(offline = true) else SyncState.Loading
        loadJob = scope.launch {
            try {
                val remoteProfile = cloud.loadProfile(uid)
                val remoteSessions = cloud.loadSessions(uid, historyLimit())
                val remoteInbox = cloud.loadNotifications(uid, NotificationRepository.MAX)
                val localProfile = profiles.profile.value
                if (remoteProfile == null && localProfile != null) {
                    // Made on this device before the first save reached the cloud: upload rather than lose it.
                    cloud.saveProfile(uid, localProfile)
                } else {
                    profiles.replace(remoteProfile)
                }
                // The cloud is the truth: Firestore's own cache already includes writes still waiting to upload, so
                // anything missing here was deleted (by the user, another device or the console) and must go.
                history.replace(remoteSessions)
                notifications.replace(remoteInbox)
                mutableState.value = SyncState.Ready()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("load", e)
                mutableState.value = if (profiles.profile.value != null) SyncState.Ready(offline = true) else SyncState.Failed
            }
        }
    }

    suspend fun signOut() = auth.signOut()

    /**
     * Deletes the account: confirms the Google account again, removes every cloud document, deletes the Firebase
     * user, then wipes this device. Throws [AuthException]; nothing on the device is removed unless the cloud
     * deletion succeeded.
     */
    suspend fun deleteAccount() {
        val uid = uid ?: throw AuthException(AuthErrorKind.WRONG_ACCOUNT)
        auth.reauthenticateWithGoogle()
        try {
            cloud?.deleteEverything(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError("delete", e)
            throw AuthException(AuthErrorKind.NETWORK, e)
        }
        auth.deleteCurrentUser()
        clearCache()
    }

    override fun profileChanged(profile: CreatorProfile) = write("profile") { cloud, uid -> cloud.saveProfile(uid, profile) }

    override fun sessionSaved(summary: SessionSummary, removedIds: List<String>) = write("session") { cloud, uid ->
        cloud.saveSession(uid, summary)
        cloud.deleteSessions(uid, removedIds)
    }

    override fun notificationsChanged(changed: List<InAppNotification>) = write("notifications") { cloud, uid ->
        cloud.saveNotifications(uid, changed)
    }

    private fun write(what: String, block: suspend (CloudStore, String) -> Unit) {
        val cloud = cloud ?: return
        val uid = uid ?: return
        scope.launch {
            try {
                block(cloud, uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("write $what", e)
            }
        }
    }

    private fun clearCache() {
        profiles.clear()
        history.clear()
        notifications.clear()
        store.remove(OWNER_KEY)
    }

    private companion object {
        const val OWNER_KEY = "account.owner.v1"
    }
}

/** Lets the repositories be built before [AccountSync], which needs them. */
class DeferredCloudSink : CloudSink {
    var target: CloudSink = CloudSink.None
    override fun profileChanged(profile: CreatorProfile) = target.profileChanged(profile)
    override fun sessionSaved(summary: SessionSummary, removedIds: List<String>) = target.sessionSaved(summary, removedIds)
    override fun notificationsChanged(changed: List<InAppNotification>) = target.notificationsChanged(changed)
}
