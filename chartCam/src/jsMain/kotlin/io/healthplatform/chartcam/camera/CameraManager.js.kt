package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private fun getVideoConstraints(mode: String): dynamic = js("({ video: { facingMode: mode } })")

private fun consoleError(msg: String, err: dynamic) {
    console.error(msg, err)
}

private fun stopMediaTracks(stream: dynamic) {
    if (stream != null && stream.getTracks != null) {
        val tracks = stream.getTracks()
        for (i in 0 until tracks.length as Int) {
            tracks[i].stop()
        }
    }
}

private fun drawImageToCanvas(ctx: org.w3c.dom.CanvasRenderingContext2D, video: HTMLVideoElement, w: Double, h: Double) {
    ctx.drawImage(video, 0.0, 0.0, w, h)
}

class JsCameraManager : CameraManager {
    val videoElement: HTMLVideoElement = document.createElement("video") as HTMLVideoElement
    private var isFrontFacing = false

    init {
        videoElement.autoplay = true
        videoElement.setAttribute("playsinline", "true")
        
        startCamera()
    }

    private fun startCamera() {
        val mode = if (isFrontFacing) "user" else "environment"
        val constraints = getVideoConstraints(mode)
        window.navigator.mediaDevices.getUserMedia(constraints)
            .then { stream ->
                videoElement.srcObject = stream
                null
            }.catch { err ->
                consoleError("Error accessing camera: ", err)
                null
            }
    }

    override suspend fun captureImage(): ByteArray? = suspendCoroutine { continuation ->
        try {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = videoElement.videoWidth
            canvas.height = videoElement.videoHeight
            val ctx = canvas.getContext("2d") as org.w3c.dom.CanvasRenderingContext2D
            
            drawImageToCanvas(ctx, videoElement, canvas.width.toDouble(), canvas.height.toDouble())

            val dataUrl = canvas.toDataURL("image/jpeg", 0.9)
            val base64 = dataUrl.substringAfter("base64,")
            
            val decoded = window.atob(base64)
            val bytes = ByteArray(decoded.length)
            for (i in 0 until decoded.length) {
                bytes[i] = decoded[i].code.toByte()
            }
            continuation.resume(bytes)
        } catch (e: Exception) {
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
        stopMediaTracks(videoElement.srcObject.asDynamic())
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
