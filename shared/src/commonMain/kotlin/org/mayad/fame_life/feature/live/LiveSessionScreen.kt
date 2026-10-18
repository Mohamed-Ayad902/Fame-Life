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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
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
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.clock
import org.mayad.fame_life.core.platform.CameraLens
import org.mayad.fame_life.core.platform.CameraPreview
import org.mayad.fame_life.core.platform.isCameraPreviewSupported
import org.mayad.fame_life.core.platform.monotonicMillis
import org.mayad.fame_life.core.platform.rememberLivePermissions
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

/**
 * The live scene. The controller owns the session; this screen renders its state, forwards the creator's
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
    val style = LiveStyles.byId(config.liveStyleId)
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
    var countdown by remember { mutableStateOf(3) }
    var lens by remember { mutableStateOf(CameraLens.FRONT) }
    var confirmEnd by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    val reduceMotion = Pulse.reducedMotion

    DisposableEffect(controller) { onDispose { controller.end() } }
    LaunchedEffect(controller) {
        for (n in 3 downTo 1) {
            countdown = n
            delay(if (reduceMotion) 500L else 800L)
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

    val live = state.live
    val cameraOn = permissions.camera && isCameraPreviewSupported
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraOn) {
            CameraPreview(lens, Modifier.fillMaxSize())
        } else {
            CameraOffBackdrop(profile, style)
        }
        // Scrims keep text readable over any camera image without covering the face in the middle.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0xAA05070D), 0.18f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xE605070D)),
            ),
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            LiveTopBar(profile, live.elapsedSeconds, live.remainingSeconds, live.viewers, onClose = { confirmEnd = true })
            Row(Modifier.padding(horizontal = Pulse.dimens.lg), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.xs)) {
                StatusChip("Simulated audience", Glyphs.Sparkles, Pulse.colors.cyan)
                aiChip(state)?.let { (text, tint) -> StatusChip(text, Glyphs.Bolt, tint) }
            }
            Spacer(Modifier.height(Pulse.dimens.xs))
            Row(Modifier.padding(horizontal = Pulse.dimens.lg)) { SpeechChip(state) }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LiveSpotlight(live.spotlight, style, Modifier.align(Alignment.Center).padding(bottom = 120.dp))
                LiveReactions(live.reactions, Modifier.align(Alignment.BottomEnd).width(72.dp).fillMaxHeight(0.7f).padding(end = Pulse.dimens.lg, bottom = Pulse.dimens.lg))
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.8f).padding(start = Pulse.dimens.lg)) {
                    LiveCommentStream(live.comments)
                    state.livePartial?.let {
                        Spacer(Modifier.height(Pulse.dimens.sm))
                        TranscriptCaption(it)
                    }
                }
            }

            LiveControls(
                micMuted = state.micMuted,
                paused = live.status == SessionStatus.PAUSED,
                canFlip = cameraOn,
                onMic = { controller.setMicMuted(!state.micMuted) },
                onFlip = { lens = if (lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT },
                onPause = { if (live.status == SessionStatus.PAUSED) controller.resume() else controller.pause() },
                onEnd = { confirmEnd = true },
            )
        }

        AnimatedVisibility(live.status == SessionStatus.PAUSED && state.summary == null, enter = fadeIn(Motion.standard()), exit = fadeOut(Motion.exit())) {
            PausedOverlay(onResume = controller::resume)
        }
        AnimatedVisibility(countdown > 0, enter = fadeIn(Motion.standard()), exit = fadeOut(Motion.standard())) {
            CountdownOverlay(countdown, style)
        }
    }

    if (confirmEnd) {
        val colors = Pulse.colors
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = colors.surfaceElevated,
            title = { Text("End your live?", style = Pulse.type.title, color = colors.textPrimary) },
            text = { Text("Your results are saved and the audience goes home.", style = Pulse.type.body, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    if (live.status == SessionStatus.READY) navigator.pop() else controller.end()
                }) { Text("End live", style = Pulse.type.label, color = colors.live) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Keep going", style = Pulse.type.label, color = colors.textPrimary) }
            },
        )
    }
}

/** Persists a finished session exactly once: history, fame points, inbox and analytics. */
private fun recordSession(container: AppContainer, summary: SessionSummary, state: LiveSessionState, limits: PlanLimits) {
    container.history.save(summary, keep = limits.historySize)
    val levels = container.profiles.recordSession(summary.famePointsEarned)
    val now = nowEpochMs()
    container.notifications.add(
        InAppNotification(
            id = "session:${summary.id}",
            kind = NotificationKind.SESSION,
            title = "Live recap ready",
            body = "${summary.title.ifBlank { "Your live" }} peaked at ${summary.peakViewers} simulated viewers. +${summary.famePointsEarned} fame points.",
            createdAtEpochMs = now,
        ),
    )
    if (levels != null && levels.second > levels.first) {
        val level = FameLevel.entries[levels.second]
        container.notifications.add(
            InAppNotification(
                id = "level_up:${summary.id}",
                kind = NotificationKind.LEVEL_UP,
                title = "You're now a ${level.title}",
                body = "Your lives earned enough fame points to reach the next level.",
                createdAtEpochMs = now,
            ),
        )
        container.analytics.log(AnalyticsEvent.AchievementUnlocked(level.name))
    }
    val totals = state.live.totals
    container.analytics.log(
        if (summary.durationSeconds < 15) AnalyticsEvent.LiveAbandoned(summary.durationSeconds)
        else AnalyticsEvent.LiveCompleted(summary.durationSeconds, totals.aiRequests, totals.aiFailures, totals.fallbackResponses),
    )
}

