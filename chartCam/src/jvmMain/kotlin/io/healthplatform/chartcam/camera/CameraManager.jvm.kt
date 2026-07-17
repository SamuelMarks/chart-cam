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
                } catch (t: Throwable) {
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
                } catch (t: Throwable) {
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
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Captures a still image from the active desktop webcam.
     *
     * @return A [ByteArray] representing the image (PNG encoded), or null if the capture failed or the webcam is not available.
     */
    override suspend fun captureImage(): ByteArray? =
        withContext(Dispatchers.IO) {
            val image = getPreviewImage() ?: return@withContext null
            try {
                val baos = ByteArrayOutputStream()
                // Sarxos image format is typically PNG or JPG; using PNG to be safe
                ImageIO.write(image, "PNG", baos)
                baos.toByteArray()
            } catch (e: Exception) {
                // Ignore exception to prevent crash
                null
            }
        }

    /**
     * Toggles the device flash. Flash is typically not supported on standard desktop webcams.
     *
     * @param on Boolean flag to turn the flash on or off, which is ignored on this platform.
     */
    override fun setFlash(on: Boolean) {
        // Not supported on standard desktop webcams
    }

    /**
     * Toggling the camera lens (e.g. front to back). This is typically not supported on standard desktop webcams.
     */
    override fun toggleLens() {
        // Not supported on standard desktop webcams
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
            } catch (e: Exception) {
                false
            }

    /**
     * Releases the active webcam resource.
     */
    override fun release() {
        try {
            if (webcam?.isOpen == true) {
                webcam?.close()
            }
        } catch (e: Exception) {
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
