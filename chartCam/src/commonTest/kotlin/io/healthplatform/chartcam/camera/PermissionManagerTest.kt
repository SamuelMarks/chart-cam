/**
 * @file PermissionManagerTest.kt
 * Contains unit tests for PermissionManager interface, PermissionStatus enum, and PermissionDeniedException.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the [PermissionManager] interface and related permission domain entities.
 */
class PermissionManagerTest {
    /**
     * Validates that [PermissionManager] can be implemented by an anonymous object and default methods work.
     */
    @Test
    fun testPermissionManagerInterface() {
        val manager =
            object : PermissionManager {
                /**
                 * Mock getCameraPermissionStatus.
                 * @return Always returns GRANTED for this test.
                 */
                override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.GRANTED

                /**
                 * Mock requestCameraPermission.
                 * @return Always returns success for this test.
                 */
                override suspend fun requestCameraPermission(): Result<Unit> = Result.success(Unit)

                /** Mock openSettings. */
                override fun openSettings() {}
            }
        assertNotNull(manager)
        assertEquals(PermissionStatus.GRANTED, manager.getCameraPermissionStatus())

        val queryRes = manager.queryCameraPermissionStatus()
        assertTrue(queryRes.isSuccess)
        assertEquals(PermissionStatus.GRANTED, queryRes.getOrThrow())
    }

    /**
     * Verifies PermissionStatus enum values and entries.
     */
    @Test
    fun testPermissionStatusEnum() {
        assertEquals(PermissionStatus.GRANTED, PermissionStatus.valueOf("GRANTED"))
        assertEquals(PermissionStatus.DENIED, PermissionStatus.valueOf("DENIED"))
        assertEquals(PermissionStatus.NOT_DETERMINED, PermissionStatus.valueOf("NOT_DETERMINED"))
        assertEquals(3, PermissionStatus.entries.size)
    }

    /**
     * Verifies PermissionDeniedException constructor and default arguments.
     */
    @Test
    fun testPermissionDeniedException() {
        val defaultEx = PermissionDeniedException()
        assertFalse(defaultEx.isPermanentlyDenied)
        assertEquals("Camera permission was denied", defaultEx.message)

        val customEx = PermissionDeniedException(isPermanentlyDenied = true, message = "Denied forever")
        assertTrue(customEx.isPermanentlyDenied)
        assertEquals("Denied forever", customEx.message)
    }
}
