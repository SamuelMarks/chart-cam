/**
 * @file PermissionManager.jvm.kt
 * Camera permission management for the JVM platform.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

/**
 * A JVM-specific implementation of [PermissionManager].
 * Desktop environments typically do not enforce runtime camera permissions in the same way
 * mobile operating systems do, so this always reports permissions as granted.
 */
class JvmPermissionManager : PermissionManager {
    /**
     * Gets the current camera permission status. Always returns [PermissionStatus.GRANTED] on JVM.
     *
     * @return The current [PermissionStatus].
     */
    override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.GRANTED

    /**
     * Requests camera permission from the user. Always returns true immediately on JVM.
     *
     * @return A boolean indicating whether the permission was granted (always true).
     */
    override suspend fun requestCameraPermission(): Boolean = true

    /**
     * Opens the system settings screen for permissions. This is a no-op on JVM.
     */
    override fun openSettings() { /* no-op */ }
}

/**
 * Remembers and creates a new instance of [PermissionManager] for the JVM platform.
 *
 * @return A [PermissionManager] implementation for Desktop.
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = JvmPermissionManager()
