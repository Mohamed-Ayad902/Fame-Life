package org.mayad.fame_life.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.app_tagline
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
import famelife.shared.generated.resources.welcome_body
import famelife.shared.generated.resources.welcome_disclosure
import famelife.shared.generated.resources.welcome_get_started
import famelife.shared.generated.resources.welcome_have_account
import famelife.shared.generated.resources.welcome_notification_body
import famelife.shared.generated.resources.welcome_notification_title
import famelife.shared.generated.resources.welcome_sample_comment_1
import famelife.shared.generated.resources.welcome_sample_comment_2
import famelife.shared.generated.resources.welcome_sample_comment_3
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseAspect
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.message
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.nowEpochMs
import org.mayad.fame_life.domain.profile.ProfileRules
import org.mayad.fame_life.feature.home.glyph
import org.mayad.fame_life.feature.profile.AvatarPalettePicker
import org.mayad.fame_life.feature.profile.selfAvatar

private enum class IntroStep { WELCOME, HOW_IT_WORKS, HONESTY, ACCOUNT }

/** The welcome card's tilt, in degrees. */
private const val STAGE_TILT = -4f
private const val STAGE_WIDTH_FRACTION = 0.68f
private const val WELCOME_ART = 42

/**
 * First run while signed out: welcome, how a live works, what's simulated, then the account step. Signing in ends
 * this flow from the outside (App switches on auth state), so there is no "finish" here.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingFlow(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val reduceMotion = Pulse.reducedMotion
    var stepName by rememberSaveable { mutableStateOf(IntroStep.WELCOME.name) }
    var returning by rememberSaveable { mutableStateOf(false) }
    val step = IntroStep.valueOf(stepName)
    val total = IntroStep.entries.size

    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.OnboardingStarted) }
    BackHandler(enabled = step != IntroStep.WELCOME) { stepName = IntroStep.entries[step.ordinal - 1].name }

    AtmosphereBackground(modifier) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { stepTransition(forward = targetState.ordinal > initialState.ordinal, reduceMotion) },
            label = "onboarding",
        ) { current ->
            when (current) {
                IntroStep.WELCOME -> WelcomeContent(
                    onContinue = { returning = false; stepName = IntroStep.HOW_IT_WORKS.name },
                    onSignIn = { returning = true; stepName = IntroStep.ACCOUNT.name },
                )
                IntroStep.HOW_IT_WORKS -> HowItWorksStep(current.ordinal, total) { stepName = IntroStep.HONESTY.name }
                IntroStep.HONESTY -> HonestyStep(current.ordinal, total) { stepName = IntroStep.ACCOUNT.name }
                IntroStep.ACCOUNT -> AccountStep(current.ordinal, total, returning = returning)
            }
        }
    }
}

private enum class SetupStep { PROFILE, INTERESTS }

/** After a new account is created: who you are on stage, then optional interests. Saving the profile ends it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfileSetupFlow(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val reduceMotion = Pulse.reducedMotion
    var stepName by rememberSaveable { mutableStateOf(SetupStep.PROFILE.name) }
    val step = SetupStep.valueOf(stepName)
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var palette by rememberSaveable { mutableStateOf(0) }
    val interests = remember { mutableStateListOf<String>() }
    // Resolved here so the saved notification reads in the language the user set up in.
    val welcomeTitle = stringResource(Res.string.welcome_notification_title, AppConfig.APP_NAME)
    val welcomeBody = stringResource(Res.string.welcome_notification_body)

    BackHandler(enabled = step == SetupStep.INTERESTS) { stepName = SetupStep.PROFILE.name }

    fun finish() {
        val now = nowEpochMs()
        container.profiles.save(
            CreatorProfile(
                displayName = displayName.trim(),
                username = ProfileRules.normalizeUsername(username),
                avatarPalette = palette,
                interests = interests.toList(),
                createdAtEpochMs = now,
            ),
        )
        container.notifications.add(InAppNotification(id = "welcome", kind = "welcome", title = welcomeTitle, body = welcomeBody, createdAtEpochMs = now))
        container.analytics.log(AnalyticsEvent.ProfileSetupCompleted)
        container.analytics.log(AnalyticsEvent.InterestsSelected(interests.size))
    }

    AtmosphereBackground(modifier) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { stepTransition(forward = targetState.ordinal > initialState.ordinal, reduceMotion) },
            label = "profile-setup",
        ) { current ->
            when (current) {
                SetupStep.PROFILE -> ProfileStepContent(
                    displayName = displayName,
                    username = username,
                    palette = palette,
                    onName = { displayName = it },
                    onUsername = { username = it },
                    onPalette = { palette = it },
                    onContinue = { stepName = SetupStep.INTERESTS.name },
                )
                SetupStep.INTERESTS -> InterestsStepContent(
                    selected = interests,
                    onToggle = { if (it in interests) interests.remove(it) else interests.add(it) },
                    onFinish = ::finish,
                )
            }
        }
    }
}

private fun stepTransition(forward: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit())
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(Motion.slide) { it * direction / 5 } + fadeIn(Motion.enter())) togetherWith fadeOut(Motion.exit())
}

// ---------------------------------------------------------------------------------------------------------------
// Welcome

@Composable
internal fun WelcomeContent(onContinue: () -> Unit, onSignIn: () -> Unit) {
    val padding = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)
    if (Pulse.width.split) {
        SplitPanes(
            padding,
            leading = { WelcomeStage(Modifier.fillMaxHeight()) },
            trailing = { Column { WelcomeCopy(onContinue, onSignIn) } },
        )
    } else {
        Column(padding) {
            WelcomeStage(Modifier.fillMaxWidth().weight(1f))
            WelcomeCopy(onContinue, onSignIn)
        }
    }
}

/** A small stage of what's to come: a tilted live card with floating comments and gifts around it. */
@Composable
private fun WelcomeStage(modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(STAGE_WIDTH_FRACTION)
                .aspectRatio(PulseAspect.Phone)
                .rotate(STAGE_TILT)
                .clip(Pulse.shapes.hero)
                .border(dimens.hairline, colors.borderStrong, Pulse.shapes.hero),
        ) {
            Artwork(ArtSpec(3, ArtMotif.PORTRAIT, WELCOME_ART), Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.mediaScrim)))
            Row(Modifier.padding(dimens.md), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
                LiveBadge()
                ViewerPill(12_480)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(dimens.md), verticalArrangement = Arrangement.spacedBy(dimens.xs)) {
                FloatingComment(stringResource(Res.string.welcome_sample_comment_1))
                FloatingComment(stringResource(Res.string.welcome_sample_comment_2))
                FloatingComment(stringResource(Res.string.welcome_sample_comment_3))
            }
        }
        GiftArt("crown", Modifier.align(Alignment.TopEnd).offset(x = -dimens.md, y = dimens.xl), size = dimens.tileXxl)
        GiftArt("heart", Modifier.align(Alignment.CenterStart).offset(x = dimens.xs, y = dimens.xxxl), size = dimens.tileXl)
        GiftArt("spark", Modifier.align(Alignment.BottomEnd).offset(x = -dimens.xl, y = -dimens.xxl), size = dimens.tileLg)
    }
}

