package org.mayad.fame_life.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.edit_bio
import famelife.shared.generated.resources.edit_bio_count
import famelife.shared.generated.resources.edit_bio_hint
import famelife.shared.generated.resources.edit_save
import famelife.shared.generated.resources.edit_title
import famelife.shared.generated.resources.setup_display_name
import famelife.shared.generated.resources.setup_username
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.core.presentation.CollectEvents
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.ScreenHeader
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.message
import org.mayad.fame_life.domain.profile.ProfileRules

/** Tier 1: connects the edit form to its ViewModel and leaves once the profile is saved. */
@Composable
fun EditProfileScreen(onDone: () -> Unit, viewModel: EditProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            EditProfileEvent.Saved -> onDone()
        }
    }
    // Without a profile the app shows onboarding instead of this screen, so there is nothing to edit here.
    if (state.original == null) return
    EditProfileContent(
        state = state,
        onName = { viewModel.onIntent(EditProfileIntent.Name(it)) },
        onUsername = { viewModel.onIntent(EditProfileIntent.Username(it)) },
        onBio = { viewModel.onIntent(EditProfileIntent.Bio(it)) },
        onPalette = { viewModel.onIntent(EditProfileIntent.Palette(it)) },
        onBack = onDone,
        onSave = { viewModel.onIntent(EditProfileIntent.Save) },
    )
}

/** Tier 2: stateless form. Wide windows show the avatar picker beside the fields. */
@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onName: (String) -> Unit,
    onUsername: (String) -> Unit,
    onBio: (String) -> Unit,
    onPalette: (Int) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val dimens = Pulse.dimens
    val draft = state
    val look: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CreatorAvatar(selfAvatar(draft.palette), dimens.avatarXl, ring = AvatarRing.SELF)
        }
        Spacer(Modifier.height(dimens.lg))
        AvatarPalettePicker(draft.palette, onPalette)
    }
    val fields: @Composable ColumnScope.() -> Unit = {
        PulseTextField(
            draft.displayName,
            onName,
            stringResource(Res.string.setup_display_name),
            error = state.nameError?.message(ProfileRules.DISPLAY_NAME_MAX),
            maxLength = ProfileRules.DISPLAY_NAME_MAX,
        )
        Spacer(Modifier.height(dimens.lg))
        PulseTextField(
            draft.username,
            onUsername,
            stringResource(Res.string.setup_username),
            prefix = "@",
            error = state.usernameError?.message(ProfileRules.USERNAME_MAX),
            maxLength = ProfileRules.USERNAME_MAX + 1,
        )
        Spacer(Modifier.height(dimens.lg))
        PulseTextField(
            draft.bio,
            onBio,
            stringResource(Res.string.edit_bio),
            placeholder = stringResource(Res.string.edit_bio_hint),
            singleLine = false,
            maxLength = ProfileRules.BIO_MAX,
        )
        Spacer(Modifier.height(dimens.xs))
        Text(stringResource(Res.string.edit_bio_count, draft.bio.length, ProfileRules.BIO_MAX), style = Pulse.type.meta, color = Pulse.colors.textMuted)
        Spacer(Modifier.height(dimens.xxl))
        PrimaryButton(
            stringResource(Res.string.edit_save),
            enabled = state.canSave,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimens.xxl))
    }
    AtmosphereBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            ScreenHeader(stringResource(Res.string.edit_title), onBack = onBack)
            if (Pulse.width.split) {
                SplitPanes(
                    Modifier.padding(horizontal = dimens.gutter),
                    leading = { Column(content = look) },
                    trailing = { Column(Modifier.verticalScroll(rememberScrollState()), content = fields) },
                )
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = dimens.gutter)) {
                    Spacer(Modifier.height(dimens.lg))
                    look()
                    Spacer(Modifier.height(dimens.xl))
                    fields()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun EditProfilePreview() = PreviewContainer {
    EditProfileContent(EditProfileUiState(SampleData.profile, username = "a", palette = 5), {}, {}, {}, {}, {}, {})
}
