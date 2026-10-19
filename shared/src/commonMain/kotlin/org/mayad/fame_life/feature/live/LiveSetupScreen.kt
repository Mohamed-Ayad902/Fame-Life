package org.mayad.fame_life.feature.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.mayad.fame_life.core.platform.isCameraPreviewSupported
import org.mayad.fame_life.core.platform.rememberLivePermissions
import org.mayad.fame_life.domain.simulation.SimulationConfig

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mayad.fame_life.core.presentation.CollectEvents

/** Tier 1: connects the studio to its ViewModel, asks for camera and mic here only, and starts the live. */
@Composable
fun LiveSetupScreen(
    title: String,
    onClose: () -> Unit,
    onSeePro: () -> Unit,
    onStartLive: (sessionId: String, config: SimulationConfig) -> Unit,
    viewModel: LiveSetupViewModel = koinViewModel { parametersOf(title) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberLivePermissions()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is LiveSetupEvent.Start -> onStartLive(event.sessionId, event.config)
        }
    }
    LaunchedEffect(permissions.asked, permissions.camera, permissions.microphone) {
        if (permissions.asked) viewModel.onIntent(LiveSetupIntent.PermissionResult(permissions.camera, permissions.microphone))
    }
    // Without a profile the app shows onboarding instead; this only covers the first frame while the cache loads.
    if (state.avatarPalette == null) return
    LiveSetupContent(
        state = state,
        permission = when {
            permissions.camera && permissions.microphone -> PermissionNeed.NONE
            !permissions.supported -> PermissionNeed.UNSUPPORTED
            permissions.asked -> PermissionNeed.DENIED
            else -> PermissionNeed.NOT_ASKED
        },
        cameraPreview = permissions.camera && isCameraPreviewSupported,
        onIntent = viewModel::onIntent,
        onGoLive = { viewModel.onIntent(LiveSetupIntent.GoLive(permissions.camera, permissions.microphone)) },
        onAllow = permissions::request,
        onClose = onClose,
        onSeePro = onSeePro,
    )
}
