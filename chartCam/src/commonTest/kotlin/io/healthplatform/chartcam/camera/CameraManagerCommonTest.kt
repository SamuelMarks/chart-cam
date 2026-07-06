package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraManagerCommonTest {
    // A simple mock of the camera manager interface for the common module
    class MockCameraManager : CameraManager {
        var captureCalled = false
        var flashSet: Boolean? = null
        var toggleCalled = false
        var releaseCalled = false

        override suspend fun captureImage(): ByteArray? {
            captureCalled = true
            return ByteArray(10)
        }

        override fun setFlash(on: Boolean) {
            flashSet = on
        }

        override fun toggleLens() {
            toggleCalled = true
        }

        override fun release() {
            releaseCalled = true
        }
    }

    class MockPermissionManager : PermissionManager {
        var status = PermissionStatus.NOT_DETERMINED
        var settingsOpened = false

        override fun getCameraPermissionStatus(): PermissionStatus = status

        override suspend fun requestCameraPermission(): Boolean {
            status = PermissionStatus.GRANTED
            return true
        }

        override fun openSettings() {
            settingsOpened = true
        }
    }

    @Test
    fun testCameraManagerInterfaceMethods() {
        val manager = MockCameraManager()

        manager.setFlash(true)
        assertEquals(true, manager.flashSet)

        manager.toggleLens()
        assertTrue(manager.toggleCalled)

        manager.release()
        assertTrue(manager.releaseCalled)

        // Testing default property
        assertTrue(manager.hasMultipleCameras)
    }

    @Test
    fun testPermissionManagerInterfaceMethods() {
        val manager = MockPermissionManager()

        assertEquals(PermissionStatus.NOT_DETERMINED, manager.getCameraPermissionStatus())

        manager.openSettings()
        assertTrue(manager.settingsOpened)
    }
}
