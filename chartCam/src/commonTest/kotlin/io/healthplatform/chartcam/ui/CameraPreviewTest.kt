/**
 * @file CameraPreviewTest.kt
 * Contains declarations for CameraPreviewTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.CameraManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Common test wrapper for [CameraPreview].
 */
class CameraPreviewTest {
    /**
     * Verifies that the standard minimal MP4 container is generated correctly.
     */
    @Test
    fun testCameraPreview() {
        val container = CameraManager.createMinimalMp4Container()
        assertTrue(container.isNotEmpty())
        assertEquals('f'.code.toByte(), container[4])
        assertEquals('t'.code.toByte(), container[5])
        assertEquals('y'.code.toByte(), container[6])
        assertEquals('p'.code.toByte(), container[7])
    }
}
