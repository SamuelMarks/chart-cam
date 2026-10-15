/**
 * @file CameraManagerWasmJsTest.kt
 * Contains WasmJS unit tests for [JsCameraManager] and [JsPermissionManager].
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [JsCameraManager] and [JsPermissionManager] on the WasmJS target.
 */
class CameraManagerWasmJsTest {
    /**
     * Verifies [JsCameraManager] video element configuration, flash, lens toggling, release,
     * and [JsPermissionManager] status and permission request results on WasmJS.
     */
    @Test
    fun testCameraManagerWasmJs() {
        val manager = JsCameraManager()
        assertNotNull(manager.videoElement)
        assertTrue(manager.videoElement.autoplay)
        assertTrue(manager.videoElement.muted)
        assertEquals("true", manager.videoElement.getAttribute("playsinline"))
        assertFalse(manager.isRecordingVideo)

        assertTrue(manager.setFlash(true).isSuccess)
        assertTrue(manager.setFlash(false).isSuccess)
        assertTrue(manager.toggleLens().isSuccess)
        assertTrue(manager.toggleLens().isSuccess)
        manager.release()

        val permissionManager = JsPermissionManager()
        assertEquals(PermissionStatus.GRANTED, permissionManager.getCameraPermissionStatus())
        assertTrue(permissionManager.queryCameraPermissionStatus().isSuccess)
        permissionManager.openSettings()
    }
}
