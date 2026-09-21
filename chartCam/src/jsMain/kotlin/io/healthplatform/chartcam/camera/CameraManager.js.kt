/**
 * @file CameraManager.js.kt
 * JS-specific camera management and permission implementations.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.json

private const val JPEG_QUALITY = 0.9

/**
 * Helper function to create JS video constraints.
 *
 * @param mode The facing mode (e.g., "user" or "environment").
 * @return A dynamic object representing the media constraints.
 */
private fun getVideoConstraints(mode: String): dynamic = json("video" to json("facingMode" to mode))

/**
 * Wrapper for logging errors to the JS console.
 *
 * @param msg The error message prefix.
 * @param err The dynamic error object from the browser.
 */
private fun consoleError(
    msg: String,
    err: dynamic,
) {
    console.error(msg, err)
}

/**
 * Stops all media tracks associated with a media stream.
 *
 * @param stream The dynamic media stream object.
 */
private fun stopMediaTracks(stream: dynamic) {
    if (stream != null && stream.getTracks != null) {
        val tracks = stream.getTracks()
        for (i in 0 until tracks.length as Int) {
            tracks[i].stop()
        }
    }
}

/**
 * Draws a video frame to a 2D canvas context.
 *
 * @param ctx The 2D rendering context of the canvas.
 * @param video The HTML video element source.
 * @param w The width to draw.
 * @param h The height to draw.
 */
private fun drawImageToCanvas(
    ctx: org.w3c.dom.CanvasRenderingContext2D,
    video: HTMLVideoElement,
    w: Double,
    h: Double,
) {
    ctx.drawImage(video, 0.0, 0.0, w, h)
}

/**
 * JS Web implementation of the [CameraManager] interface.
 * Uses WebRTC `getUserMedia` to access device cameras.
 */
class JsCameraManager : CameraManager {
    /**
     * The internal HTML video element used to display the camera stream.
     */
    val videoElement: HTMLVideoElement = document.createElement("video") as HTMLVideoElement

    /**
     * Tracks whether the currently active camera is the front-facing (user) camera.
     */
    private var isFrontFacing = false

    init {
        videoElement.autoplay = true
        videoElement.setAttribute("playsinline", "true")

        startCamera()
    }

    /**
     * Initializes the camera stream using current [isFrontFacing] state.
     */
    private fun startCamera() {
        val mode = if (isFrontFacing) "user" else "environment"
        val constraints = getVideoConstraints(mode)
        window.navigator.mediaDevices
            .getUserMedia(constraints)
            .then { stream ->
                videoElement.srcObject = stream
                null
            }.catch { err ->
                // allow-exception
                consoleError("Error accessing camera: ", err)
                null
            }
    }

    /**
     * Captures a still image from the current video stream frame.
     * Uses a temporary canvas element to extract the frame as JPEG data.
     *
     * @return A byte array representing the captured JPEG image, or null if capture fails.
     */
    override suspend fun captureImage(): ByteArray? =
        suspendCoroutine { continuation ->
            val result =
                runCatching {
                    val canvas = document.createElement("canvas") as HTMLCanvasElement
                    canvas.width = videoElement.videoWidth
                    canvas.height = videoElement.videoHeight
                    val ctx = canvas.getContext("2d") as org.w3c.dom.CanvasRenderingContext2D

                    drawImageToCanvas(ctx, videoElement, canvas.width.toDouble(), canvas.height.toDouble())

                    val dataUrl = canvas.toDataURL("image/jpeg", JPEG_QUALITY)
                    val base64 = dataUrl.substringAfter("base64,")

                    val decoded = window.atob(base64)
                    val bytes = ByteArray(decoded.length)
                    for (i in 0 until decoded.length) {
                        bytes[i] = decoded[i].code.toByte()
                    }
                    bytes
                }.onFailure { e ->
                    println(e.message)
                }
            continuation.resume(result.getOrNull())
        }

    /**
     * Sets the state of the device flashlight (torch) via track constraints.
     *
     * @param on True to turn the flash on, false to turn it off.
     * @return A [Result] indicating success or failure.
     */
    override fun setFlash(on: Boolean): Result<Unit> =
        runCatching {
            val stream = videoElement.srcObject.asDynamic()
            if (stream != null && stream.getVideoTracks != null) {
                val tracks = stream.getVideoTracks()
                if (tracks != null && tracks.length > 0) {
                    val track = tracks[0]
                    if (track != null && track.applyConstraints != null) {
                        val constraints = js("({ advanced: [{ torch: on }] })")
                        track.applyConstraints(constraints)
                    }
                }
            }
        }

    /**
     * Toggles between the front and rear cameras, if available.
     *
     * @return A [Result] indicating success or failure.
     */
    override fun toggleLens(): Result<Unit> =
        runCatching {
            isFrontFacing = !isFrontFacing
            release()
            startCamera()
        }

    /**
     * Stops the active media stream and releases camera resources.
     */
    override fun release() {
        stopMediaTracks(videoElement.srcObject.asDynamic())
        videoElement.srcObject = null
    }
}

/**
 * Remembers a [CameraManager] instance and ensures it is properly released on disposal.
 *
 * @return A [CameraManager] configured for the JS platform.
 */
@Composable
actual fun rememberCameraManager(): CameraManager {
    val manager = remember { JsCameraManager() }
    DisposableEffect(manager) {
        onDispose { manager.release() }
    }
    return manager
}

/**
 * JS implementation of the [PermissionManager] interface for camera access.
 * Web browsers generally handle permission prompts dynamically when `getUserMedia` is called.
 */
class JsPermissionManager : PermissionManager {
    /**
     * Retrieves the current camera permission status.
     * Returns [PermissionStatus.GRANTED] assuming the browser prompts automatically on stream request.
     *
     * @return The current permission status.
     */
    override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.GRANTED

    /**
     * Requests camera permission. Web handles this on `getUserMedia`.
     *
     * @return A [Result] indicating success of the permission grant.
     */
    override suspend fun requestCameraPermission(): Result<Unit> = Result.success(Unit)

    /**
     * Displays browser site settings guidance for managing camera permissions.
     */
    override fun openSettings() {
        runCatching {
            val guidance =
                "To manage camera permissions, please check your browser " +
                    "site settings or click the permissions icon in your address bar."
            kotlinx.browser.window.alert(guidance)
        }
    }
}

/**
 * Remembers a [PermissionManager] instance for the JS platform.
 *
 * @return A configured [PermissionManager].
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = JsPermissionManager()
