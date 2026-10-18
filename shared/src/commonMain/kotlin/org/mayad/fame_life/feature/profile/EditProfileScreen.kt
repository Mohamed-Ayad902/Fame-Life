package org.mayad.fame_life.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.ScreenHeader
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.domain.profile.ProfileRules

@Composable
fun EditProfileScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    // Without a profile the app shows onboarding instead of this screen, so there is nothing to edit here.
    val profile = container.profiles.profile.value ?: return
    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var palette by rememberSaveable { mutableStateOf(profile.avatarPalette) }
    val nameError = ProfileRules.displayNameError(displayName)
    val usernameError = ProfileRules.usernameError(username)
    val changed = displayName.trim() != profile.displayName ||
        ProfileRules.normalizeUsername(username) != profile.username ||
        bio.trim() != profile.bio ||
        palette != profile.avatarPalette
    val colors = Pulse.colors

    AtmosphereBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            ScreenHeader("Edit profile", onBack = { navigator.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)) {
                Spacer(Modifier.height(Space.lg))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CreatorAvatar(selfAvatar(palette), 112.dp, ring = AvatarRing.SELF)
                }
                Spacer(Modifier.height(Space.lg))
                AvatarPalettePicker(palette, { palette = it })
                Spacer(Modifier.height(Space.xl))
                PulseTextField(displayName, { displayName = it }, "Display name", error = nameError, maxLength = ProfileRules.DISPLAY_NAME_MAX)
                Spacer(Modifier.height(Space.lg))
                PulseTextField(
                    username,
                    { username = it.trim().lowercase() },
                    "Username",
                    prefix = "@",
                    error = usernameError,
                    maxLength = ProfileRules.USERNAME_MAX + 1,
                )
                Spacer(Modifier.height(Space.lg))
                PulseTextField(
                    bio,
                    { bio = it },
                    "Bio",
                    placeholder = "What do you go live about?",
                    singleLine = false,
                    maxLength = ProfileRules.BIO_MAX,
                )
                Spacer(Modifier.height(4.dp))
                Text("${bio.length}/${ProfileRules.BIO_MAX}", style = Pulse.type.meta, color = colors.textMuted)
                Spacer(Modifier.height(Space.xxl))
                PrimaryButton(
                    "Save",
                    enabled = changed && nameError == null && usernameError == null,
                    onClick = {
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}
