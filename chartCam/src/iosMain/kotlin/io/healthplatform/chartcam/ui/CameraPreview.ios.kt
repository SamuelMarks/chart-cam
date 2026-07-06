/**
 * iOS implementation of the camera preview UI component.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.IOSCameraManager
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

/**
 * A Composable that displays the camera preview using an iOS [UIView].
 *
 * This function integrates with iOS's AVFoundation to render the live camera
 * feed into a Compose Multiplatform hierarchy via [UIKitView].
 *
 * @param modifier The modifier to be applied to the camera preview.
 * @param cameraManager The [CameraManager] instance managing the camera session.
 *                      Expected to be an [IOSCameraManager] on this platform.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager,
) {
    val iosManager = cameraManager as? IOSCameraManager
    UIKitView(
        factory = {
            val view = UIView()
            view.backgroundColor = platform.UIKit.UIColor.blackColor

            iosManager?.let { manager ->
                val layer = AVCaptureVideoPreviewLayer(session = manager.captureSession)
                layer.videoGravity = platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
                view.layer.addSublayer(layer)
            }
            view
        },
        modifier = modifier,
        update = { view ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.sublayers?.firstOrNull()?.let {
                (it as? platform.QuartzCore.CALayer)?.frame = view.bounds
            }
            CATransaction.commit()
        },
    )
}
