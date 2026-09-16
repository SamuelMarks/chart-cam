/**
 * @file CameraManagerTest.kt
 * Contains declarations for CameraManagerTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for the [CameraManager] interface.
 */
class CameraManagerTest {
    /**
     * Tests that the [CameraManager] interface can be mocked.
     */
    @Test
    fun testCameraManagerInterface() {
        val manager =
            object : CameraManager {
                /**
                 * Mock captureImage.
                 * @return Always returns null for this test.
                 */
                override suspend fun captureImage(): ByteArray? = null

                /**
                 * Mock setFlash.
                 * @param on Boolean state.
                 */
                override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                /** Mock toggleLens. */
                override fun toggleLens(): Result<Unit> = Result.success(Unit)

                /** Mock release. */
                override fun release() {}

                override val hasMultipleCameras: Boolean = false
            }
        assertNotNull(manager)
    }

    /**
     * Tests captureImageCatching extension returning Result.
     */
    @Test
    fun testCaptureImageCatching() =
        kotlinx.coroutines.test.runTest {
            val failingManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override fun release() {}
                }
            kotlin.test.assertTrue(failingManager.captureImageCatching().isFailure)

            val successManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = byteArrayOf(1, 2, 3)

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override fun release() {}
                }
            val result = successManager.captureImageCatching()
            kotlin.test.assertTrue(result.isSuccess)
            kotlin.test.assertEquals(3, result.getOrNull()?.size)
        }
}
