/**
 * @file CameraManagerAndroidTest.kt
 * Contains declarations for CameraManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android host tests for CameraManager.
 */
class CameraManagerAndroidTest {
    /**
     * Verifies MP4 payload creation on Android.
     */
    @Test
    fun testCameraManagerAndroid() {
        val container = CameraManager.createMinimalMp4Container()
        assertNotNull(container)
        assertTrue(container.isNotEmpty())
    }
}
