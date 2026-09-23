/**
 * @file PermissionManager.jvm.kt
 * Camera permission management for the JVM platform.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import com.github.sarxos.webcam.Webcam

/**
 * A JVM-specific implementation of [PermissionManager].
 * Checks for connected webcam hardware and provides OS settings navigation.
 */
class JvmPermissionManager : PermissionManager {
    /**
     * Companion object providing configuration overrides for [JvmPermissionManager].
     */
    companion object {
        /**
         * Optional provider override for detected webcams during unit tests.
         */
        var defaultWebcamSupplier: (() -> List<Webcam>?)? = null
    }

    private val webcamSupplier: () -> List<Webcam>?

    /**
     * Default constructor using standard Sarxos Webcam detection.
     */
    constructor() : this(defaultWebcamSupplier ?: { JvmCameraManager.createDefaultWebcams() })

    /**
     * Testing constructor allowing custom webcam detection provider.
     *
     * @param webcamSupplier The provider function returning detected webcams.
     */
    constructor(webcamSupplier: () -> List<Webcam>?) {
        this.webcamSupplier = webcamSupplier
    }

    /**
     * Gets the current camera permission status based on webcam hardware availability.
     *
     * @return [PermissionStatus.GRANTED] if hardware is present, [PermissionStatus.DENIED] otherwise.
     */
    override fun getCameraPermissionStatus(): PermissionStatus {
        val webcams = runCatching { webcamSupplier() }.getOrNull()
        return if (!webcams.isNullOrEmpty()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    /**
     * Requests camera permission from the user. Returns success if a webcam is available.
     *
     * @return A [Result] indicating success of the permission grant.
     */
    override suspend fun requestCameraPermission(): Result<Unit> =
        if (getCameraPermissionStatus() == PermissionStatus.GRANTED) {
            Result.success(Unit)
        } else {
            Result.failure(
                PermissionDeniedException(
                    isPermanentlyDenied = true,
                    message = "No webcam device detected on desktop host system.",
                ),
            )
        }

    /**
     * Opens the desktop operating system camera privacy settings if supported.
     */
    override fun openSettings() {
        runCatching {
            val prop = System.getProperty("os.name")
            val os = if (prop != null) prop.lowercase() else ""
            if (os.contains("mac")) {
                ProcessBuilder("open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Camera").start()
            } else if (os.contains("win")) {
                ProcessBuilder("cmd", "/c", "start", "ms-settings:privacy-webcam").start()
            }
        }
    }
}

/**
 * Remembers and creates a new instance of [PermissionManager] for the JVM platform.
 *
 * @return A [PermissionManager] implementation for Desktop.
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = JvmPermissionManager()
