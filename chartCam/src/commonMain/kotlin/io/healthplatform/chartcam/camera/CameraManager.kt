/**
 * @file CameraManager.kt
 * Contains declarations for CameraManager.kt.
 *
 * Contains cross-platform abstractions for camera management and configuration.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

/**
 * Interface defining the capabilities of the ChartCam camera.
 * This abstraction allows Shared Code to trigger captures and manage camera state
 * without knowing about underlying platform APIs like Android CameraX or iOS AVFoundation.
 */
interface CameraManager {
    /**
     * Captures a still image from the active camera stream.
     *
     * @return A [ByteArray] representing the image (JPEG encoded) or null if the capture failed.
     */
    suspend fun captureImage(): ByteArray?

    /**
     * Toggles the flash mode if supported by the underlying device hardware.
     *
     * @param on True to enable flash, false to disable.
     */
    fun setFlash(on: Boolean)

    /**
     * Switches between front and back camera lenses if multiple lenses are available.
     */
    fun toggleLens()

    /**
     * Releases camera resources when the camera is no longer needed.
     * Important to call to prevent battery drain or camera lockups on mobile OSs.
     */
    fun release()

    /**
     * Indicates whether the device has more than one camera available (e.g., front and back).
     * Used to conditionally display the camera flip button in the UI.
     */
    val hasMultipleCameras: Boolean get() = true
}

/**
 * Factory function to create or remember a [CameraManager] instance scoped to a Composable.
 * Note: CameraManager usually requires binding to a lifecycle or view via platform-specific
 * implementations, so this factory is often used internally by the Preview composable.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @return A [CameraManager] instance valid for the current composition.
 */
@Composable
expect fun rememberCameraManager(): CameraManager
