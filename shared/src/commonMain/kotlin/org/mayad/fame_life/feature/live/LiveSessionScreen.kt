package org.mayad.fame_life.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.live_ai_budget
import famelife.shared.generated.resources.live_ai_on_device
import famelife.shared.generated.resources.live_ai_reacting
import famelife.shared.generated.resources.live_camera_off
import famelife.shared.generated.resources.live_cant_hear
import famelife.shared.generated.resources.live_end
import famelife.shared.generated.resources.live_end_body
import famelife.shared.generated.resources.live_end_live
import famelife.shared.generated.resources.live_end_title
import famelife.shared.generated.resources.live_flip
import famelife.shared.generated.resources.live_keep_going
import famelife.shared.generated.resources.live_level_body
import famelife.shared.generated.resources.live_level_title
import famelife.shared.generated.resources.live_listening
import famelife.shared.generated.resources.live_mic_denied
import famelife.shared.generated.resources.live_mic_muted
import famelife.shared.generated.resources.live_mic_off
import famelife.shared.generated.resources.live_mute
import famelife.shared.generated.resources.live_pause
import famelife.shared.generated.resources.live_paused_body
import famelife.shared.generated.resources.live_paused_title
import famelife.shared.generated.resources.live_recap_body
import famelife.shared.generated.resources.live_recap_title
import famelife.shared.generated.resources.live_resume
import famelife.shared.generated.resources.live_resume_live
import famelife.shared.generated.resources.live_simulated_audience
import famelife.shared.generated.resources.live_simulated_viewers
import famelife.shared.generated.resources.live_speech_unavailable
import famelife.shared.generated.resources.live_starting_mic
import famelife.shared.generated.resources.live_time_left
import famelife.shared.generated.resources.live_unmute
import famelife.shared.generated.resources.live_your_live
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppContainer
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AnimatedCount
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.clock
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.platform.CameraLens
import org.mayad.fame_life.core.platform.CameraPreview
import org.mayad.fame_life.core.platform.isCameraPreviewSupported
import org.mayad.fame_life.core.platform.monotonicMillis
import org.mayad.fame_life.core.platform.rememberLivePermissions
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.titleRes
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.nowEpochMs
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.progression.FameLevel
import org.mayad.fame_life.domain.simulation.AiActivity
import org.mayad.fame_life.domain.simulation.LiveSessionController
import org.mayad.fame_life.domain.simulation.LiveSessionState
import org.mayad.fame_life.domain.simulation.SessionStatus
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.domain.simulation.SimulationConfig
import org.mayad.fame_life.feature.notifications.NotificationKind
import org.mayad.fame_life.feature.profile.selfAvatar

private const val COUNTDOWN_FROM = 3
private const val ENDING_SOON_SECONDS = 60
private const val ABANDONED_SECONDS = 15
private const val CHAT_WIDTH_FRACTION = 0.8f
private const val REACTIONS_HEIGHT_FRACTION = 0.7f