@Composable
private fun ColumnScope.WelcomeCopy(onContinue: () -> Unit, onSignIn: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    Text(stringResource(Res.string.app_tagline), style = Pulse.type.display, color = colors.textPrimary, modifier = Modifier.cascadeIn(cascade, "tagline"))
    Spacer(Modifier.height(dimens.md))
    Text(stringResource(Res.string.welcome_body), style = Pulse.type.body, color = colors.textSecondary, modifier = Modifier.cascadeIn(cascade, "body"))
    Spacer(Modifier.height(dimens.md))
    Text(
        stringResource(Res.string.welcome_disclosure, AppConfig.APP_NAME),
        style = Pulse.type.bodySmall,
        color = colors.textMuted,
        modifier = Modifier.cascadeIn(cascade, "disclosure"),
    )
    Spacer(Modifier.height(dimens.xl))
    PrimaryButton(stringResource(Res.string.welcome_get_started), onContinue, Modifier.fillMaxWidth().cascadeIn(cascade, "start"))
    Spacer(Modifier.height(dimens.sm))
    SecondaryButton(stringResource(Res.string.welcome_have_account), onSignIn, Modifier.fillMaxWidth().cascadeIn(cascade, "signin"))
    Spacer(Modifier.height(dimens.sm))
}

@Composable
private fun FloatingComment(text: String) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.onMedia,
        modifier = Modifier
            .clip(Pulse.shapes.bubble)
            .background(Pulse.colors.glass)
            .padding(horizontal = Pulse.dimens.sm, vertical = Pulse.dimens.xs),
    )
}

