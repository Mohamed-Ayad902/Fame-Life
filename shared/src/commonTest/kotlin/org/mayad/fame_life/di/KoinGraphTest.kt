package org.mayad.fame_life.di

import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.mayad.fame_life.app.RootViewModel
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.InMemoryAnalytics
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.core.platform.MotionPreference
import org.mayad.fame_life.core.platform.ShareService
import org.mayad.fame_life.core.platform.TextStore
import org.mayad.fame_life.data.cloud.AccountSync
import org.mayad.fame_life.data.cloud.FakeCloudStore
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.FakeAuthService
import org.mayad.fame_life.domain.simulation.SimulationConfig
import org.mayad.fame_life.feature.ViewModelTest
import org.mayad.fame_life.feature.discover.DiscoverViewModel
import org.mayad.fame_life.feature.home.HomeViewModel
import org.mayad.fame_life.feature.live.LiveSessionViewModel
import org.mayad.fame_life.feature.live.LiveSetupViewModel
import org.mayad.fame_life.feature.live.LiveSummaryViewModel
import org.mayad.fame_life.feature.notifications.InboxViewModel
import org.mayad.fame_life.feature.onboarding.OnboardingViewModel
import org.mayad.fame_life.feature.onboarding.ProfileSetupViewModel
import org.mayad.fame_life.feature.post.PostDetailViewModel
import org.mayad.fame_life.feature.profile.CreatorProfileViewModel
import org.mayad.fame_life.feature.profile.EditProfileViewModel
import org.mayad.fame_life.feature.profile.OwnProfileViewModel
import org.mayad.fame_life.feature.settings.PaywallViewModel
import org.mayad.fame_life.feature.settings.SettingsViewModel
import org.mayad.fame_life.feature.shell.ShellViewModel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Every screen ViewModel resolves from [viewModelModule] against a data graph of fakes, so a missing binding fails here. */
class KoinGraphTest : ViewModelTest() {
    private val fakeData = module {
        single<CoroutineScope>(AppScope) { CoroutineScope(dispatcher) }
        single<TextStore> { InMemoryTextStore() }
        single<Analytics> { InMemoryAnalytics() }
        single<ShareService> { ShareService { } }
        single<EpochClock> { EpochClock { 0L } }
        single<MotionPreference> { MotionPreference { true } }
        single<AuthService> { FakeAuthService() }
        single { ProfileRepository(get()) { _, _ -> } }
        single { SessionHistoryRepository(get()) { _, _ -> } }
        single { NotificationRepository(get()) { _, _ -> } }
        single { EntitlementRepository() }
        single { AccountSync(get(), FakeCloudStore(), get(), get(), get(), get(), get(AppScope), { 10 }, { _, _ -> }) }
        single { LiveServices(speech = null, audience = null) }
    }

    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun everyViewModelResolves() {
        val koin = startKoin { modules(fakeData, viewModelModule) }.koin
        assertNotNull(koin.get<RootViewModel>())
        assertNotNull(koin.get<OnboardingViewModel>())
        assertNotNull(koin.get<ProfileSetupViewModel>())
        assertNotNull(koin.get<ShellViewModel>())
        assertNotNull(koin.get<HomeViewModel>())
        assertNotNull(koin.get<DiscoverViewModel>())
        assertNotNull(koin.get<InboxViewModel>())
        assertNotNull(koin.get<OwnProfileViewModel>())
        assertNotNull(koin.get<EditProfileViewModel>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<PaywallViewModel>())
        assertNotNull(koin.get<PostDetailViewModel> { parametersOf("missing") })
        assertNotNull(koin.get<CreatorProfileViewModel> { parametersOf("missing") })
        assertNotNull(koin.get<LiveSetupViewModel> { parametersOf("title") })
        assertNotNull(koin.get<LiveSessionViewModel> { parametersOf("s1", SimulationConfig()) })
        assertNotNull(koin.get<LiveSummaryViewModel> { parametersOf("s1") })
    }
}
