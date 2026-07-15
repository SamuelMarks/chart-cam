/**
 * @file CameraManager.wasmJs.kt
 * Contains declarations for CameraManager.wasmJs.kt.
 */
@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS", "USELESS_CAST")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
/**
 * @file CameraManager.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of [CameraManager] and [PermissionManager],
 * handling direct interaction with the browser's `MediaDevices` API for capturing video and photos.
 */

package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import okio.ByteString.Companion.decodeBase64
import org.w3c.dom.HTMLVideoElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.toJsString

/**
 * Creates JavaScript `MediaStreamConstraints` configured for video with a specific facing mode.
 *
 * @param mode The facing mode (e.g., "user" for front camera, "environment" for rear camera) as a [JsAny].
 * @return A configured [org.w3c.dom.mediacapture.MediaStreamConstraints] object.
 */
private fun getVideoConstraints(mode: JsAny): org.w3c.dom.mediacapture.MediaStreamConstraints = js("({ video: { facingMode: mode } })")

/**
 * Logs an error message to the browser console.
 *
 * @param msg The error message to log.
 * @param err An optional JavaScript error object to include in the log.
 */
private fun consoleError(
    msg: String,
    err: JsAny?,
) {
    js("console.error(msg, err)")
}

/**
 * Stops all tracks associated with a given media stream.
 *
 * @param stream The media stream (e.g., [org.w3c.dom.mediacapture.MediaStream]) represented as a [JsAny].
 */
private fun stopMediaTracks(stream: JsAny?) {
    js("if (stream && stream.getTracks) { stream.getTracks().forEach(t => t.stop()); }")
}

/**
 * Captures the current frame from an [HTMLVideoElement] and returns it as a base64 encoded JPEG string.
 *
 * @param video The HTML video element to capture.
 * @return A base64-encoded string representing the captured frame.
 */
private fun getBase64Image(video: HTMLVideoElement): String =
    js(
        """
    (() => {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth || 640;
        canvas.height = video.videoHeight || 480;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
        const base64 = dataUrl.split(',')[1];
        if (!base64) throw new Error("Could not get base64 data");
        return base64;
    })()
""",
    )

/**
 * WebAssembly (WasmJs) specific implementation of [CameraManager].
 * Manages access to the device's camera via browser APIs.
 */
class JsCameraManager : CameraManager {
    /**
     * The hidden HTML video element used to play the camera stream for capturing frames.
     */
    val videoElement: HTMLVideoElement = document.createElement("video") as HTMLVideoElement

    /**
     * Indicates whether the front-facing camera is currently active.
     */
    private var isFrontFacing = false

    init {
        videoElement.autoplay = true
        videoElement.muted = true
        videoElement.setAttribute("playsinline", "true")

        startCamera()
    }

    /**
     * Requests access to the camera and begins playing the stream in [videoElement].
     */
    private fun startCamera() {
        val mode = if (isFrontFacing) "user" else "environment"
        val constraints = getVideoConstraints(mode.toJsString())
        window.navigator.mediaDevices
            .getUserMedia(constraints)
            .then { stream ->
                videoElement.srcObject = stream
                videoElement.play()
                null
            }.catch { err ->
                consoleError("Error accessing camera: ", err)
                null
            }
    }

    /**
     * Captures a single image from the current camera feed.
     *
     * @return A byte array containing the JPEG image data, or null if capture failed.
     */
    override suspend fun captureImage(): ByteArray? =
        suspendCoroutine { continuation ->
            try {
                val base64 = getBase64Image(videoElement)
                val bytes = base64.decodeBase64()?.toByteArray()
                if (bytes != null) {
                    continuation.resume(bytes)
                } else {
                    continuation.resume(null)
                }
            } catch (e: Throwable) {
                consoleError("Error capturing image: ", e.message?.toJsString())
                continuation.resume(null)
            }
        }

    /**
     * Toggles the camera flash (torch) on or off.
     * Note: Flash control is currently not implemented on the Web (WasmJs) target.
     *
     * @param on True to enable the flash, false to disable it.
     */
    override fun setFlash(on: Boolean) {}

    /**
     * Switches the active camera between the front-facing and rear-facing lenses.
     */
    override fun toggleLens() {
        isFrontFacing = !isFrontFacing
        release()
        startCamera()
    }

    /**
     * Releases resources associated with the camera, stopping all media tracks.
     */
    override fun release() {
        stopMediaTracks(videoElement.srcObject as? JsAny)
        videoElement.srcObject = null
    }
}

/**
 * Remembers and provisions a WasmJs-specific [CameraManager] instance.
 * Automatically handles releasing the camera when the composable is disposed.
 *
 * @return An active [CameraManager] ready to capture video/photos.
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
 * WebAssembly (WasmJs) specific implementation of [PermissionManager].
 * Note: Browser permissions are typically handled dynamically by the browser via prompts,
 * so this implementation acts primarily as a stub that assumes access or relies on native browser flow.
 */
class JsPermissionManager : PermissionManager {
    /**
     * Retrieves the current camera permission status. Always returns GRANTED as a stub for web.
     *
     * @return The [PermissionStatus.GRANTED] status.
     */
    override fun getCameraPermissionStatus() = PermissionStatus.GRANTED

    /**
     * Requests camera permission from the user. For the web, this is usually implicit when accessing the media stream.
     *
     * @return True, assuming permission is implicitly granted or handled by the browser flow.
     */
    override suspend fun requestCameraPermission() = true

    /**
     * Opens application settings. This is a no-op on the web platform.
     */
    override fun openSettings() {}
}

/**
 * Remembers and provisions a WasmJs-specific [PermissionManager].
 *
 * @return A [PermissionManager] instance for the WasmJs platform.
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = JsPermissionManager()
