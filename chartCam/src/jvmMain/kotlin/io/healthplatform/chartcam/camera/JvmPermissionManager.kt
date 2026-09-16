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
     * Gets the current camera permission status based on webcam hardware availability.
     *
     * @return [PermissionStatus.GRANTED] if hardware is present, [PermissionStatus.DENIED] otherwise.
     */
    override fun getCameraPermissionStatus(): PermissionStatus {
        val webcams = runCatching { Webcam.getWebcams() }.getOrNull()
        return if (webcams != null && webcams.isNotEmpty()) {
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
            val os = System.getProperty("os.name")?.lowercase() ?: ""
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