// ---------------------------------------------------------------------------------------------------------------
// Profile setup

@Composable
internal fun ProfileStepContent(
    displayName: String,
    username: String,
    palette: Int,
    onName: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPalette: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    var attempted by rememberSaveable { mutableStateOf(false) }
    val nameError = ProfileRules.displayNameError(displayName)
    val usernameError = ProfileRules.usernameError(username)
    val submit = {
        attempted = true
        if (nameError == null && usernameError == null) onContinue()
    }
    val outer = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
        .padding(Pulse.dimens.gutter)
    val avatar: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CreatorAvatar(selfAvatar(palette), Pulse.dimens.avatarXl, ring = AvatarRing.SELF)
        }
    }
    val form: @Composable ColumnScope.() -> Unit = {
        Text(stringResource(Res.string.setup_pick_look), style = Pulse.type.meta, color = Pulse.colors.textSecondary)
        Spacer(Modifier.height(Pulse.dimens.sm))
        AvatarPalettePicker(palette, onPalette)
        Spacer(Modifier.height(Pulse.dimens.xl))
        PulseTextField(
            displayName,
            onName,
            stringResource(Res.string.setup_display_name),
            placeholder = stringResource(Res.string.setup_display_name_hint),
            error = if (attempted) nameError?.message(ProfileRules.DISPLAY_NAME_MAX) else null,
            maxLength = ProfileRules.DISPLAY_NAME_MAX,
        )
        Spacer(Modifier.height(Pulse.dimens.lg))
        PulseTextField(
            username,
            { onUsername(it.trim().lowercase()) },
            stringResource(Res.string.setup_username),
            placeholder = stringResource(Res.string.setup_username_hint),
            prefix = "@",
            error = if (attempted || username.length >= ProfileRules.USERNAME_MIN) usernameError?.message(ProfileRules.USERNAME_MAX) else null,
            maxLength = ProfileRules.USERNAME_MAX + 1,
        )
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(stringResource(Res.string.setup_saved_note), style = Pulse.type.meta, color = Pulse.colors.textMuted)
        Spacer(Modifier.height(Pulse.dimens.xxl))
        PrimaryButton(stringResource(Res.string.setup_continue), onClick = submit, modifier = Modifier.fillMaxWidth())
    }
    if (Pulse.width.split) {
        SplitPanes(
            outer,
            leading = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SetupHeading(stringResource(Res.string.setup_step_one), stringResource(Res.string.setup_profile_title))
                    Spacer(Modifier.height(Pulse.dimens.xl))
                    avatar()
                }
            },
            trailing = { Column(Modifier.verticalScroll(rememberScrollState()), content = form) },
        )
    } else {
        Column(outer.verticalScroll(rememberScrollState())) {
            SetupHeading(stringResource(Res.string.setup_step_one), stringResource(Res.string.setup_profile_title))
            Spacer(Modifier.height(Pulse.dimens.xl))
            avatar()
            Spacer(Modifier.height(Pulse.dimens.lg))
            form()
        }
    }
}

@Composable
private fun SetupHeading(step: String, title: String) {
    Column {
        Text(step, style = Pulse.type.overline, color = Pulse.colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text(title, style = Pulse.type.display, color = Pulse.colors.textPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InterestsStepContent(selected: List<String>, onToggle: (String) -> Unit, onFinish: () -> Unit) {
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
                    selected = category.name in selected,
                    onClick = { onToggle(category.name) },
                    leading = category.glyph(),
                    modifier = Modifier.cascadeIn(cascade, category.name),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            stringResource(if (selected.isEmpty()) Res.string.setup_skip else Res.string.setup_lets_go),
            onFinish,
            Modifier.fillMaxWidth(),
            leading = Glyphs.Bolt,
        )
        Spacer(Modifier.height(dimens.sm))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun WelcomePreview() = PreviewContainer { AtmosphereBackground { WelcomeContent({}, {}) } }

@ScreenPreviews
@Composable
private fun ProfileStepPreview() = PreviewContainer {
    AtmosphereBackground { ProfileStepContent("Maya", "maya", 3, {}, {}, {}, {}) }
}

@ScreenPreviews
@Composable
private fun InterestsStepPreview() = PreviewContainer {
    AtmosphereBackground { InterestsStepContent(listOf(Category.COMEDY.name), {}, {}) }
}
