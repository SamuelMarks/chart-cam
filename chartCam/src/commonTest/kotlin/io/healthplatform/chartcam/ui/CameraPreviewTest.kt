/**
 * @file CameraPreviewTest.kt
 * Contains common unit tests for the cross-platform [CameraPreview] contract and [CameraManager] interaction.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.captureImageCatching
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Common tests for [CameraPreview] and [CameraManager] preview lifecycle and capture state transitions.
 */
class CameraPreviewTest {
    /**
     * Verifies that a [CameraManager] bound to a preview session handles flash, lens switching,
     * image capture, and release lifecycle transitions deterministically.
     */
    @Test
    fun testCameraPreview() =
        runTest {
            var released = false
            var flashEnabled = false
            var lensToggled = false
            val expectedFrame = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

            val previewCameraManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? =
                        if (released) null else expectedFrame

                    override fun setFlash(on: Boolean): Result<Unit> {
                        flashEnabled = on
                        return Result.success(Unit)
                    }

                    override fun toggleLens(): Result<Unit> {
                        lensToggled = !lensToggled
                        return Result.success(Unit)
                    }

                    override fun release() {
                        released = true
                    }
                }

            assertFalse(previewCameraManager.isRecordingVideo)
            assertTrue(previewCameraManager.hasMultipleCameras)
            assertTrue(previewCameraManager.setFlash(true).isSuccess)
            assertTrue(flashEnabled)
            assertTrue(previewCameraManager.toggleLens().isSuccess)
            assertTrue(lensToggled)

            val capturedResult = previewCameraManager.captureImageCatching()
            assertTrue(capturedResult.isSuccess)
            assertContentEquals(expectedFrame, capturedResult.getOrThrow())

            assertTrue(previewCameraManager.releaseCatching().isSuccess)
            assertTrue(released)
            assertTrue(previewCameraManager.captureImageCatching().isFailure)
        }
}
