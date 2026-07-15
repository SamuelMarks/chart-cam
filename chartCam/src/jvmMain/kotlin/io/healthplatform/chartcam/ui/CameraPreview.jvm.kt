/**
 * @file CameraPreview.jvm.kt
 * Camera preview implementation for the JVM platform.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.camera_unavailable
import chartcam.chartcam.generated.resources.cd_camera_preview
import chartcam.chartcam.generated.resources.error
import chartcam.chartcam.generated.resources.initializing_camera
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JvmCameraManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource

/**
 * A Composable function that displays a live camera preview.
 *
 * This function handles fetching preview frames from the provided [CameraManager] and
 * renders them using a Compose [Image]. If the camera is initializing or frames are not
 * yet available, it displays a loading message.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param cameraManager The camera manager instance responsible for capturing frames.
 */
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager,
) {
    /**
     * The current frame captured from the camera, converted to an [ImageBitmap].
     */
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    /**
     * Tracks if the camera initialization has timed out or failed.
     */
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(cameraManager) {
        if (cameraManager is JvmCameraManager) {
            // Wait up to 5 seconds for the first frame
            val initSuccess =
                kotlinx.coroutines.withTimeoutOrNull(5000) {
                    while (isActive && imageBitmap == null) {
                        val img = cameraManager.getPreviewImage()
                        if (img != null) {
                            imageBitmap = img.toComposeImageBitmap()
                            return@withTimeoutOrNull true
                        }
                        delay(100) // Poll for the first frame
                    }
                    true
                }

            if (initSuccess == null || imageBitmap == null) {
                hasError = true
            } else {
                // First frame loaded, now stream at ~30fps
                while (isActive) {
                    val img = cameraManager.getPreviewImage()
                    if (img != null) {
                        imageBitmap = img.toComposeImageBitmap()
                    } else {
                        // If streaming suddenly drops frames consistently, we could set hasError,
                        // but usually it's just a skipped frame.
                    }
                    delay(33)
                }
            }
        } else {
            hasError = true
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = stringResource(Res.string.cd_camera_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (hasError) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = stringResource(Res.string.error), tint = Color.White)
                Text(stringResource(Res.string.camera_unavailable), color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Text(stringResource(Res.string.initializing_camera), color = Color.White)
        }
    }
}
