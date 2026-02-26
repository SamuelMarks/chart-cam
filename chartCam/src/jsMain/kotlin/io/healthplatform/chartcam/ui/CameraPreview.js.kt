package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JsCameraManager
import kotlinx.browser.document

@Composable 
actual fun CameraPreview(modifier: Modifier, cameraManager: CameraManager) {
    if (cameraManager is JsCameraManager) {
        DisposableEffect(cameraManager) {
            val video = cameraManager.videoElement
            video.style.position = "fixed"
            video.style.left = "0px"
            video.style.top = "0px"
            video.style.width = "100vw"
            video.style.height = "100vh"
            video.style.zIndex = "-1"
            video.style.setProperty("object-fit", "cover")
            
            // Ensure the video is not already attached somewhere else
            if (video.parentElement != null) {
                video.parentElement?.removeChild(video)
            }
            
            document.body?.appendChild(video)
            
            onDispose {
                if (video.parentElement != null) {
                    video.parentElement?.removeChild(video)
                }
            }
        }
    }
}
