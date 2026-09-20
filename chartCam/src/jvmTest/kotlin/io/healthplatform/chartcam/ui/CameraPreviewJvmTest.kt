/**
 * @file CameraPreviewJvmTest.kt
 * Contains declarations for CameraPreviewJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.camera.JvmCameraManager
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraPreview on JVM.
 */
class CameraPreviewJvmTest {
    /**
     * Verifies CameraPreview composable rendering and JvmCameraManager release on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewJvm() =
        runComposeUiTest {
            val manager = JvmCameraManager()
            assertNotNull(manager)

            setContent {
                CameraPreview(
                    modifier = Modifier,
                    cameraManager = manager,
                )
            }
            waitForIdle()

            onRoot().assertExists()
            manager.release()
        }
}
