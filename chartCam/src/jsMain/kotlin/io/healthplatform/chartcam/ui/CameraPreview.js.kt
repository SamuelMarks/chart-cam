package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import io.healthplatform.chartcam.camera.CameraManager
import androidx.compose.ui.viewinterop.WebElementView
import io.healthplatform.chartcam.camera.JsCameraManager
import org.w3c.dom.HTMLVideoElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable 
actual fun CameraPreview(modifier: Modifier, cameraManager: CameraManager) {
    if (cameraManager is JsCameraManager) {
        WebElementView(
            factory = { cameraManager.videoElement },
            modifier = modifier
        )
    }
}
