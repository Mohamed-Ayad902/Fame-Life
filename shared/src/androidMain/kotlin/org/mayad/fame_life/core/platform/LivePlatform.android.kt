package org.mayad.fame_life.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun Context.granted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private class AndroidLivePermissions(camera: Boolean, microphone: Boolean) : LivePermissions {
    override var camera by mutableStateOf(camera)
    override var microphone by mutableStateOf(microphone)
    override var asked by mutableStateOf(false)
    override val supported = true
    var launch: () -> Unit = {}
    override fun request() = launch()
}

@Composable
actual fun rememberLivePermissions(): LivePermissions {
    val context = LocalContext.current
    val state = remember { AndroidLivePermissions(context.granted(Manifest.permission.CAMERA), context.granted(Manifest.permission.RECORD_AUDIO)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        state.camera = result[Manifest.permission.CAMERA] ?: context.granted(Manifest.permission.CAMERA)
        state.microphone = result[Manifest.permission.RECORD_AUDIO] ?: context.granted(Manifest.permission.RECORD_AUDIO)
        state.asked = true
    }
    state.launch = { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
    // The user may change access in system settings while the app is in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.camera = context.granted(Manifest.permission.CAMERA)
        state.microphone = context.granted(Manifest.permission.RECORD_AUDIO)
    }
    return state
}

actual val isCameraPreviewSupported: Boolean = true

@Composable
actual fun CameraPreview(lens: CameraLens, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val preview = remember { Preview.Builder().build() }

    DisposableEffect(lens, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            val cameraProvider = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = cameraProvider
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val wanted = if (lens == CameraLens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val fallback = if (lens == CameraLens.FRONT) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            val selector = if (runCatching { cameraProvider.hasCamera(wanted) }.getOrDefault(false)) wanted else fallback
            // Only one preview can own the camera; the newest screen takes it over.
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbind(preview)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
