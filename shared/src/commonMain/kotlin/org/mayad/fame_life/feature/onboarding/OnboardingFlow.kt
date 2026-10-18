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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
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
    var startInSignIn by rememberSaveable { mutableStateOf(false) }
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
                IntroStep.WELCOME -> WelcomeStep(
                    onContinue = { startInSignIn = false; stepName = IntroStep.HOW_IT_WORKS.name },
                    onSignIn = { startInSignIn = true; stepName = IntroStep.ACCOUNT.name },
                )
                IntroStep.HOW_IT_WORKS -> HowItWorksStep(current.ordinal, total) { stepName = IntroStep.HONESTY.name }
                IntroStep.HONESTY -> HonestyStep(current.ordinal, total) { stepName = IntroStep.ACCOUNT.name }
                IntroStep.ACCOUNT -> AccountStep(current.ordinal, total, returning = startInSignIn)
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
        container.notifications.add(
            InAppNotification(
                id = "welcome",
                kind = "welcome",
                title = "Welcome to ${AppConfig.APP_NAME}",
                body = "Tap the glowing button to start your first live. Your audience is simulated and reacts to what you say.",
                createdAtEpochMs = now,
            ),
        )
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
                SetupStep.PROFILE -> ProfileStep(
                    displayName = displayName,
                    username = username,
                    palette = palette,
                    onName = { displayName = it },
                    onUsername = { username = it },
                    onPalette = { palette = it },
                    onContinue = { stepName = SetupStep.INTERESTS.name },
                )
                SetupStep.INTERESTS -> InterestsStep(
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

@Composable
private fun WelcomeStep(onContinue: () -> Unit, onSignIn: () -> Unit) {
    val colors = Pulse.colors
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)) {
        // A small stage of what's to come: a live card, floating comments and a gift.
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(0.66f)
                    .rotate(-4f)
                    .clip(Pulse.shapes.hero)
                    .border(1.dp, colors.borderStrong, Pulse.shapes.hero),
            ) {
                Artwork(ArtSpec(3, ArtMotif.PORTRAIT, 42), Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC070A14))))
                Row(Modifier.padding(Pulse.dimens.md), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
                    LiveBadge()
                    ViewerPill(12_480)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(Pulse.dimens.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FloatingComment("you're so funny 😂")
                    FloatingComment("wait, tell us more!!")
                    FloatingComment("هلا والله 👋")
                }
            }
            GiftArt("crown", Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 24.dp), size = 84.dp)
            GiftArt("heart", Modifier.align(Alignment.CenterStart).offset(x = 6.dp, y = 40.dp), size = 62.dp)
            GiftArt("spark", Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-30).dp), size = 54.dp)
        }
        Text(AppConfig.TAGLINE, style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.md))
        Text(
            "Start a live, talk about anything, and an AI audience reacts to what you actually say.",
            style = Pulse.type.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(Pulse.dimens.md))
        Text(
            "${AppConfig.APP_NAME} is a simulation. Viewers, comments, gifts and followers are generated in the app. " +
                "Your camera stays on your device and is never broadcast.",
            style = Pulse.type.bodySmall,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(Pulse.dimens.xl))
        PrimaryButton("Get started", onContinue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Pulse.dimens.sm))
        SecondaryButton("I already have an account", onSignIn, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Pulse.dimens.sm))
    }
}

@Composable
private fun FloatingComment(text: String) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Color.White,
        modifier = Modifier.clip(Pulse.shapes.bubble).background(Color(0x80101526)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun ProfileStep(
    displayName: String,
    username: String,
    palette: Int,
    onName: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPalette: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = Pulse.colors
    var attempted by rememberSaveable { mutableStateOf(false) }
    val nameError = ProfileRules.displayNameError(displayName)
    val usernameError = ProfileRules.usernameError(username)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Pulse.dimens.gutter),
    ) {
        Text("STEP 1 OF 2", style = Pulse.type.overline, color = colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text("Who's going live?", style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.xl))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CreatorAvatar(selfAvatar(palette), 120.dp, ring = AvatarRing.SELF)
        }
        Spacer(Modifier.height(Pulse.dimens.lg))
        Text("Pick your look", style = Pulse.type.meta, color = colors.textSecondary)
        Spacer(Modifier.height(Pulse.dimens.sm))
        AvatarPalettePicker(palette, onPalette)
        Spacer(Modifier.height(Pulse.dimens.xl))
        PulseTextField(displayName, onName, "Display name", placeholder = "Your name", error = if (attempted) nameError else null, maxLength = ProfileRules.DISPLAY_NAME_MAX)
        Spacer(Modifier.height(Pulse.dimens.lg))
        PulseTextField(
            username,
            { onUsername(it.trim().lowercase()) },
            "Username",
            placeholder = "yourname",
            prefix = "@",
            error = if (attempted || username.length >= ProfileRules.USERNAME_MIN) usernameError else null,
            maxLength = ProfileRules.USERNAME_MAX + 1,
        )
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text("Saved to your account so it follows you to any device.", style = Pulse.type.meta, color = colors.textMuted)
        Spacer(Modifier.height(Pulse.dimens.xxl))
        PrimaryButton(
            "Continue",
            onClick = {
                attempted = true
                if (nameError == null && usernameError == null) onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsStep(selected: List<String>, onToggle: (String) -> Unit, onFinish: () -> Unit) {
    val colors = Pulse.colors
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)) {
        Text("STEP 2 OF 2", style = Pulse.type.overline, color = colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text("What do you love talking about?", style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text("We'll tune your feed. You can skip this.", style = Pulse.type.body, color = colors.textSecondary)
        Spacer(Modifier.height(Pulse.dimens.xl))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            Category.entries.forEach { category ->
                PulseChip(category.label, selected = category.name in selected, onClick = { onToggle(category.name) }, leading = category.glyph())
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(if (selected.isEmpty()) "Skip for now" else "Let's go", onFinish, Modifier.fillMaxWidth(), leading = Glyphs.Bolt)
        Spacer(Modifier.height(Pulse.dimens.sm))
    }
}
