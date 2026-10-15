/**
 * @file CameraPreviewJsTest.kt
 * Contains JS unit tests for [CameraPreview] and its DOM video element lifecycle.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.JsCameraManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [CameraPreview] on the JS target.
 */
class CameraPreviewJsTest {
    /**
     * Verifies that [JsCameraManager] creates a detached HTMLVideoElement ready for [CameraPreview] attachment
     * and cleans up its stream reference upon release.
     */
    @Test
    fun testCameraPreviewJs() {
        val manager = JsCameraManager()
        val video = manager.videoElement
        assertNotNull(video)
        assertEquals("VIDEO", video.tagName.uppercase())
        manager.release()
        assertNull(video.srcObject)
    }
}
