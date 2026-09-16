/**
 * @file CameraPreviewJvmTest.kt
 * Contains declarations for CameraPreviewJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.JvmCameraManager
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraPreview on JVM.
 */
class CameraPreviewJvmTest {
    /**
     * Verifies JvmCameraManager initialization for camera preview.
     */
    @Test
    fun testCameraPreviewJvm() {
        val manager = JvmCameraManager()
        assertNotNull(manager)
        manager.release()
    }
}
