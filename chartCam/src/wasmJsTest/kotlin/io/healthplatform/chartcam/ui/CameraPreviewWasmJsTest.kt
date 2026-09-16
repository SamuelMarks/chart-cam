/**
 * @file CameraPreviewWasmJsTest.kt
 * Contains declarations for CameraPreviewWasmJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.CameraManager
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraPreview on WasmJS.
 */
class CameraPreviewWasmJsTest {
    /**
     * Test camera minimal mp4 container helper on WasmJS.
     */
    @Test
    fun testCameraPreviewWasmJs() {
        val container = CameraManager.createMinimalMp4Container()
        assertNotNull(container)
    }
}
