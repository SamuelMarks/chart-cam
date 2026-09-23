/**
 * @file CameraPreview.jvm.kt
 * Camera preview implementation for the JVM platform.
 * Serves as a placeholder or mock UI, as native camera capture is not natively
 * supported directly via Compose Desktop in this module.
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.camera_unavailable
import chartcam.chartcam.generated.resources.cd_camera_preview
import chartcam.chartcam.generated.resources.error
import chartcam.chartcam.generated.resources.initializing_camera
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JvmCameraManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

internal var timeoutMs: Long = 5000L
internal var pollIntervalMs: Long = 100L
private const val STREAM_INTERVAL_MS = 33L

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
            // Wait up to timeoutMs for the first frame
            val initSuccess =
                kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                    while (true) {
                        val img = cameraManager.getPreviewImage()
                        if (img != null) {
                            imageBitmap = img.toComposeImageBitmap()
                            break
                        }
                        delay(pollIntervalMs)
                    }
                }

            if (initSuccess == null) {
                hasError = true
            } else {
                // First frame loaded, now stream at ~30fps
                while (true) {
                    val img = cameraManager.getPreviewImage()
                    if (img != null) {
                        imageBitmap = img.toComposeImageBitmap()
                    } else {
                        // If streaming suddenly drops frames consistently, we could set hasError,
                        // but usually it's just a skipped frame.
                    }
                    delay(STREAM_INTERVAL_MS)
                }
            }
        } else {
            hasError = true
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = stringResource(Res.string.cd_camera_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (hasError) {
            val errorMsg = stringResource(Res.string.camera_unavailable)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.semantics {
                        error(errorMsg)
                        liveRegion = LiveRegionMode.Polite
                    },
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            val initMsg = stringResource(Res.string.initializing_camera)
            Text(
                text = initMsg,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = initMsg
                    },
            )
        }
    }
}