/**
 * Tier 1: the live scene. The controller owns the session; this screen renders its state, forwards the creator's
 * controls, and records the result once. Leaving the composition for any reason ends the session and releases
 * the microphone.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveSessionScreen(sessionId: String, config: SimulationConfig, navigator: Navigator) {
    val container = LocalAppContainer.current
    val profile = container.profiles.profile.collectAsState().value ?: return
    val limits = container.entitlements.entitlements.collectAsState().value.limits
    val permissions = rememberLivePermissions()
    val scope = rememberCoroutineScope()
    val controller = remember(sessionId) {
        LiveSessionController(
            sessionId = sessionId,
            config = config,
            limits = limits,
            speechInput = container.speechInput,
            remote = container.audienceGenerator,
            scope = scope,
            clock = ::monotonicMillis,
            epochClock = ::nowEpochMs,
        )
    }
    val state by controller.state.collectAsState()
    var countdown by remember { mutableStateOf(COUNTDOWN_FROM) }
    var lens by remember { mutableStateOf(CameraLens.FRONT) }
    var confirmEnd by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    val reduceMotion = Pulse.reducedMotion

    DisposableEffect(controller) { onDispose { controller.end() } }
    LaunchedEffect(controller) {
        for (n in COUNTDOWN_FROM downTo 1) {
            countdown = n
            delay((if (reduceMotion) Motion.COUNTDOWN_STEP_REDUCED else Motion.COUNTDOWN_STEP).toLong())
        }
        countdown = 0
        controller.start()
    }
    // Leaving the app pauses the room. The creator resumes explicitly, so nothing happens while they are away.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.pause() }
    BackHandler(enabled = state.summary == null) { confirmEnd = true }

    LaunchedEffect(state.summary) {
        val summary = state.summary ?: return@LaunchedEffect
        if (recorded) return@LaunchedEffect
        recorded = true
        recordSession(container, summary, state, limits)
        navigator.replace(Route.Summary(summary.id))
    }

    val cameraOn = permissions.camera && isCameraPreviewSupported
    LiveSessionContent(
        state = state,
        profile = profile,
        style = LiveStyles.byId(config.liveStyleId),
        cameraOn = cameraOn,
        lens = lens,
        countdown = countdown,
        onClose = { confirmEnd = true },
        onMic = { controller.setMicMuted(!state.micMuted) },
        onFlip = { lens = if (lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT },
        onPause = { if (state.live.status == SessionStatus.PAUSED) controller.resume() else controller.pause() },
        onResume = controller::resume,
    )

    if (confirmEnd) {
        val colors = Pulse.colors
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = colors.surfaceElevated,
            title = { Text(stringResource(Res.string.live_end_title), style = Pulse.type.title, color = colors.textPrimary) },
            text = { Text(stringResource(Res.string.live_end_body), style = Pulse.type.body, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    if (state.live.status == SessionStatus.READY) navigator.pop() else controller.end()
                }) { Text(stringResource(Res.string.live_end_live), style = Pulse.type.label, color = colors.live) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text(stringResource(Res.string.live_keep_going), style = Pulse.type.label, color = colors.textPrimary) }
            },
        )
    }
}

/**
 * Tier 2: stateless live scene. Camera fills the screen; on wide windows the chat keeps a readable width and the
 * controls sit in a column at the end so the face stays in the middle.
 */
@Composable
fun LiveSessionContent(
    state: LiveSessionState,
    profile: CreatorProfile,
    style: LiveStyle,
    cameraOn: Boolean,
    lens: CameraLens,
    countdown: Int,
    onClose: () -> Unit,
    onMic: () -> Unit,
    onFlip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val live = state.live
    val paused = live.status == SessionStatus.PAUSED
    Box(Modifier.fillMaxSize().background(colors.cameraBase)) {
        if (cameraOn) CameraPreview(lens, Modifier.fillMaxSize()) else CameraOffBackdrop(profile, style)
        // Scrims keep text readable over any camera image without covering the face in the middle.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to colors.mediaScrimSoft, 0.18f to Color.Transparent, 0.55f to Color.Transparent, 1f to colors.mediaScrim),
            ),
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            LiveTopBar(profile, live.elapsedSeconds, live.remainingSeconds, live.viewers, onClose)
            Row(Modifier.padding(horizontal = dimens.lg), horizontalArrangement = Arrangement.spacedBy(dimens.xs)) {
                StatusChip(stringResource(Res.string.live_simulated_audience), Glyphs.Sparkles, colors.cyan)
                aiChip(state.ai)?.let { (text, tint) -> StatusChip(stringResource(text), Glyphs.Bolt, tint) }
            }
            Spacer(Modifier.height(dimens.xs))
            Row(Modifier.padding(horizontal = dimens.lg)) { SpeechChip(state) }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LiveSpotlight(live.spotlight, style, Modifier.align(Alignment.Center).padding(bottom = dimens.liveChatClearance))
                LiveReactions(
                    live.reactions,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .width(dimens.reactionLane)
                        .fillMaxHeight(REACTIONS_HEIGHT_FRACTION)
                        .padding(end = dimens.lg, bottom = dimens.lg),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .then(if (Pulse.width.split) Modifier.widthIn(max = dimens.contentMaxWidth) else Modifier.fillMaxWidth(CHAT_WIDTH_FRACTION))
                        .padding(start = dimens.lg),
                ) {
                    LiveCommentStream(live.comments)
                    state.livePartial?.let {
                        Spacer(Modifier.height(dimens.sm))
                        TranscriptCaption(it)
                    }
                }
            }

            LiveControls(state.micMuted, paused, cameraOn, onMic, onFlip, onPause, onClose)
        }

        AnimatedVisibility(paused && state.summary == null, enter = fadeIn(Motion.standard()), exit = fadeOut(Motion.exit())) {
            PausedOverlay(onResume = onResume)
        }
        AnimatedVisibility(countdown > 0, enter = fadeIn(Motion.standard()), exit = fadeOut(Motion.standard())) {
            CountdownOverlay(countdown, style)
        }
    }
}

