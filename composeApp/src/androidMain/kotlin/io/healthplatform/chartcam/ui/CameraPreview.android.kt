package io.healthplatform.chartcam.ui

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.healthplatform.chartcam.camera.AndroidCameraManager
import io.healthplatform.chartcam.camera.CameraManager

@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                // Configure scale type
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { view ->
            // Bind the lifecycle when the view is updated/attached
            (cameraManager as? AndroidCameraManager)?.bindToLifecycle(lifecycleOwner, view)
        }
    )
}