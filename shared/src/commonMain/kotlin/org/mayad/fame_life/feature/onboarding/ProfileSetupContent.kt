package org.mayad.fame_life.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.setup_continue
import famelife.shared.generated.resources.setup_display_name
import famelife.shared.generated.resources.setup_display_name_hint
import famelife.shared.generated.resources.setup_interests_body
import famelife.shared.generated.resources.setup_interests_title
import famelife.shared.generated.resources.setup_lets_go
import famelife.shared.generated.resources.setup_pick_look
import famelife.shared.generated.resources.setup_profile_title
import famelife.shared.generated.resources.setup_saved_note
import famelife.shared.generated.resources.setup_skip
import famelife.shared.generated.resources.setup_step_one
import famelife.shared.generated.resources.setup_step_two
import famelife.shared.generated.resources.setup_username
import famelife.shared.generated.resources.setup_username_hint
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.message
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.domain.profile.ProfileRules
import org.mayad.fame_life.feature.home.glyph
import org.mayad.fame_life.feature.profile.AvatarPalettePicker
import org.mayad.fame_life.feature.profile.selfAvatar

/** Step 1 of setup: the avatar look, display name and username. Wide windows show the avatar beside the form. */
@Composable
fun ProfileStepContent(state: ProfileSetupUiState, onIntent: (ProfileSetupIntent) -> Unit) {
    val outer = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
        .padding(Pulse.dimens.gutter)
    if (Pulse.width.split) {
        SplitPanes(
            outer,
            leading = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SetupHeading(stringResource(Res.string.setup_step_one), stringResource(Res.string.setup_profile_title))
                    Spacer(Modifier.height(Pulse.dimens.xl))
                    SetupAvatar(state.palette)
                }
            },
            trailing = { Column(Modifier.verticalScroll(rememberScrollState())) { ProfileForm(state, onIntent) } },
        )
    } else {
        Column(outer.verticalScroll(rememberScrollState())) {
            SetupHeading(stringResource(Res.string.setup_step_one), stringResource(Res.string.setup_profile_title))
            Spacer(Modifier.height(Pulse.dimens.xl))
            SetupAvatar(state.palette)
            Spacer(Modifier.height(Pulse.dimens.lg))
            ProfileForm(state, onIntent)
        }
    }
}

@Composable
private fun SetupAvatar(palette: Int) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CreatorAvatar(selfAvatar(palette), Pulse.dimens.avatarXl, ring = AvatarRing.SELF)
    }
}

@Composable
private fun ColumnScope.ProfileForm(state: ProfileSetupUiState, onIntent: (ProfileSetupIntent) -> Unit) {
    val dimens = Pulse.dimens
    Text(stringResource(Res.string.setup_pick_look), style = Pulse.type.meta, color = Pulse.colors.textSecondary)
    Spacer(Modifier.height(dimens.sm))
    AvatarPalettePicker(state.palette, { onIntent(ProfileSetupIntent.Palette(it)) })
    Spacer(Modifier.height(dimens.xl))
    PulseTextField(
        state.displayName,
        { onIntent(ProfileSetupIntent.Name(it)) },
        stringResource(Res.string.setup_display_name),
        placeholder = stringResource(Res.string.setup_display_name_hint),
        error = if (state.attempted) state.nameError?.message(ProfileRules.DISPLAY_NAME_MAX) else null,
        maxLength = ProfileRules.DISPLAY_NAME_MAX,
    )
    Spacer(Modifier.height(dimens.lg))
    PulseTextField(
        state.username,
        { onIntent(ProfileSetupIntent.Username(it)) },
        stringResource(Res.string.setup_username),
        placeholder = stringResource(Res.string.setup_username_hint),
        prefix = "@",
        // Typing a full username shows its problems early; a half-typed one waits for "Continue".
        error = if (state.attempted || state.username.length >= ProfileRules.USERNAME_MIN) state.usernameError?.message(ProfileRules.USERNAME_MAX) else null,
        maxLength = ProfileRules.USERNAME_MAX + 1,
    )
    Spacer(Modifier.height(dimens.sm))
    Text(stringResource(Res.string.setup_saved_note), style = Pulse.type.meta, color = Pulse.colors.textMuted)
    Spacer(Modifier.height(dimens.xxl))
    PrimaryButton(stringResource(Res.string.setup_continue), onClick = { onIntent(ProfileSetupIntent.Continue) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SetupHeading(step: String, title: String) {
    Column {
        Text(step, style = Pulse.type.overline, color = Pulse.colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text(title, style = Pulse.type.display, color = Pulse.colors.textPrimary)
    }
}

/** Step 2 of setup: optional interests that tune the feed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsStepContent(state: ProfileSetupUiState, onIntent: (ProfileSetupIntent) -> Unit) {
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(dimens.gutter)) {
        SetupHeading(stringResource(Res.string.setup_step_two), stringResource(Res.string.setup_interests_title))
        Spacer(Modifier.height(dimens.sm))
        Text(stringResource(Res.string.setup_interests_body), style = Pulse.type.body, color = Pulse.colors.textSecondary)
        Spacer(Modifier.height(dimens.xl))
        FlowRow(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.sm),
            verticalArrangement = Arrangement.spacedBy(dimens.sm),
        ) {
            Category.entries.forEach { category ->
                PulseChip(
                    category.label(),
                    selected = category in state.interests,
                    onClick = { onIntent(ProfileSetupIntent.ToggleInterest(category)) },
                    leading = category.glyph(),
                    modifier = Modifier.cascadeIn(cascade, category.name),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            stringResource(if (state.interests.isEmpty()) Res.string.setup_skip else Res.string.setup_lets_go),
            onClick = { onIntent(ProfileSetupIntent.Finish) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.saving,
            leading = Glyphs.Bolt,
        )
        Spacer(Modifier.height(dimens.sm))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun ProfileStepPreview() = PreviewContainer {
    AtmosphereBackground { ProfileStepContent(ProfileSetupUiState(displayName = "Maya", username = "a", palette = 3, attempted = true)) {} }
}

@ScreenPreviews
@Composable
private fun InterestsStepPreview() = PreviewContainer {
    AtmosphereBackground { InterestsStepContent(ProfileSetupUiState(interests = persistentSetOf(Category.COMEDY))) {} }
}
