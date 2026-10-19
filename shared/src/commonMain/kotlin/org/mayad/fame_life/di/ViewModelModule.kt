package org.mayad.fame_life.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mayad.fame_life.app.RootViewModel
import org.mayad.fame_life.feature.discover.DiscoverViewModel
import org.mayad.fame_life.feature.home.HomeViewModel
import org.mayad.fame_life.feature.live.LiveSessionViewModel
import org.mayad.fame_life.feature.live.LiveSetupViewModel
import org.mayad.fame_life.feature.live.LiveSummaryViewModel
import org.mayad.fame_life.feature.live.SessionRecorder
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
import org.mayad.fame_life.domain.simulation.SimulationConfig

/**
 * One entry per screen ViewModel. Screens get them with `koinViewModel()`; the Navigation 3 entry decorator scopes
 * each one to its back-stack entry, so it is cleared when the screen is popped. ViewModels that take a route argument
 * read it from the parameters (`parametersOf(...)` at the call site).
 */
val viewModelModule = module {
    factory { SessionRecorder(get(), get(), get(), get(), get(), get()) }

    viewModelOf(::RootViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::ProfileSetupViewModel)
    viewModelOf(::ShellViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::DiscoverViewModel)
    viewModelOf(::InboxViewModel)
    viewModelOf(::OwnProfileViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PaywallViewModel)

    viewModel { (postId: String) -> PostDetailViewModel(postId, get(), get()) }
    viewModel { (creatorId: String) -> CreatorProfileViewModel(creatorId, get()) }
    viewModel { (title: String) -> LiveSetupViewModel(title, get(), get(), get(), get(), get()) }
    viewModel { (sessionId: String, config: SimulationConfig) ->
        LiveSessionViewModel(sessionId, config, get(), get(), get(), get(), get(), get())
    }
    viewModel { (sessionId: String) -> LiveSummaryViewModel(sessionId, get(), get(), get(), get()) }
}
