/**
 * @file CameraManagerTest.kt
 * Unit tests for CameraManager interface defaults and extension functions.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the [CameraManager] interface.
 */
class CameraManagerTest {
    /**
     * Tests that the [CameraManager] interface default methods and properties behave as expected.
     */
    @Test
    fun testCameraManagerInterfaceDefaults() =
        runTest {
            val defaultManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun release() {}
                }

            assertNotNull(defaultManager)
            assertTrue(defaultManager.hasMultipleCameras)
            assertFalse(defaultManager.isRecordingVideo)

            assertTrue(defaultManager.setFlash(true).isSuccess)
            assertTrue(defaultManager.setFlash(false).isSuccess)
            assertTrue(defaultManager.toggleLens().isSuccess)

            assertTrue(defaultManager.startVideoRecording().isSuccess)
            val stopRes = defaultManager.stopVideoRecording()
            assertTrue(stopRes.isSuccess)
            assertTrue(stopRes.getOrThrow().isNotEmpty())

            assertTrue(defaultManager.cancelVideoRecording().isSuccess)

            val mp4Bytes = CameraManager.createMinimalMp4Container()
            assertTrue(mp4Bytes.isNotEmpty())
        }

    /**
     * Tests captureImageCatching extension returning Result across null, empty, and populated byte arrays.
     */
    @Test
    fun testCaptureImageCatching() =
        runTest {
            val nullManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun release() {}
                }
            assertTrue(nullManager.captureImageCatching().isFailure)

            val emptyBytesManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray = byteArrayOf()

                    override fun release() {}
                }
            assertTrue(emptyBytesManager.captureImageCatching().isFailure)

            val successManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray = byteArrayOf(1, 2, 3)

                    override fun release() {}
                }
            val result = successManager.captureImageCatching()
            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrThrow().size)
        }
}
