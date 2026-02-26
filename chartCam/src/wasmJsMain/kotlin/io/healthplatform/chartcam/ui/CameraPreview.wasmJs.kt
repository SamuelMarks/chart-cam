package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JsCameraManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.w3c.dom.HTMLVideoElement
import okio.ByteString.Companion.decodeBase64

private fun getBase64ImageFast(video: HTMLVideoElement): String? = js("""
    (() => {
        if (video.videoWidth === 0 || video.videoHeight === 0) return null;
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.6);
        const base64 = dataUrl.split(',')[1];
        return base64 || null;
    })()
""")

@OptIn(ExperimentalResourceApi::class)
@Composable 
actual fun CameraPreview(modifier: Modifier, cameraManager: CameraManager) {
    if (cameraManager is JsCameraManager) {
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        
        LaunchedEffect(cameraManager) {
            val video = cameraManager.videoElement
            while (true) {
                try {
                    // 2 means HAVE_CURRENT_DATA or higher
                    if (video.readyState.toInt() >= 2) {
                        val b64 = getBase64ImageFast(video)
                        if (b64 != null) {
                            val bytes = b64.decodeBase64()?.toByteArray()
                            if (bytes != null) {
                                imageBitmap = bytes.decodeToImageBitmap()
                            }
                        }
                    }
                } catch (e: Throwable) {
                    // Ignore errors during frame capture
                }
                // roughly 15 fps
                delay(66)
            }
        }
        
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Camera Preview",
                modifier = modifier.background(Color.Black),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = modifier.background(Color.Black))
        }
    }
}
