/**
 * @file CameraPreviewJsTest.kt
 * Contains declarations for CameraPreviewJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.CameraManager
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraPreview on JS.
 */
class CameraPreviewJsTest {
    /**
     * Test camera minimal mp4 container helper on JS.
     */
    @Test
    fun testCameraPreviewJs() {
        val container = CameraManager.createMinimalMp4Container()
        assertNotNull(container)
    }
}
