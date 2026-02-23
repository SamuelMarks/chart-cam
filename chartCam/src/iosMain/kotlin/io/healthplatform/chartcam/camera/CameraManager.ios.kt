package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.*
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.darwin.NSObject

/**
 * iOS implementation of CameraManager.
 * Manages AVCaptureSession.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSCameraManager : CameraManager {
    
    val captureSession = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private var videoDeviceInput: AVCaptureDeviceInput? = null
    
    // Maintain a strong reference to the delegate so it doesn't get deallocated
    // prematurely before the photo capture completes
    private var activeDelegate: AVCapturePhotoCaptureDelegateProtocol? = null
    
    init {
        configureSession()
    }
    
    private fun configureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return
        
        if (captureSession.canAddInput(input)) {
            captureSession.addInput(input)
            videoDeviceInput = input as AVCaptureDeviceInput
        }
        
        if (captureSession.canAddOutput(photoOutput)) {
            captureSession.addOutput(photoOutput)
        }
        
        captureSession.commitConfiguration()
        
        if (!captureSession.running) {
             captureSession.startRunning()
        }
    }

    override suspend fun captureImage(): ByteArray? = suspendCoroutine { continuation ->
        // The crash occurs if we try to capture when there's no active video connection or session isn't running.
        if (!captureSession.running || photoOutput.connections.isEmpty()) {
            continuation.resume(null)
            return@suspendCoroutine
        }
    
        val settings = AVCapturePhotoSettings.photoSettings()
        
        val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?
            ) {
                activeDelegate = null
                
                if (error != null) {
                    continuation.resume(null)
                    return
                }
                
                val data = didFinishProcessingPhoto.fileDataRepresentation()
                if (data != null) {
                    val bytes = data.toByteArray()
                    continuation.resume(bytes)
                } else {
                    continuation.resume(null)
                }
            }
        }
        
        activeDelegate = delegate
        
        try {
            photoOutput.capturePhotoWithSettings(settings, delegate)
        } catch (e: Exception) {
            activeDelegate = null
            continuation.resume(null)
        }
    }

    override fun setFlash(on: Boolean) {
        val device = videoDeviceInput?.device ?: return
        if (device.hasTorch) {
            try {
                device.lockForConfiguration(null)
                device.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                device.unlockForConfiguration()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun toggleLens() {
        // Implementation omitted for brevity
    }

    override fun release() {
        captureSession.stopRunning()
    }
}

/**
 * Extension to convert NSData to ByteArray.
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    this.bytes?.let { pointer ->
        byteArray.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), pointer, length.toULong())
        }
    }
    return byteArray
}

@Composable
actual fun rememberCameraManager(): CameraManager {
    return remember { IOSCameraManager() }
}
