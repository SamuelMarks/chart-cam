package io.healthplatform.chartcam.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.healthplatform.chartcam.AndroidAppInit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Android implementation of [CameraManager] using CameraX.
 */
class AndroidCameraManager(private val context: Context) : CameraManager {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var provider: ProcessCameraProvider? = null
    private val executor = ContextCompat.getMainExecutor(context)
    
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    /**
     * Binds the camera to the lifecycle and the provided PreviewView.
     * Called by the Android CameraPreview composable.
     */
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, view: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            provider = cameraProviderFuture.get()
            startCamera(lifecycleOwner, view)
        }, executor)
    }

    private fun startCamera(lifecycleOwner: LifecycleOwner, view: PreviewView) {
        val cameraProvider = provider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            // Handle binding errors (logs in real app)
        }
    }

    override suspend fun captureImage(): ByteArray? {
        val capture = imageCapture ?: return null

        return suspendCoroutine { continuation ->
            capture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.capacity())
                            buffer.get(bytes)
                            continuation.resume(bytes)
                        } catch (e: Exception) {
                            continuation.resume(null)
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override fun setFlash(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    override fun toggleLens() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK
        }
        // Note: Re-binding requires LifecycleOwner reference if dynamic toggling is needed outside composition flow.
        // In simple flow, the View updates on recomposition or we store the lifecycle owner reference.
    }

    override fun release() {
        provider?.unbindAll()
    }
}

@Composable
actual fun rememberCameraManager(): CameraManager {
    val context = AndroidAppInit.getContext()
    return remember { AndroidCameraManager(context) }
}