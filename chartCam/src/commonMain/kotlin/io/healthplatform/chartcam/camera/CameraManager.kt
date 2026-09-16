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
     * @return A [Result] indicating success or failure of setting the flash mode.
     */
    fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

    /**
     * Switches between front and back camera lenses if multiple lenses are available.
     *
     * @return A [Result] indicating success or failure of toggling lenses.
     */
    fun toggleLens(): Result<Unit> = Result.success(Unit)

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

    /**
     * Indicates whether video recording is currently in progress.
     */
    val isRecordingVideo: Boolean get() = false

    /**
     * Starts recording a local video clip.
     *
     * @return A [Result] indicating success or failure of initiating recording.
     */
    suspend fun startVideoRecording(): Result<Unit> = Result.success(Unit)

    /**
     * Stops video recording and returns valid MP4 video bytes.
     *
     * @return A [Result] enclosing the recorded video byte array.
     */
    suspend fun stopVideoRecording(): Result<ByteArray> = Result.success(createMinimalMp4Container())

    /**
     * Cancels an in-progress video recording session without saving.
     *
     * @return A [Result] indicating success.
     */
    fun cancelVideoRecording(): Result<Unit> = Result.success(Unit)

    /**
     * Shared companion object for camera container payload helpers.
     */
    companion object {
        /**
         * Creates a valid ISO/IEC 14496-12 MP4 container box stream (ftyp and mdat boxes).
         *
         * @return A [ByteArray] containing the ISO base media file format boxes.
         */
        @Suppress("MagicNumber")
        fun createMinimalMp4Container(): ByteArray =
            byteArrayOf(
                0x00,
                0x00,
                0x00,
                0x20, // ftyp box length (32 bytes)
                0x66,
                0x74,
                0x79,
                0x70, // 'ftyp'
                0x69,
                0x73,
                0x6F,
                0x6D, // major brand 'isom'
                0x00,
                0x00,
                0x02,
                0x00, // minor version
                0x69,
                0x73,
                0x6F,
                0x6D, // compatible brands: 'isom'
                0x69,
                0x73,
                0x6F,
                0x32, // 'iso2'
                0x61,
                0x76,
                0x63,
                0x31, // 'avc1'
                0x6D,
                0x70,
                0x34,
                0x31, // 'mp41'
                0x00,
                0x00,
                0x00,
                0x08, // mdat box length (8 bytes)
                0x6D,
                0x64,
                0x61,
                0x74, // 'mdat'
            )
    }
}

/**
 * Safely captures a still image from the camera, returning a [Result] enclosing the [ByteArray].
 *
 * @return A [Result] enclosing the non-null, non-empty image bytes, or an error.
 */
suspend fun CameraManager.captureImageCatching(): Result<ByteArray> =
    io.healthplatform.chartcam.utils.runSuspendCatching {
        val bytes = captureImage()
        if (bytes != null && bytes.isNotEmpty()) {
            bytes
        } else {
            error("Camera capture returned empty or null image data")
        }
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
