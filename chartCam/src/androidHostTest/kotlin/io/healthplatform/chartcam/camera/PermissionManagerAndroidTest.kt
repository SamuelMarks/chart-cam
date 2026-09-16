/**
 * @file PermissionManagerAndroidTest.kt
 * Contains declarations for PermissionManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Android host tests for PermissionManager.
 */
class PermissionManagerAndroidTest {
    /**
     * Verifies PermissionDeniedException creation on Android.
     */
    @Test
    fun testPermissionManagerAndroid() {
        val ex = PermissionDeniedException()
        assertNotNull(ex)
    }
}
