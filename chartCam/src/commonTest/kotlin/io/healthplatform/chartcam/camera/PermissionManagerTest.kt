/**
 * @file PermissionManagerTest.kt
 * Contains declarations for PermissionManagerTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for the [PermissionManager] interface.
 */
class PermissionManagerTest {
    /**
     * Validates that [PermissionManager] can be implemented by an anonymous object.
     */
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