private fun aiChip(state: LiveSessionState): Pair<String, Color>? = when (state.ai) {
    AiActivity.THINKING -> "Audience reacting…" to Color(0xFF945CFF)
    AiActivity.ON_DEVICE -> "On-device audience" to Color(0xFFA5AEC4)
    AiActivity.BUDGET_REACHED -> "AI limit reached · on-device" to Color(0xFFFFC66D)
    AiActivity.LISTENING -> null
}

@Composable
private fun SpeechChip(state: LiveSessionState) {
    val colors = Pulse.colors
    val (text, tint) = when {
        state.micMuted -> "Mic muted" to colors.textMuted
        else -> when (state.speech) {
            SpeechStatus.LISTENING -> "Listening" to colors.success
            SpeechStatus.STARTING -> "Starting mic…" to colors.textSecondary
            SpeechStatus.PERMISSION_DENIED -> "Mic not allowed · audience reacts to the room" to colors.warning
            SpeechStatus.UNAVAILABLE -> "Speech recognition isn't available here" to colors.warning
            SpeechStatus.ERROR -> "Couldn't hear you, retrying" to colors.warning
            SpeechStatus.OFF -> "Mic off" to colors.textMuted
        }
    }
    StatusChip(text, if (state.micMuted || state.speech != SpeechStatus.LISTENING) Glyphs.MicOff else Glyphs.Mic, tint)
}

@Composable
private fun CameraOffBackdrop(profile: CreatorProfile, style: LiveStyle) {
    Artwork(style.backdrop, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 140.dp)) {
                CreatorAvatar(selfAvatar(profile.avatarPalette), 148.dp, ring = AvatarRing.LIVE)
                Spacer(Modifier.height(Pulse.dimens.md))
                Text("Camera off", style = Pulse.type.label, color = Pulse.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LiveTopBar(profile: CreatorProfile, elapsed: Int, remaining: Int, viewers: Int, onClose: () -> Unit) {
    val colors = Pulse.colors
    Row(Modifier.fillMaxWidth().padding(Pulse.dimens.lg), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).clip(Pulse.shapes.pill).background(Color(0x66080B14)).padding(4.dp).padding(end = Pulse.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreatorAvatar(selfAvatar(profile.avatarPalette), 34.dp)
            Spacer(Modifier.width(Pulse.dimens.sm))
            Column {
                Text(profile.displayName, style = Pulse.type.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(Modifier.width(6.dp))
                    val ending = remaining in 1..60
                    Text(
                        if (ending) "${clock(remaining)} left" else clock(elapsed),
                        style = Pulse.type.meta,
                        color = if (ending) colors.warning else colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(Pulse.shapes.pill).background(Color(0x66080B14)).padding(horizontal = Pulse.dimens.md, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseIcon(Glyphs.Eye, colors.textPrimary, size = 16.dp, contentDescription = "Simulated viewers")
            Spacer(Modifier.width(6.dp))
            AnimatedCount(viewers, Pulse.type.label, colors.textPrimary)
        }
        Spacer(Modifier.width(Pulse.dimens.sm))
        GlassIconButton(Glyphs.Close, contentDescription = "End live", onClick = onClose, size = 40.dp)
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Pulse.dimens.lg, vertical = Pulse.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm),
    ) {
        GlassIconButton(if (micMuted) Glyphs.MicOff else Glyphs.Mic, if (micMuted) "Unmute microphone" else "Mute microphone", onMic, active = !micMuted)
        if (canFlip) GlassIconButton(Glyphs.FlipCamera, "Flip camera", onFlip)
        GlassIconButton(if (paused) Glyphs.Play else Glyphs.Pause, if (paused) "Resume live" else "Pause live", onPause)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(44.dp)
                .pressable(onClick = onEnd)
                .clip(Pulse.shapes.pill)
                .background(colors.liveGradient)
                .padding(horizontal = Pulse.dimens.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text("End", style = Pulse.type.label, color = Color.White)
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val colors = Pulse.colors
    Box(Modifier.fillMaxSize().background(Color(0xCC080B14)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Pulse.dimens.xxl)) {
            PulseIcon(Glyphs.Pause, colors.textPrimary, size = 40.dp)
            Spacer(Modifier.height(Pulse.dimens.md))
            Text("Live paused", style = Pulse.type.headline, color = colors.textPrimary)
            Text("The audience is waiting. The mic is off while paused.", style = Pulse.type.body, color = colors.textSecondary)
            Spacer(Modifier.height(Pulse.dimens.xl))
            PrimaryButton("Resume", onClick = onResume, leading = Glyphs.Play, modifier = Modifier.size(width = 200.dp, height = 54.dp))
        }
    }
}
