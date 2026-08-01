/**
 * @file PermissionManager.ios.kt
 * Contains declarations for PermissionManager.ios.kt.
 *
 * iOS implementation of the camera permission manager.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS-specific implementation of [PermissionManager].
 *
 * This class uses the AVFoundation framework to check and request camera permissions,
 * and UIKit to navigate to the app settings if permissions are denied.
 */
class IosPermissionManager : PermissionManager {
    /**
     * Retrieves the current camera permission status.
     *
     * Queries the system using [authorizationStatusForMediaType] for video.
     *
     * @return The current [PermissionStatus], such as [PermissionStatus.GRANTED],
     *         [PermissionStatus.DENIED], or [PermissionStatus.NOT_DETERMINED].
     */
    override fun getCameraPermissionStatus(): PermissionStatus {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return when (status) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
            AVAuthorizationStatusDenied -> PermissionStatus.DENIED
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
            else -> PermissionStatus.DENIED
        }
    }

    /**
     * Requests camera permission from the user if not already granted or denied.
     *
     * If the status is undetermined, it prompts the user using
     * [requestAccessForMediaType] and suspends until the user responds.
     *
     * @return `true` if permission is granted, `false` otherwise. Note that iOS
     *         does not allow re-prompting once denied, so it will return `false`
     *         immediately if previously denied.
     */
    override suspend fun requestCameraPermission(): Boolean {
        val status = getCameraPermissionStatus()
        return when (status) {
            PermissionStatus.GRANTED -> true
            PermissionStatus.DENIED -> false
            else ->
                suspendCoroutine { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        continuation.resume(granted)
                    }
                }
        }
    }

    /**
     * Opens the iOS application settings.
     *
     * This is useful when the user has denied camera access and needs to manually
     * enable it from the system settings.
     */
    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null) {
            UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any?>(), null)
        }
    }
}

/**
 * Creates and remembers an iOS-specific [PermissionManager].
 *
 * @return An instance of [IosPermissionManager] managed by Compose.
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = remember { IosPermissionManager() }
