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
                override suspend fun captureImage(): ByteArray? = null

                override fun setFlash(on: Boolean) {}

                override fun toggleLens() {}

                override fun release() {}

                override val hasMultipleCameras: Boolean = false
            }
        assertNotNull(manager)
    }
}
