@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
    "WildcardImport",
    "UNCHECKED_CAST",
    "CAST_NEVER_SUCCEEDS",
    "USELESS_CAST",
)
/**
 * @file IOSCameraManager.kt
 * Contains declarations for IOSCameraManager.kt.
 */

package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of [CameraManager].
 *
 * This class uses the AVFoundation framework, specifically managing an
 * [AVCaptureSession] and an [AVCapturePhotoOutput] to control the device's
 * camera and capture still images.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSCameraManager : CameraManager {
    /**
     * The primary capture session responsible for coordinating data flow from
     * input devices to outputs.
     */
    val captureSession = AVCaptureSession()

    /**
     * The output responsible for capturing still photos.
     */
    private val photoOutput = AVCapturePhotoOutput()

    /**
     * The currently active video device input (typically the back or front camera).
     */
    private var videoDeviceInput: AVCaptureDeviceInput? = null

    /**
     * Maintain a strong reference to the delegate so it doesn't get deallocated
     * prematurely before the photo capture completes.
     */
    private var activeDelegate: AVCapturePhotoCaptureDelegateProtocol? = null

    /**
     * Initializes the [IOSCameraManager] and configures the [AVCaptureSession].
     */
    init {
        configureSession()
    }

    /**
     * Configures the [AVCaptureSession] by setting the preset, finding the default
     * video device, and adding the necessary inputs and outputs.
     */
    private fun configureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return

        if (captureSession.canAddInput(input)) {
            captureSession.addInput(input)
            videoDeviceInput = input as AVCaptureDeviceInput
        }

        if (captureSession.canAddOutput(photoOutput)) {
            captureSession.addOutput(photoOutput)
        }

        captureSession.commitConfiguration()

        if (!captureSession.running) {
            captureSession.startRunning()
        }
    }

    /**
     * Captures a still image from the camera.
     *
     * Suspends the coroutine until a photo is captured, processed, and its
     * binary data is ready.
     *
     * @return A [ByteArray] containing the JPEG-encoded image data, or `null` if the
     *         capture fails or no camera is available.
     */
    override suspend fun captureImage(): ByteArray? =
        suspendCoroutine { continuation ->
            // The crash occurs if we try to capture when there's no active video connection or session isn't running.
            if (!captureSession.running || photoOutput.connections.isEmpty()) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            val settings = AVCapturePhotoSettings.photoSettings()

            val delegate =
                object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
                    /**
                     * Called when the photo output finishes processing a captured photo.
                     *
                     * @param output The output that captured the photo.
                     * @param didFinishProcessingPhoto The captured photo object.
                     * @param error An error object indicating capture failure, if any.
                     */
                    override fun captureOutput(
                        output: AVCapturePhotoOutput,
                        didFinishProcessingPhoto: AVCapturePhoto,
                        error: NSError?,
                    ) {
                        activeDelegate = null

                        if (error != null) {
                            continuation.resume(null)
                            return
                        }

                        val data = didFinishProcessingPhoto.fileDataRepresentation()
                        if (data != null) {
                            val bytes = data.toByteArray()
                            continuation.resume(bytes)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }

            activeDelegate = delegate

            try {
                photoOutput.capturePhotoWithSettings(settings, delegate)
            } catch (e: IllegalStateException) {
                println(e.message)
                activeDelegate = null
                continuation.resume(null)
            }
        }

    /**
     * Sets the flash/torch mode on the current camera device.
     *
     * @param on `true` to turn the torch on, `false` to turn it off.
     */
    override fun setFlash(on: Boolean) {
        val device = videoDeviceInput?.device ?: return
        if (device.hasTorch) {
            try {
                device.lockForConfiguration(null)
                device.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                device.unlockForConfiguration()
            } catch (e: IllegalStateException) {
                println(e.message)
                // Handle error
            }
        }
    }

    /**
     * Toggles between front and rear lenses.
     *
     * Implementation is omitted for brevity in this class.
     */
    override fun toggleLens() {
        // Implementation omitted for brevity
    }

    /**
     * Releases camera resources by stopping the capture session.
     */
    override fun release() {
        captureSession.stopRunning()
    }
}

/**
 * Extension function to convert an [NSData] object to a Kotlin [ByteArray].
 *
 * @return A [ByteArray] containing a copy of the bytes from the [NSData].
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    this.bytes?.let { pointer ->
        byteArray.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), pointer, length.toULong())
        }
    }
    return byteArray
}

/**
 * Creates and remembers an iOS-specific [CameraManager].
 *
 * @return An instance of [IOSCameraManager] managed by Compose.
 */
@Composable
actual fun rememberCameraManager(): CameraManager {
    val manager = remember { IOSCameraManager() }
    DisposableEffect(manager) {
        onDispose {
            manager.release()
        }
    }
    return manager
}
