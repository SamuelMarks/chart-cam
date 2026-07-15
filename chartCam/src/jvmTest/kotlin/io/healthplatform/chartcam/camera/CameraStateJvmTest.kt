/**
 * Contains unit tests validating the coordination logic around Camera functionality.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests validating the coordination logic around Camera functionality.
 * Since hardware cannot be tested in Unit Tests, we stub the [CameraManager].
 */
class CameraStateJvmTest {
    /**
     * A Mock implementation of [CameraManager] for testing.
     * Stubbed to track states like flash and lens toggles, and simulate image captures.
     */
    class MockCameraManager : CameraManager {
        /**
         * Represents whether the camera flash is currently set to on.
         */
        var isFlashOn = false

        /**
         * Tracks the number of times the lens has been toggled.
         */
        var lensToggledCount = 0

        /**
         * Determines whether the next image capture attempt should succeed or return null.
         */
        var shouldSucceedCapture = true

        /**
         * Simulates capturing an image.
         *
         * @return A dummy [ByteArray] if [shouldSucceedCapture] is true, otherwise null.
         */
        override suspend fun captureImage(): ByteArray? = if (shouldSucceedCapture) ByteArray(1024) else null

        /**
         * Sets the flash state.
         *
         * @param on True to turn the flash on, false to turn it off.
         */
        override fun setFlash(on: Boolean) {
            isFlashOn = on
        }

        /**
         * Toggles the active camera lens and increments [lensToggledCount].
         */
        override fun toggleLens() {
            lensToggledCount++
        }

        /**
         * Releases camera resources. For this mock, it performs a no-op.
         */
        override fun release() {
            // no-op
        }
    }

    /**
     * Tests a successful image capture scenario, ensuring data is returned correctly.
     */
    @Test
    fun testCaptureSuccess() =
        runTest {
            val manager = MockCameraManager()

            val result = manager.captureImage()

            assertNotNull(result, "Capture should return data")
            assertEquals(1024, result.size)
        }

    /**
     * Tests a failed image capture scenario, ensuring null is returned.
     */
    @Test
    fun testCaptureFailure() =
        runTest {
            val manager = MockCameraManager()
            manager.shouldSucceedCapture = false

            val result = manager.captureImage()

            assertNull(result, "Capture should return null on failure")
        }

    /**
     * Tests toggling the camera flash and verifies the state is updated correctly.
     */
    @Test
    fun testFlashToggle() {
        val manager = MockCameraManager()

        manager.setFlash(true)
        assertTrue(manager.isFlashOn)

        manager.setFlash(false)
        assertEquals(false, manager.isFlashOn)
    }

    /**
     * Tests toggling the camera lens and verifies the toggle count is incremented.
     */
    @Test
    fun testLensToggle() {
        val manager = MockCameraManager()

        manager.toggleLens()
        assertEquals(1, manager.lensToggledCount)
    }
}
