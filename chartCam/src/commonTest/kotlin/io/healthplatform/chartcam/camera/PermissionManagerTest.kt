package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

class PermissionManagerTest {
    @Test
    fun testPermissionManagerInterface() {
        val manager =
            object : PermissionManager {
                override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.NOT_DETERMINED

                override suspend fun requestCameraPermission(): Boolean = false

                override fun openSettings() {}
            }
        assertNotNull(manager)
    }
}
