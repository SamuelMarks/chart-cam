/**
 * @file AndroidCameraManager.kt
 * Contains declarations for AndroidCameraManager.kt.
 *
 * File defining the Android-specific implementation of [CameraManager] and its composable factory.
 */
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
 * Android implementation of [CameraManager] using the CameraX library.
 *
 * @param context The application [Context] used to interact with CameraX and the system.
 */
class AndroidCameraManager(
    private val context: Context,
) : CameraManager {
    /**
     * CameraX use case for capturing images.
     */
    private var imageCapture: ImageCapture? = null

    /**
     * Represents the currently bound camera instance, used for controlling flash and other hardware properties.
     */
    private var camera: Camera? = null

    /**
     * Provides access to bind use cases to the lifecycle.
     */
    private var provider: ProcessCameraProvider? = null

    /**
     * Main thread executor used for CameraX callbacks.
     */
    private val executor = ContextCompat.getMainExecutor(context)

    /**
     * Current lens facing direction (defaults to back camera).
     */
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    /**
     * Binds the camera to the provided lifecycle and connects the output to the [PreviewView].
     * Called by the Android CameraPreview composable.
     *
     * @param lifecycleOwner The lifecycle owner (usually the containing Activity or Fragment) to bind the camera to.
     * @param view The [PreviewView] surface where the camera preview stream will be rendered.
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        view: PreviewView,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            provider = cameraProviderFuture.get()
            startCamera(lifecycleOwner, view)
        }, executor)
    }

    /**
     * Internal method to configure and bind the Preview and ImageCapture use cases to the [ProcessCameraProvider].
     *
     * @param lifecycleOwner The lifecycle owner that controls the camera's active state.
     * @param view The [PreviewView] used to display the stream.
     */
    private fun startCamera(
        lifecycleOwner: LifecycleOwner,
        view: PreviewView,
    ) {
        val cameraProvider = provider ?: return

        val preview =
            Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder().build()

        val cameraSelector =
            CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

        try {
            cameraProvider.unbindAll()
            camera =
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
        } catch (exc: IllegalArgumentException) {
            println("Camera binding failed: ${exc.message}")
        }
    }

    /**
     * Captures an image asynchronously using the bound [ImageCapture] use case.
     *
     * @return A [ByteArray] containing the JPEG encoded image data, or `null` if the
     * capture failed or no use case is bound.
     */
    override suspend fun captureImage(): ByteArray? {
        val capture = imageCapture ?: return null

        return suspendCoroutine { continuation ->
            capture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    /**
                     * Callback when image is successfully captured.
                     *
                     * @param image The captured image proxy.
                     */
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.capacity())
                            buffer.get(bytes)
                            continuation.resume(bytes)
                        } catch (e: java.nio.BufferUnderflowException) {
                            println("Image buffer read failed: ${e.message}")
                            continuation.resume(null)
                        } catch (e: IllegalArgumentException) {
                            println("Image buffer allocation failed: ${e.message}")
                            continuation.resume(null)
                        } catch (e: IllegalStateException) {
                            println("Image plane access failed: ${e.message}")
                            continuation.resume(null)
                        } finally {
                            image.close()
                        }
                    }

                    /**
                     * Callback when image capture fails.
                     *
                     * @param exception The exception describing the capture failure.
                     */
                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
    }

    /**
     * Turns the device's camera flash (torch) on or off.
     *
     * @param on `true` to enable the torch, `false` to disable it.
     */
    override fun setFlash(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    /**
     * Toggles between the front and back camera lenses.
     * Note: Re-binding requires [LifecycleOwner] reference if dynamic toggling is needed outside composition flow.
     */
    override fun toggleLens() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK
        }
        // Note: Re-binding requires LifecycleOwner reference if dynamic toggling is needed outside composition flow.
        // In simple flow, the View updates on recomposition or we store the lifecycle owner reference.
    }

    /**
     * Releases camera resources by unbinding all use cases from the [ProcessCameraProvider].
     */
    override fun release() {
        provider?.unbindAll()
    }
}

/**
 * A composable function that remembers an instance of [CameraManager] tailored for the Android platform.
 *
 * @return An instance of [CameraManager] (specifically [AndroidCameraManager]).
 */
@Composable
actual fun rememberCameraManager(): CameraManager {
    val context = AndroidAppInit.getContext()
    return remember { AndroidCameraManager(context) }
}
