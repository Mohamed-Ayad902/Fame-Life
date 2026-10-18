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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
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
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.domain.profile.ProfileRules

/** Tier 1: holds the draft and saves it through the profile repository (which mirrors it to the account). */
@Composable
fun EditProfileScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    // Without a profile the app shows onboarding instead of this screen, so there is nothing to edit here.
    val profile = container.profiles.profile.value ?: return
    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var palette by rememberSaveable { mutableStateOf(profile.avatarPalette) }
    EditProfileContent(
        original = profile,
        draft = EditDraft(displayName, username, bio, palette),
        onName = { displayName = it },
        onUsername = { username = it.trim().lowercase() },
        onBio = { bio = it },
        onPalette = { palette = it },
        onBack = { navigator.pop() },
        onSave = {
            container.profiles.save(
                profile.copy(
                    displayName = displayName.trim(),
                    username = ProfileRules.normalizeUsername(username),
                    bio = bio.trim(),
                    avatarPalette = palette,
                ),
            )
            navigator.pop()
        },
    )
}

data class EditDraft(val displayName: String, val username: String, val bio: String, val palette: Int)

/** Tier 2: stateless form. Wide windows show the avatar picker beside the fields. */
@Composable
fun EditProfileContent(
    original: CreatorProfile,
    draft: EditDraft,
    onName: (String) -> Unit,
    onUsername: (String) -> Unit,
    onBio: (String) -> Unit,
    onPalette: (Int) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val dimens = Pulse.dimens
    val nameError = ProfileRules.displayNameError(draft.displayName)
    val usernameError = ProfileRules.usernameError(draft.username)
    val changed = draft.displayName.trim() != original.displayName ||
        ProfileRules.normalizeUsername(draft.username) != original.username ||
        draft.bio.trim() != original.bio ||
        draft.palette != original.avatarPalette
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
            error = nameError?.message(ProfileRules.DISPLAY_NAME_MAX),
            maxLength = ProfileRules.DISPLAY_NAME_MAX,
        )
        Spacer(Modifier.height(dimens.lg))
        PulseTextField(
            draft.username,
            onUsername,
            stringResource(Res.string.setup_username),
            prefix = "@",
            error = usernameError?.message(ProfileRules.USERNAME_MAX),
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
            enabled = changed && nameError == null && usernameError == null,
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
    val p = SampleData.profile
    EditProfileContent(p, EditDraft(p.displayName, "a", p.bio, 5), {}, {}, {}, {}, {}, {})
}
