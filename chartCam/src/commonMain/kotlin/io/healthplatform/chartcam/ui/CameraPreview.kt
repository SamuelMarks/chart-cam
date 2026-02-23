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
 * @param modifier Layout modifiers.
 * @param cameraManager The manager instance controlling the session.
 */
@Composable
expect fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager
)