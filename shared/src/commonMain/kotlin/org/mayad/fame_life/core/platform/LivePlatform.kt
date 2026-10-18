package org.mayad.fame_life.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

enum class CameraLens { FRONT, BACK }

/** Camera and microphone access for going live. Requested only from the live setup screen. */
@Stable
interface LivePermissions {
    val camera: Boolean
    val microphone: Boolean

    /** True once the user answered the system prompt at least once in this screen. */
    val asked: Boolean

    /** False where the platform integration is not built yet; the live screen then runs without camera and mic. */
    val supported: Boolean

    fun request()
}

@Composable
expect fun rememberLivePermissions(): LivePermissions

/** True when [CameraPreview] can show the camera on this platform. */
expect val isCameraPreviewSupported: Boolean

/**
 * Shows the local camera feed. Frames are rendered on screen only; nothing is recorded, stored or uploaded.
 * Callers must check [LivePermissions.camera] first.
 */
@Composable
expect fun CameraPreview(lens: CameraLens, modifier: Modifier = Modifier)
