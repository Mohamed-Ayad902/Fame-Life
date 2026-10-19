package org.mayad.fame_life.feature.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.welcome_notification_body
import famelife.shared.generated.resources.welcome_notification_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.domain.profile.ProfileFieldError
import org.mayad.fame_life.domain.profile.ProfileRules

enum class SetupStep { PROFILE, INTERESTS }

@Immutable
data class ProfileSetupUiState(
    val step: SetupStep = SetupStep.PROFILE,
    val displayName: String = "",
    val username: String = "",
    val palette: Int = 0,
    val interests: ImmutableSet<Category> = persistentSetOf(),
    /** Errors show only after the first attempt to continue, so the form doesn't greet the user in red. */
    val attempted: Boolean = false,
    val saving: Boolean = false,
) {
    val nameError: ProfileFieldError? get() = ProfileRules.displayNameError(displayName)
    val usernameError: ProfileFieldError? get() = ProfileRules.usernameError(username)
}

sealed interface ProfileSetupIntent {
    data class Name(val value: String) : ProfileSetupIntent
    data class Username(val value: String) : ProfileSetupIntent
    data class Palette(val index: Int) : ProfileSetupIntent
    data class ToggleInterest(val category: Category) : ProfileSetupIntent
    data object Continue : ProfileSetupIntent
    data object Back : ProfileSetupIntent
    data object Finish : ProfileSetupIntent
}

/** After a new account: who you are on stage, then optional interests. Saving the profile ends setup (the root gate moves on). */
class ProfileSetupViewModel(
    private val profiles: ProfileRepository,
    private val notifications: NotificationRepository,
    private val analytics: Analytics,
    private val clock: EpochClock,
) : MviViewModel<ProfileSetupUiState, ProfileSetupIntent, NoEvent>(ProfileSetupUiState()) {

    override fun onIntent(intent: ProfileSetupIntent) {
        when (intent) {
            is ProfileSetupIntent.Name -> setState { copy(displayName = intent.value) }
            is ProfileSetupIntent.Username -> setState { copy(username = intent.value.trim().lowercase()) }
            is ProfileSetupIntent.Palette -> setState { copy(palette = intent.index) }
            is ProfileSetupIntent.ToggleInterest -> setState {
                copy(interests = (if (intent.category in interests) interests - intent.category else interests + intent.category).toImmutableSet())
            }
            ProfileSetupIntent.Continue -> setState {
                if (nameError == null && usernameError == null) copy(step = SetupStep.INTERESTS, attempted = true) else copy(attempted = true)
            }
            ProfileSetupIntent.Back -> setState { copy(step = SetupStep.PROFILE) }
            ProfileSetupIntent.Finish -> finish()
        }
    }

    private fun finish() {
        val draft = currentState
        if (draft.saving) return
        setState { copy(saving = true) }
        viewModelScope.launch {
            val now = clock.now()
            profiles.save(
                CreatorProfile(
                    displayName = draft.displayName.trim(),
                    username = ProfileRules.normalizeUsername(draft.username),
                    avatarPalette = draft.palette,
                    interests = draft.interests.map { it.name },
                    createdAtEpochMs = now,
                ),
            )
            // Written in the language the user set up in.
            notifications.add(
                InAppNotification(
                    id = "welcome",
                    kind = "welcome",
                    title = getString(Res.string.welcome_notification_title, AppConfig.APP_NAME),
                    body = getString(Res.string.welcome_notification_body),
                    createdAtEpochMs = now,
                ),
            )
            analytics.log(AnalyticsEvent.ProfileSetupCompleted)
            analytics.log(AnalyticsEvent.InterestsSelected(draft.interests.size))
        }
    }
}
