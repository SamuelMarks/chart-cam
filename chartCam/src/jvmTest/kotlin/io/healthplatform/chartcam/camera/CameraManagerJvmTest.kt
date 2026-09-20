/**
 * @file CameraManagerJvmTest.kt
 * Contains declarations for CameraManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for CameraManager on JVM.
 */
class CameraManagerJvmTest {
    /**
     * Verifies JvmCameraManager construction, video lifecycle, and resource release.
     */
    @Test
    fun testCameraManagerJvm() =
        runTest {
            val manager = JvmCameraManager()
            assertNotNull(manager)
            assertFalse(manager.isRecordingVideo)

            // Video lifecycle
            val startRes = manager.startVideoRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(manager.isRecordingVideo)

            val stopRes = manager.stopVideoRecording()
            assertTrue(stopRes.isSuccess)
            assertFalse(manager.isRecordingVideo)
            assertTrue(stopRes.getOrThrow().isNotEmpty())

            // Stopping when not recording
            val stopFailRes = manager.stopVideoRecording()
            assertTrue(stopFailRes.isFailure)

            // Cancel recording
            manager.startVideoRecording()
            assertTrue(manager.isRecordingVideo)
            val cancelRes = manager.cancelVideoRecording()
            assertTrue(cancelRes.isSuccess)
            assertFalse(manager.isRecordingVideo)

            // Image and preview capture
            manager.captureImage()
            manager.getPreviewImage()

            // Camera check
            assertNotNull(manager.hasMultipleCameras)

            manager.release()
        }

    /**
     * Verifies rememberCameraManager composable instantiation.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRememberCameraManager() {
        runComposeUiTest {
            setContent {
                val manager = rememberCameraManager()
                assertNotNull(manager)
            }
        }
    }
}
