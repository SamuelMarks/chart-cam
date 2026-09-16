/**
 * @file IosCameraManagerTest.kt
 * Contains declarations for IosCameraManagerTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Unit tests evaluating the iOS CameraManager and PermissionManager initialization.
 */
class IosCameraManagerTest {
    /**
     * Verifies that IOSCameraManager instantiates its internal AVCaptureSession.
     */
    @Test
    fun testCameraManagerInstantiation() {
        val manager = IOSCameraManager()
        assertNotNull(manager.captureSession, "Capture session should be initialized")
        manager.release()
    }
}

/**
 * Test wrapper for iOS PermissionManager behaviors.
 */
class IosPermissionManagerTest {
    /**
     * Verifies that IosPermissionManager instantiates and queries permission status.
     */
    @Test
    fun testPermissionManagerStatus() {
        val permissionManager = IosPermissionManager()
        val status = permissionManager.getCameraPermissionStatus()
        assertNotNull(status, "Permission status should be resolved")
    }
}
