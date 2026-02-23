package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.sarxos.webcam.Webcam
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * JVM implementation of [CameraManager] using the Sarxos webcam-capture library.
 * This provides real camera functionality for Desktop targets, fully supporting
 * the Photo Capture feature.
 */
class JvmCameraManager : CameraManager {
    
    companion object {
        init {
            try {
                Webcam.setDriver(NativeDriver())
            } catch (e: Exception) {
                // Driver might already be set or failed to initialize
            }
        }
    }

    /**
     * The internal webcam instance.
     */
    private var webcam: Webcam? = null

    /**
     * Initializes the webcam instance securely off the main thread.
     */
    private suspend fun getWebcam(): Webcam? = withContext(Dispatchers.IO) {
        if (webcam == null) {
            try {
                webcam = Webcam.getDefault()
            } catch (e: Exception) {
                // Return null if webcam lookup fails
            }
        }
        webcam
    }

    /**
     * Gets a raw BufferedImage for preview purposes.
     */
    suspend fun getPreviewImage(): BufferedImage? = withContext(Dispatchers.IO) {
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
     * @return A ByteArray representing the image (PNG encoded) or null if capture failed/webcam not available.
     */
    override suspend fun captureImage(): ByteArray? = withContext(Dispatchers.IO) {
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
     * Flash is typically not supported on standard desktop webcams.
     * @param on Boolean flag which is ignored.
     */
    override fun setFlash(on: Boolean) {
        // Not supported on standard desktop webcams
    }

    /**
     * Toggling lens is typically not supported on standard desktop webcams.
     */
    override fun toggleLens() {
        // Not supported on standard desktop webcams
    }

    /**
     * Releases the webcam resource.
     */
    override val hasMultipleCameras: Boolean
        get() = try { Webcam.getWebcams().size > 1 } catch (e: Exception) { false }

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
 * @return the remember-able [CameraManager] instance
 */
@Composable
actual fun rememberCameraManager(): CameraManager {
    return remember { JvmCameraManager() }
}