/** Persists a finished session exactly once: history, fame points, inbox (in the user's language) and analytics. */
private suspend fun recordSession(container: AppContainer, summary: SessionSummary, state: LiveSessionState, limits: PlanLimits) {
    container.history.save(summary, keep = limits.historySize)
    val levels = container.profiles.recordSession(summary.famePointsEarned)
    val now = nowEpochMs()
    container.notifications.add(
        InAppNotification(
            id = "session:${summary.id}",
            kind = NotificationKind.SESSION,
            title = getString(Res.string.live_recap_title),
            body = getString(
                Res.string.live_recap_body,
                summary.title.ifBlank { getString(Res.string.live_your_live) },
                compactCount(summary.peakViewers),
                summary.famePointsEarned,
            ),
            createdAtEpochMs = now,
        ),
    )
    if (levels != null && levels.second > levels.first) {
        val level = FameLevel.entries[levels.second]
        container.notifications.add(
            InAppNotification(
                id = "level_up:${summary.id}",
                kind = NotificationKind.LEVEL_UP,
                title = getString(Res.string.live_level_title, getString(level.titleRes)),
                body = getString(Res.string.live_level_body),
                createdAtEpochMs = now,
            ),
        )
        container.analytics.log(AnalyticsEvent.AchievementUnlocked(level.name))
    }
    val totals = state.live.totals
    container.analytics.log(
        if (summary.durationSeconds < ABANDONED_SECONDS) AnalyticsEvent.LiveAbandoned(summary.durationSeconds)
        else AnalyticsEvent.LiveCompleted(summary.durationSeconds, totals.aiRequests, totals.aiFailures, totals.fallbackResponses),
    )
}

@Composable
private fun aiChip(ai: AiActivity): Pair<StringResource, Color>? {
    val colors = Pulse.colors
    return when (ai) {
        AiActivity.THINKING -> Res.string.live_ai_reacting to colors.violet
        AiActivity.ON_DEVICE -> Res.string.live_ai_on_device to colors.textSecondary
        AiActivity.BUDGET_REACHED -> Res.string.live_ai_budget to colors.warning
        AiActivity.LISTENING -> null
    }
}

@Composable
private fun SpeechChip(state: LiveSessionState) {
    val colors = Pulse.colors
    val (text, tint) = when {
        state.micMuted -> Res.string.live_mic_muted to colors.textMuted
        else -> when (state.speech) {
            SpeechStatus.LISTENING -> Res.string.live_listening to colors.success
            SpeechStatus.STARTING -> Res.string.live_starting_mic to colors.textSecondary
            SpeechStatus.PERMISSION_DENIED -> Res.string.live_mic_denied to colors.warning
            SpeechStatus.UNAVAILABLE -> Res.string.live_speech_unavailable to colors.warning
            SpeechStatus.ERROR -> Res.string.live_cant_hear to colors.warning
            SpeechStatus.OFF -> Res.string.live_mic_off to colors.textMuted
        }
    }
    StatusChip(stringResource(text), if (state.micMuted || state.speech != SpeechStatus.LISTENING) Glyphs.MicOff else Glyphs.Mic, tint)
}

