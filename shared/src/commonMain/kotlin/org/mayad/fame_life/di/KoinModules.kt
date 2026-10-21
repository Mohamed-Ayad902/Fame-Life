package org.mayad.fame_life.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.InMemoryAnalytics
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.platform.MotionPreference
import org.mayad.fame_life.core.platform.PlatformShare
import org.mayad.fame_life.core.platform.ShareService
import org.mayad.fame_life.core.platform.SystemEpochClock
import org.mayad.fame_life.core.platform.SystemMotionPreference
import org.mayad.fame_life.core.platform.TextStore
import org.mayad.fame_life.core.platform.createGoogleTokenProvider
import org.mayad.fame_life.core.platform.createPulseBackend
import org.mayad.fame_life.core.platform.createSpeechInput
import org.mayad.fame_life.core.platform.createTextStore
import org.mayad.fame_life.core.platform.isFirebaseConfigured
import org.mayad.fame_life.data.cloud.AccountSync
import org.mayad.fame_life.data.cloud.DeferredCloudSink
import org.mayad.fame_life.data.cloud.FirebaseAuthService
import org.mayad.fame_life.data.cloud.FirestoreCloudStore
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.data.remote.audienceTransport
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.UnavailableAuthService
import org.mayad.fame_life.domain.ai.AudienceGenerator
import org.mayad.fame_life.domain.ai.RemoteAudienceGenerator
import org.mayad.fame_life.domain.ai.SpeechInput

/**
 * Optional services for a live session. [audience] is the backend generator: null when the build has no backend URL
 * or Firebase isn't configured (the backend needs the user's ID token), and the session then uses the on-device
 * audience and shows that. [speech] is null on platforms without speech recognition yet.
 */
class LiveServices(val speech: SpeechInput?, val audience: AudienceGenerator?)

/** The app-wide coroutine scope: lives as long as the process, never cancelled by a screen. */
val AppScope = named("appScope")

private val reportCorruption: (String, Throwable) -> Unit = { key, error ->
    println("Pulse: discarded unreadable local data for '$key': ${error.message}")
}

/**
 * Platform services and data. Everything is a lazy singleton, so nothing touches the platform until a screen
 * first needs it (by then Android's context and Firebase are initialized).
 */
val dataModule = module {
    single<CoroutineScope>(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<TextStore> { createTextStore() }
    single<Analytics> { InMemoryAnalytics() }
    single<ShareService> { PlatformShare }
    single<EpochClock> { SystemEpochClock }
    single<MotionPreference> { SystemMotionPreference }

    single<AuthService> { if (isFirebaseConfigured()) FirebaseAuthService(get(AppScope), createGoogleTokenProvider()) else UnavailableAuthService() }

    // The repositories are the device cache screens read; AccountSync fills them from Firestore and mirrors writes.
    single { DeferredCloudSink() }
    single { ProfileRepository(get(), get<DeferredCloudSink>(), reportCorruption) }
    single { SessionHistoryRepository(get(), get<DeferredCloudSink>(), reportCorruption) }
    single { NotificationRepository(get(), get<DeferredCloudSink>(), reportCorruption) }
    single { EntitlementRepository() }
    single {
        val entitlements = get<EntitlementRepository>()
        AccountSync(
            auth = get(),
            cloud = if (isFirebaseConfigured()) FirestoreCloudStore() else null,
            store = get(),
            profiles = get(),
            history = get(),
            notifications = get(),
            scope = get(AppScope),
            historyLimit = { entitlements.entitlements.value.limits.historySize },
            // Only the operation and error type: messages can contain paths with the uid.
            onError = { what, error -> println("Pulse: cloud $what failed (${error::class.simpleName})") },
        ).also { get<DeferredCloudSink>().target = it }
    }

    single {
        val backend = if (isFirebaseConfigured()) createPulseBackend() else null
        LiveServices(speech = createSpeechInput(backend), audience = backend?.let { RemoteAudienceGenerator(it.audienceTransport()) })
    }
}

/** Every module the app needs. Tests can start Koin with [dataModule] swapped for fakes. */
fun appModules(): List<Module> = listOf(dataModule, viewModelModule)

private var started = false

/** Starts Koin once per process. Android calls this from its Application, iOS from MainViewController. */
fun initKoin(config: KoinAppDeclaration = {}): KoinApplication? {
    if (started) return null
    started = true
    return startKoin {
        config()
        modules(appModules())
    }
}
