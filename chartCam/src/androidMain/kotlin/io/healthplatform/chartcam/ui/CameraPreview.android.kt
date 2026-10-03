/**
 * @file CameraPreview.android.kt
 * Contains declarations for CameraPreview.android.kt.
 *
 * File defining the Android-specific implementation of the [CameraPreview] composable.
 * Integrates CameraX API for previewing and capturing images safely on Android devices.
 */
package io.healthplatform.chartcam.ui

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_camera_preview
import io.healthplatform.chartcam.camera.AndroidCameraManager
import io.healthplatform.chartcam.camera.CameraManager
import org.jetbrains.compose.resources.stringResource

/**
 * A composable function that renders the camera preview for the Android platform.
 *
 * It uses a [PreviewView] from the CameraX library embedded inside an [AndroidView],
 * and binds it to the current lifecycle using the provided [cameraManager].
 *
 * @param modifier The modifier to be applied to the camera preview layout.
 * @param cameraManager The camera manager instance handling the camera lifecycle and operations.
 */
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    cameraManager: CameraManager,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cdText =
        safeGetCameraPreviewDescription()
            .getOrElse { stringResource(Res.string.cd_camera_preview) }

    AndroidView(
        modifier = modifier.semantics { contentDescription = cdText },
        factory = { context ->
            PreviewView(context)
                .apply {
                    layoutParams =
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    // Configure scale type
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { view ->
                    // Bind the lifecycle only once when the view is created
                    (cameraManager as? AndroidCameraManager)?.bindToLifecycle(lifecycleOwner, view)
                }
        },
        update = { view ->
            // No-op: Do not bind here to prevent continuous rebinding on recomposition
        },
    )
}
