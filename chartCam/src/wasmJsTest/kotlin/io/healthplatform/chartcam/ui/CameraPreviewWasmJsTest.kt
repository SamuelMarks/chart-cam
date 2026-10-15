/**
 * @file CameraPreviewWasmJsTest.kt
 * Contains WasmJS unit tests for [CameraPreview] and its video element lifecycle.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.JsCameraManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [CameraPreview] on the WasmJS target.
 */
class CameraPreviewWasmJsTest {
    /**
     * Verifies that [JsCameraManager] creates an HTMLVideoElement ready for [CameraPreview] frame extraction
     * and clears its stream reference upon release.
     */
    @Test
    fun testCameraPreviewWasmJs() {
        val manager = JsCameraManager()
        val video = manager.videoElement
        assertNotNull(video)
        assertEquals("VIDEO", video.tagName.uppercase())
        manager.release()
        assertNull(video.srcObject)
    }
}
