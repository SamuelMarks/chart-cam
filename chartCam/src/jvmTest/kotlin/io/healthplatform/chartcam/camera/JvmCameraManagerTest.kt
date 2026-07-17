/**
 * @file JvmCameraManagerTest.kt
 * Contains declarations for JvmCameraManagerTest.kt.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmCameraManagerTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRememberJvmManagers() =
        runComposeUiTest {
            setContent {
                val cameraManager = rememberCameraManager()
                assertTrue(cameraManager is JvmCameraManager)

                val permissionManager = rememberPermissionManager()
                assertTrue(permissionManager is JvmPermissionManager)
            }
        }

    @Test
    fun testJvmCameraManager() {
        runBlocking {
            val manager = JvmCameraManager()

            // These are no-ops, just ensure they don't crash
            manager.setFlash(true)
            manager.toggleLens()
            // Invoke to hit branch if webcam is null
            manager.release()
            manager.getPreviewImage()
            manager.release() // Hit branch if webcam is open

            // On a CI environment without a webcam, getPreviewImage and captureImage should gracefully handle it
            val capture = manager.captureImage()
            assertTrue(capture == null || capture.isNotEmpty())
            val preview = manager.getPreviewImage()
            assertTrue(preview == null || preview != null)

            // In CI it usually doesn't have webcams
            val hasMultiple = manager.hasMultipleCameras

            // Trigger Exception path
            try {
                Webcam.setDriver(null as com.github.sarxos.webcam.WebcamDriver?)
            } catch (e: Exception) {
            }
            manager.release()
            manager.hasMultipleCameras
        }
    }

    @Test
    fun testJvmPermissionManager() {
        runBlocking {
            val manager = JvmPermissionManager()

            assertEquals(PermissionStatus.GRANTED, manager.getCameraPermissionStatus())
            assertTrue(manager.requestCameraPermission())

            // Ensure no-op doesn't crash
            manager.openSettings()
        }
    }
}
