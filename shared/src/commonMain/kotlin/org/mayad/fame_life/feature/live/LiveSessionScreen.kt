package org.mayad.fame_life.feature.live

import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.live_end_body
import famelife.shared.generated.resources.live_end_live
import famelife.shared.generated.resources.live_end_title
import famelife.shared.generated.resources.live_keep_going
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.platform.isCameraPreviewSupported
import org.mayad.fame_life.core.platform.rememberLivePermissions
import org.mayad.fame_life.domain.simulation.SimulationConfig
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mayad.fame_life.core.presentation.CollectEvents

/**
 * Tier 1: the live scene. The ViewModel owns the session; this screen renders it, forwards the creator's controls,
 * pauses the room when the app goes to the background, and asks before ending.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveSessionScreen(
    sessionId: String,
    config: SimulationConfig,
    onCancelled: () -> Unit,
    onFinished: (sessionId: String) -> Unit,
    viewModel: LiveSessionViewModel = koinViewModel(key = sessionId) { parametersOf(sessionId, config) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberLivePermissions()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is LiveSessionEvent.Finished -> onFinished(event.sessionId)
            LiveSessionEvent.Cancelled -> onCancelled()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onIntent(LiveSessionIntent.AppStopped) }
    BackHandler(enabled = state.session.summary == null) { viewModel.onIntent(LiveSessionIntent.AskEnd) }

    val profile = state.profile ?: return
    LiveSessionContent(
        state = state,
        profile = profile,
        cameraOn = permissions.camera && isCameraPreviewSupported,
        onClose = { viewModel.onIntent(LiveSessionIntent.AskEnd) },
        onMic = { viewModel.onIntent(LiveSessionIntent.ToggleMic) },
        onFlip = { viewModel.onIntent(LiveSessionIntent.FlipLens) },
        onPause = { viewModel.onIntent(LiveSessionIntent.TogglePause) },
        onResume = { viewModel.onIntent(LiveSessionIntent.Resume) },
    )
    if (state.confirmEnd) {
        EndLiveDialog(
            onEnd = { viewModel.onIntent(LiveSessionIntent.ConfirmEnd) },
            onKeepGoing = { viewModel.onIntent(LiveSessionIntent.KeepGoing) },
        )
    }
}

@Composable
private fun EndLiveDialog(onEnd: () -> Unit, onKeepGoing: () -> Unit) {
    val colors = Pulse.colors
    AlertDialog(
        onDismissRequest = onKeepGoing,
        containerColor = colors.surfaceElevated,
        title = { Text(stringResource(Res.string.live_end_title), style = Pulse.type.title, color = colors.textPrimary) },
        text = { Text(stringResource(Res.string.live_end_body), style = Pulse.type.body, color = colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onEnd) { Text(stringResource(Res.string.live_end_live), style = Pulse.type.label, color = colors.live) }
        },
        dismissButton = {
            TextButton(onClick = onKeepGoing) { Text(stringResource(Res.string.live_keep_going), style = Pulse.type.label, color = colors.textPrimary) }
        },
    )
}
