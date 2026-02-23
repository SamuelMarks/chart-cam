package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

/**
 * Interface defining the capabilities of the ChartCam camera.
 * This abstraction allows Shared Code to trigger captures and manage camera state
 * without knowing about Android CameraX or iOS AVFoundation.
 */
interface CameraManager {
    /**
     * Captures a still image from the active camera stream.
     *
     * @return A ByteArray representing the image (JPEG encoded) or null if capture failed.
     */
    suspend fun captureImage(): ByteArray?

    /**
     * Toggles the flash mode if supported.
     * @param on True to enable flash, false to disable.
     */
    fun setFlash(on: Boolean)

    /**
     * Switches between front and back lens if available.
     */
    fun toggleLens()
    
    /**
     * Releases resources when the camera is no longer needed.
     */
    fun release()

    val hasMultipleCameras: Boolean get() = true
}

/**
 * Factory function to create a CameraManager instance.
 * Note: CameraManager usually requires binding to a lifecycle or view via [CameraPreview],
 * so this factory is often used internally by the Preview composable.
 */
@Composable
expect fun rememberCameraManager(): CameraManager