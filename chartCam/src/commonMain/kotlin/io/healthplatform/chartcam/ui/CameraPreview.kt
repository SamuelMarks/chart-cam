/**
 * Contains the cross-platform CameraPreview composable definition.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.healthplatform.chartcam.camera.CameraManager

/**
 * A platform-agnostic Composable that renders the Camera Preview.
 *
 * It manages the underlying native View (AndroidView using PreviewView,
 * or UIKitView using AVCaptureVideoPreviewLayer).
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param modifier Layout modifiers to apply to the camera preview.
 * @param cameraManager The manager instance controlling the camera session and settings.
 */
@Composable
expect fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager,
)
