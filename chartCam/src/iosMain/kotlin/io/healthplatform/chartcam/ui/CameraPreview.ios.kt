package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.IOSCameraManager
import kotlinx.cinterop.CValue
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.CoreGraphics.CGRect
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager
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
                // Tag the layer or view to find it in resize? 
                // Alternatively, subclass UIView to handle layoutSubviews.
            }
            view
        },
        modifier = modifier,
        onResize = { view: UIView, rect: CValue<CGRect> ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.sublayers?.firstOrNull()?.let { layer ->
                (layer as AVCaptureVideoPreviewLayer).frame = rect
            }
            CATransaction.commit()
        }
    )
}