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
                /**
                 * Mock getCameraPermissionStatus.
                 * @return Always returns NOT_DETERMINED for this test.
                 */
                override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.NOT_DETERMINED

                /**
                 * Mock requestCameraPermission.
                 * @return Always returns false for this test.
                 */
                override suspend fun requestCameraPermission(): Boolean = false

                /** Mock openSettings. */
                override fun openSettings() {}
            }
        assertNotNull(manager)
    }
}
