package org.mayad.fame_life.app

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.analytics.InMemoryAnalytics
import org.mayad.fame_life.core.platform.TextStore
import org.mayad.fame_life.core.platform.createSpeechInput
import org.mayad.fame_life.core.platform.createGoogleTokenProvider
import org.mayad.fame_life.core.platform.createTextStore
import org.mayad.fame_life.core.platform.isFirebaseConfigured
import org.mayad.fame_life.data.cloud.AccountSync
import org.mayad.fame_life.data.cloud.CloudStore
import org.mayad.fame_life.data.cloud.DeferredCloudSink
import org.mayad.fame_life.data.cloud.FirebaseAuthService
import org.mayad.fame_life.data.cloud.FirestoreCloudStore
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.UnavailableAuthService
import org.mayad.fame_life.domain.ai.AudienceGenerator
import org.mayad.fame_life.domain.ai.SpeechInput

/** Product name and copy that may change. Keep brand strings here, not scattered in screens. */
object AppConfig {
    const val APP_NAME = "Pulse"
    const val TAGLINE = "Go live. Talk naturally. Feel the crowd."
}

/**
 * Manual dependency wiring. The app is small enough that a DI framework would add more ceremony than it
 * removes; swap implementations here (Firebase, backend generator) as they are configured.
 */
class AppContainer(
    store: TextStore = createTextStore(),
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    firebase: Boolean = isFirebaseConfigured(),
    val auth: AuthService = if (firebase) FirebaseAuthService(scope, createGoogleTokenProvider()) else UnavailableAuthService(),
    cloud: CloudStore? = if (firebase) FirestoreCloudStore() else null,
) {
    private val reportCorruption: (String, Throwable) -> Unit = { key, error ->
        println("Pulse: discarded unreadable local data for '$key': ${error.message}")
    }

    val analytics: Analytics = InMemoryAnalytics()
    private val sink = DeferredCloudSink()

    // Screens read these device caches; AccountSync fills them from Firestore and mirrors changes back.
    val profiles = ProfileRepository(store, sink, reportCorruption)
    val history = SessionHistoryRepository(store, sink, reportCorruption)
    val notifications = NotificationRepository(store, sink, reportCorruption)
    val entitlements = EntitlementRepository()

    val account = AccountSync(
        auth = auth,
        cloud = cloud,
        store = store,
        profiles = profiles,
        history = history,
        notifications = notifications,
        scope = scope,
        historyLimit = { entitlements.entitlements.value.limits.historySize },
        // Only the operation and error type: messages can contain paths with the uid.
        onError = { what, error -> println("Pulse: cloud $what failed (${error::class.simpleName})") },
    ).also { sink.target = it }

    /**
     * The backend audience generator. Null until the trusted backend exists (see docs/ARCHITECTURE.md); the
     * live session then uses the on-device audience and shows that state.
     */
    val audienceGenerator: AudienceGenerator? = null

    val speechInput: SpeechInput? by lazy { createSpeechInput() }

    /** Deletes the account, its cloud documents and this device's cache. Throws AuthException. */
    suspend fun deleteAccount() {
        account.deleteAccount()
        analytics.log(AnalyticsEvent.AccountDeleted)
    }

    fun close() = scope.cancel()
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
