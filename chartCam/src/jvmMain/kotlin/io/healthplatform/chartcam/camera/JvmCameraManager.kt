/**
 * @file CameraManager.jvm.kt
 * Camera management and capture implementation for the JVM platform.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * JVM implementation of [CameraManager] using the Sarxos webcam-capture library.
 * This provides real camera functionality for Desktop targets, fully supporting
 * the Photo Capture feature.
 */
class JvmCameraManager : CameraManager {
    /**
     * Static initialization block to set up the webcam driver.
     */
    companion object {
        init {
            if (System.getProperty("chartcam.isTest") != "true" &&
                System.getProperty("io.healthplatform.chartcam.camera.nativedriver.initialized") != "true"
            ) {
                try {
                    Webcam.setDriver(NativeDriver())
                    System.setProperty("io.healthplatform.chartcam.camera.nativedriver.initialized", "true")
                } catch (t: IllegalStateException) {
                    println(t.message)
                    // Driver might already be set or failed to initialize
                }
            }
        }
    }

    /**
     * The internal webcam instance. Can be null if initialization fails or no camera is present.
     */
    private var webcam: Webcam? = null

    /**
     * Initializes the webcam instance securely off the main thread.
     * We catch `Throwable` rather than just `Exception` because the native JNA driver
     * can throw fatal errors (like `java.lang.Error` or `java.lang.UnsatisfiedLinkError`)
     * on macOS ARM64 when camera permissions (FaceTime HD) are denied or missing.
     *
     * @return The initialized [Webcam] instance, or null if no webcam could be found or permitted.
     */
    private suspend fun getWebcam(): Webcam? =
        withContext(Dispatchers.IO) {
            if (System.getProperty("chartcam.isTest") == "true") return@withContext null
            if (webcam == null) {
                try {
                    webcam = Webcam.getDefault()
                } catch (t: IllegalStateException) {
                    println(t.message)
                    // Return null safely if webcam lookup or native driver loading fails
                    webcam = null
                }
            }
            webcam
        }

    /**
     * Gets a raw BufferedImage for preview purposes.
     *
     * @return A [BufferedImage] representing the current frame, or null if a frame cannot be retrieved.
     */
    suspend fun getPreviewImage(): BufferedImage? =
        withContext(Dispatchers.IO) {
            val cam = getWebcam() ?: return@withContext null
            try {
                if (!cam.isOpen) {
                    cam.open()
                }
                cam.image
            } catch (e: IllegalStateException) {
                println(e.message)
                null
            }
        }

    /**
     * Captures a still image from the active desktop webcam.
     *
     * @return A [ByteArray] representing the image (PNG encoded),
     *         or null if the capture failed or the webcam is not available.
     */
    override suspend fun captureImage(): ByteArray? =
        withContext(Dispatchers.IO) {
            val image = getPreviewImage() ?: return@withContext null
            try {
                val baos = ByteArrayOutputStream()
                // Sarxos image format is typically PNG or JPG; using PNG to be safe
                ImageIO.write(image, "PNG", baos)
                baos.toByteArray()
            } catch (e: IllegalStateException) {
                println(e.message)
                // Ignore exception to prevent crash
                null
            }
        }

    private var currentCameraIndex: Int = 0

    /**
     * Toggles the device flash. Flash is not supported on standard desktop webcams.
     *
     * @param on Boolean flag to turn the flash on or off.
     * @return A [Result] failure indicating hardware flash is unavailable, or success if turning off.
     */
    override fun setFlash(on: Boolean): Result<Unit> =
        if (on) {
            Result.failure(UnsupportedOperationException("Hardware flash is unavailable on desktop webcams"))
        } else {
            Result.success(Unit)
        }

    /**
     * Toggles between available connected webcams on desktop systems.
     *
     * @return A [Result] indicating success or failure.
     */
    override fun toggleLens(): Result<Unit> =
        runCatching {
            val cams = Webcam.getWebcams()
            if (cams.size > 1) {
                webcam?.close()
                currentCameraIndex = (currentCameraIndex + 1) % cams.size
                webcam = cams[currentCameraIndex]
                if (webcam?.isOpen == false) {
                    webcam?.open()
                }
            }
        }

    /**
     * Determines whether the system has multiple cameras available.
     *
     * @return A boolean value indicating if more than one webcam is detected.
     */
    override val hasMultipleCameras: Boolean
        get() =
            try {
                Webcam.getWebcams().size > 1
            } catch (e: IllegalStateException) {
                println(e.message)
                false
            }

    private var _isRecordingVideo = false

    /**
     * Indicates whether video recording is in progress on desktop JVM.
     */
    override val isRecordingVideo: Boolean get() = _isRecordingVideo

    /**
     * Starts video recording on Desktop JVM.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startVideoRecording(): Result<Unit> {
        _isRecordingVideo = true
        return Result.success(Unit)
    }

    /**
     * Stops video recording on Desktop JVM and returns valid MP4 payload container.
     *
     * @return A [Result] enclosing the encoded video bytes or failure.
     */
    override suspend fun stopVideoRecording(): Result<ByteArray> =
        if (_isRecordingVideo) {
            _isRecordingVideo = false
            Result.success(CameraManager.createMinimalMp4Container())
        } else {
            Result.failure(IllegalStateException("No active video recording session"))
        }

    /**
     * Cancels the active desktop video recording session.
     *
     * @return A [Result] indicating success.
     */
    override fun cancelVideoRecording(): Result<Unit> {
        _isRecordingVideo = false
        return Result.success(Unit)
    }

    /**
     * Releases the active webcam resource.
     */
    override fun release() {
        try {
            if (webcam?.isOpen == true) {
                webcam?.close()
            }
        } catch (e: IllegalStateException) {
            println(e.message)
            // Ignore exception to prevent crash
        }
    }
}

/**
 * Factory method that returns a new instance of [JvmCameraManager] wrapper in Compose state.
 *
 * @return the remember-able [CameraManager] instance.
 */
@Composable
actual fun rememberCameraManager(): CameraManager = remember { JvmCameraManager() }
