package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.toJsString
import okio.ByteString.Companion.decodeBase64

private fun getVideoConstraints(mode: JsAny): org.w3c.dom.mediacapture.MediaStreamConstraints = js("({ video: { facingMode: mode } })")

private fun consoleError(msg: String, err: JsAny?) {
    js("console.error(msg, err)")
}

private fun stopMediaTracks(stream: JsAny?) {
    js("if (stream && stream.getTracks) { stream.getTracks().forEach(t => t.stop()); }")
}

private fun getBase64Image(video: HTMLVideoElement): String = js("""
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
""")

class JsCameraManager : CameraManager {
    val videoElement: HTMLVideoElement = document.createElement("video") as HTMLVideoElement
    private var isFrontFacing = false

    init {
        videoElement.autoplay = true
        videoElement.muted = true
        videoElement.setAttribute("playsinline", "true")
        
        startCamera()
    }

    private fun startCamera() {
        val mode = if (isFrontFacing) "user" else "environment"
        val constraints = getVideoConstraints(mode.toJsString())
        window.navigator.mediaDevices.getUserMedia(constraints)
            .then { stream ->
                videoElement.srcObject = stream
                videoElement.play()
                null
            }.catch { err ->
                consoleError("Error accessing camera: ", err)
                null
            }
    }

    override suspend fun captureImage(): ByteArray? = suspendCoroutine { continuation ->
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

    override fun setFlash(on: Boolean) {}
    
    override fun toggleLens() {
        isFrontFacing = !isFrontFacing
        release()
        startCamera()
    }
    
    override fun release() {
        stopMediaTracks(videoElement.srcObject as? JsAny)
        videoElement.srcObject = null
    }
}

@Composable 
actual fun rememberCameraManager(): CameraManager {
    val manager = remember { JsCameraManager() }
    DisposableEffect(manager) {
        onDispose { manager.release() }
    }
    return manager
}

class JsPermissionManager : PermissionManager {
    override fun getCameraPermissionStatus() = PermissionStatus.GRANTED
    override suspend fun requestCameraPermission() = true
    override fun openSettings() {}
}

@Composable 
actual fun rememberPermissionManager(): PermissionManager = JsPermissionManager()
