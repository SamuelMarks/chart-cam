/**
 * Contains cross-platform abstractions for permission management.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

/**
 * Enum representing the status of a specific system permission.
 */
enum class PermissionStatus {
    /**
     * The permission has been explicitly granted by the user.
     */
    GRANTED,

    /**
     * The permission has been explicitly denied by the user.
     */
    DENIED,

    /**
     * The permission status has not yet been determined (e.g., the user hasn't been asked).
     */
    NOT_DETERMINED,
}

/**
 * Interface for checking and requesting platform-specific system permissions.
 * Abstracts away the differences between Android's ActivityResultContracts
 * and iOS's AVAuthorizationStatus.
 */
interface PermissionManager {
    /**
     * Checks the current status of the Camera permission.
     *
     * @return The current [PermissionStatus] for camera access.
     */
    fun getCameraPermissionStatus(): PermissionStatus

    /**
     * Requests the camera permission from the operating system.
     * Suspends execution until the user responds to the system prompt.
     *
     * @return True if the permission is granted after the prompt, false otherwise.
     */
    suspend fun requestCameraPermission(): Boolean

    /**
     * Dispatches an Intent or URL to open the system settings app
     * to the page for this application, useful if a permission is permanently denied.
     */
    fun openSettings()
}

/**
 * Composable helper to create and remember a [PermissionManager] instance scoped to the composition.
 *
 * @return A [PermissionManager] valid for the current platform and context.
 */
@Composable
expect fun rememberPermissionManager(): PermissionManager
