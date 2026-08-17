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
                override fun setFlash(on: Boolean) {}

                /** Mock toggleLens. */
                override fun toggleLens() {}

                /** Mock release. */
                override fun release() {}

                override val hasMultipleCameras: Boolean = false
            }
        assertNotNull(manager)
    }
}
