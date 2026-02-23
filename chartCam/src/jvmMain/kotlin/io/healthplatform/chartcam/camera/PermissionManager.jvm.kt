package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable

class JvmPermissionManager : PermissionManager {
    override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.GRANTED
    override suspend fun requestCameraPermission(): Boolean = true
    override fun openSettings() {}
}

@Composable
actual fun rememberPermissionManager(): PermissionManager = JvmPermissionManager()