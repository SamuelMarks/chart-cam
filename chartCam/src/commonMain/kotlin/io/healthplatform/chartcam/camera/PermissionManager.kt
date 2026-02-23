package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

/**
 * Enum representing the status of a specific permission.
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}

/**
 * Interface for checking and requesting platform permissions.
 */
interface PermissionManager {
    /**
     * Checks the current status of the Camera permission.
     */
    fun getCameraPermissionStatus(): PermissionStatus

    /**
     * Requests the camera permission.
     * Suspends until the user responds.
     */
    suspend fun requestCameraPermission(): Boolean
    
    /**
     * Intent to open system settings if permission is permanently denied.
     */
    fun openSettings()
}

/**
 * Composable helper to remember the permission manager.
 */
@Composable
expect fun rememberPermissionManager(): PermissionManager