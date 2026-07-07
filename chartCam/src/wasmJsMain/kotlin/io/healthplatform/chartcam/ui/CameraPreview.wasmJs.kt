/**
 * @file CameraPreview.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of the [CameraPreview] composable,
 * designed to integrate with browser-based video elements and capture frames as [ImageBitmap]s.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_camera_preview
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JsCameraManager
import kotlinx.coroutines.delay
import okio.ByteString.Companion.decodeBase64
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.HTMLVideoElement

/**
 * Executes raw JavaScript to extract a frame from an [HTMLVideoElement] and encodes it
 * as a base64-encoded JPEG image string.
 *
 * @param video The HTML video element to capture the frame from.
 * @return A base64-encoded string representation of the captured JPEG image,
 *         or null if the capture fails (e.g., if video dimensions are zero).
 */
private fun getBase64ImageFast(video: HTMLVideoElement): String? =
    js(
        """
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
""",
    )

/**
 * A WebAssembly (WasmJs) specific implementation of the [CameraPreview] composable.
 * Displays real-time camera feed by periodically capturing frames from the [CameraManager]'s
 * underlying [HTMLVideoElement] and rendering them as [ImageBitmap]s.
 *
 * @param modifier The [Modifier] to be applied to the preview's layout.
 * @param cameraManager The [CameraManager] instance managing the active camera session.
 *                      Must be of type [JsCameraManager] in order to extract frames.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager,
) {
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
                contentDescription = stringResource(Res.string.cd_camera_preview),
                modifier = modifier.background(Color.Black),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = modifier.background(Color.Black))
        }
    }
}
