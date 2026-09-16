/**
 * @file CameraManagerJvmTest.kt
 * Contains declarations for CameraManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraManager on JVM.
 */
class CameraManagerJvmTest {
    /**
     * Verifies JvmCameraManager construction and resource release.
     */
    @Test
    fun testCameraManagerJvm() {
        val manager = JvmCameraManager()
        assertNotNull(manager)
        manager.release()
    }
}
