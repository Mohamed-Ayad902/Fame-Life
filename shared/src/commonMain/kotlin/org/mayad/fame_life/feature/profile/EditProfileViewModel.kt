package org.mayad.fame_life.feature.profile

import androidx.compose.runtime.Immutable
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.domain.profile.ProfileRules

/** The editable fields. Validation and "changed" are derived, so the screen never computes them. */
@Immutable
data class EditProfileUiState(
    val original: CreatorProfile?,
    val displayName: String = original?.displayName.orEmpty(),
    val username: String = original?.username.orEmpty(),
    val bio: String = original?.bio.orEmpty(),
    val palette: Int = original?.avatarPalette ?: 0,
) {
    val nameError get() = ProfileRules.displayNameError(displayName)
    val usernameError get() = ProfileRules.usernameError(username)

    val changed: Boolean
        get() = original != null && (
            displayName.trim() != original.displayName ||
                ProfileRules.normalizeUsername(username) != original.username ||
                bio.trim() != original.bio ||
                palette != original.avatarPalette
            )

    val canSave: Boolean get() = changed && nameError == null && usernameError == null
}

sealed interface EditProfileIntent {
    data class Name(val value: String) : EditProfileIntent
    data class Username(val value: String) : EditProfileIntent
    data class Bio(val value: String) : EditProfileIntent
    data class Palette(val value: Int) : EditProfileIntent
    data object Save : EditProfileIntent
}

sealed interface EditProfileEvent {
    data object Saved : EditProfileEvent
}

/** Holds the draft across rotation and process-free recomposition; saving goes through the profile repository. */
class EditProfileViewModel(
    private val profiles: ProfileRepository,
) : MviViewModel<EditProfileUiState, EditProfileIntent, EditProfileEvent>(EditProfileUiState(profiles.profile.value)) {

    override fun onIntent(intent: EditProfileIntent) {
        when (intent) {
            is EditProfileIntent.Name -> setState { copy(displayName = intent.value) }
            is EditProfileIntent.Username -> setState { copy(username = intent.value.trim().lowercase()) }
            is EditProfileIntent.Bio -> setState { copy(bio = intent.value) }
            is EditProfileIntent.Palette -> setState { copy(palette = intent.value) }
            EditProfileIntent.Save -> save()
        }
    }

    private fun save() {
        val state = currentState
        val original = state.original ?: return
        if (!state.canSave) return
        profiles.save(
            original.copy(
                displayName = state.displayName.trim(),
                username = ProfileRules.normalizeUsername(state.username),
                bio = state.bio.trim(),
                avatarPalette = state.palette,
            ),
        )
        emit(EditProfileEvent.Saved)
    }
}