@Composable
private fun CameraOffBackdrop(profile: CreatorProfile, style: LiveStyle) {
    Artwork(style.backdrop, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = Pulse.dimens.liveControlsClearance)) {
                CreatorAvatar(selfAvatar(profile.avatarPalette), Pulse.dimens.giftMajor, ring = AvatarRing.LIVE)
                Spacer(Modifier.height(Pulse.dimens.md))
                Text(stringResource(Res.string.live_camera_off), style = Pulse.type.label, color = Pulse.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LiveTopBar(profile: CreatorProfile, elapsed: Int, remaining: Int, viewers: Int, onClose: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(Modifier.fillMaxWidth().padding(dimens.lg), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).clip(Pulse.shapes.pill).background(colors.glass).padding(dimens.xs).padding(end = dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreatorAvatar(selfAvatar(profile.avatarPalette), dimens.avatarSm)
            Spacer(Modifier.width(dimens.sm))
            Column {
                Text(profile.displayName, style = Pulse.type.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(Modifier.width(dimens.xs))
                    val ending = remaining in 1..ENDING_SOON_SECONDS
                    Text(
                        if (ending) stringResource(Res.string.live_time_left, clock(remaining)) else clock(elapsed),
                        style = Pulse.type.meta,
                        color = if (ending) colors.warning else colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(Pulse.shapes.pill).background(colors.glass).padding(horizontal = dimens.md, vertical = dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseIcon(Glyphs.Eye, colors.textPrimary, size = dimens.iconSmall, contentDescription = stringResource(Res.string.live_simulated_viewers))
            Spacer(Modifier.width(dimens.xs))
            AnimatedCount(viewers, Pulse.type.label, colors.textPrimary)
        }
        Spacer(Modifier.width(dimens.sm))
        GlassIconButton(Glyphs.Close, contentDescription = stringResource(Res.string.live_end_live), onClick = onClose, size = dimens.glassButtonSmall)
    }
}

@Composable
private fun LiveControls(
    micMuted: Boolean,
    paused: Boolean,
    canFlip: Boolean,
    onMic: () -> Unit,
    onFlip: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = dimens.lg, vertical = dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.sm),
    ) {
        GlassIconButton(
            if (micMuted) Glyphs.MicOff else Glyphs.Mic,
            stringResource(if (micMuted) Res.string.live_unmute else Res.string.live_mute),
            onMic,
            active = !micMuted,
        )
        if (canFlip) GlassIconButton(Glyphs.FlipCamera, stringResource(Res.string.live_flip), onFlip)
        GlassIconButton(
            if (paused) Glyphs.Play else Glyphs.Pause,
            stringResource(if (paused) Res.string.live_resume_live else Res.string.live_pause),
            onPause,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(dimens.glassButton)
                .pressable(onClick = onEnd)
                .clip(Pulse.shapes.pill)
                .background(colors.liveGradient)
                .padding(horizontal = dimens.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.live_end), style = Pulse.type.label, color = colors.onMedia)
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(Modifier.fillMaxSize().background(colors.mediaScrim), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(dimens.xxl).widthIn(max = dimens.contentMaxWidth)) {
            PulseIcon(Glyphs.Pause, colors.textPrimary, size = dimens.xxxl)
            Spacer(Modifier.height(dimens.md))
            Text(stringResource(Res.string.live_paused_title), style = Pulse.type.headline, color = colors.textPrimary)
            Text(stringResource(Res.string.live_paused_body), style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(dimens.xl))
            PrimaryButton(stringResource(Res.string.live_resume), onClick = onResume, leading = Glyphs.Play, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun LiveSessionPreview() = PreviewContainer {
    LiveSessionContent(
        state = SampleData.liveState,
        profile = SampleData.profile,
        style = LiveStyles.Aurora,
        cameraOn = false,
        lens = CameraLens.FRONT,
        countdown = 0,
        onClose = {},
        onMic = {},
        onFlip = {},
        onPause = {},
        onResume = {},
    )
}
