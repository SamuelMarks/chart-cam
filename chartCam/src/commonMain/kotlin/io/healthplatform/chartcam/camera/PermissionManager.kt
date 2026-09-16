/**
 * @file PermissionManager.kt
 * Contains declarations for PermissionManager.kt.
 *
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
     * Queries the current camera permission status safely wrapped in a [Result].
     *
     * @return A [Result] enclosing the resolved [PermissionStatus].
     */
    fun queryCameraPermissionStatus(): Result<PermissionStatus> = Result.success(getCameraPermissionStatus())

    /**
     * Requests the camera permission from the operating system.
     * Suspends execution until the user responds to the system prompt.
     *
     * @return A [Result] indicating success if granted, or [PermissionDeniedException] if denied.
     */
    suspend fun requestCameraPermission(): Result<Unit>

    /**
     * Dispatches an Intent or URL to open the system settings app
     * to the page for this application, useful if a permission is permanently denied.
     */
    fun openSettings()
}

/**
 * Exception indicating that the requested camera permission was denied by the user or system.
 *
 * @param isPermanentlyDenied True if the user selected "Don't ask again" or if denied by device policy.
 * @param message Detail message regarding the permission rejection.
 */
class PermissionDeniedException(
    val isPermanentlyDenied: Boolean = false,
    override val message: String = "Camera permission was denied",
) : Exception(message)

/**
 * Composable helper to create and remember a [PermissionManager] instance scoped to the composition.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @return A [PermissionManager] valid for the current platform and context.
 */
@Composable
expect fun rememberPermissionManager(): PermissionManager
