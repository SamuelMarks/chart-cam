/**
 * @file CameraManagerJvmTest.kt
 * Contains declarations for CameraManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
     * Verifies setFlash behavior on Desktop JVM.
     */
    @Test
    fun testSetFlash() {
        val manager = JvmCameraManager()
        val flashOnRes = manager.setFlash(true)
        assertTrue(flashOnRes.isFailure)
        assertTrue(flashOnRes.exceptionOrNull() is UnsupportedOperationException)

        val flashOffRes = manager.setFlash(false)
        assertTrue(flashOffRes.isSuccess)
    }

    /**
     * Verifies webcam capture, preview, and lifecycle with simulated Webcam.
     */
    @Test
    fun testJvmCameraManagerWithSimulatedWebcam() =
        runTest {
            val mockCam = Mockito.mock(Webcam::class.java)
            var isOpen = false
            Mockito.`when`(mockCam.isOpen).thenAnswer { isOpen }
            Mockito.`when`(mockCam.open()).thenAnswer {
                isOpen = true
                true
            }
            Mockito.`when`(mockCam.close()).thenAnswer {
                isOpen = false
                true
            }

            val testImg = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
            Mockito.`when`(mockCam.image).thenReturn(testImg)

            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { mockCam },
                    webcamsProvider = { emptyList() },
                )

            val preview = manager.getPreviewImage()
            assertNotNull(preview)
            assertTrue(isOpen)

            // Subsequent preview when already open
            val preview2 = manager.getPreviewImage()
            assertNotNull(preview2)

            val bytes = manager.captureImage()
            assertNotNull(bytes)
            assertTrue(bytes.isNotEmpty())

            // Release when open
            manager.release()
            assertFalse(isOpen)

            // Release when already closed
            manager.release()
        }

    /**
     * Verifies multiple cameras detection and lens toggling.
     */
    @Test
    fun testJvmCameraManagerMultipleCamerasAndToggle() =
        runTest {
            val cam1 = Mockito.mock(Webcam::class.java)
            val cam2 = Mockito.mock(Webcam::class.java)
            var cam2Open = false
            Mockito.`when`(cam2.isOpen).thenAnswer { cam2Open }
            Mockito.`when`(cam2.open()).thenAnswer {
                cam2Open = true
                true
            }

            // Multi camera setup
            val multiManager =
                JvmCameraManager(
                    defaultWebcamProvider = { cam1 },
                    webcamsProvider = { listOf(cam1, cam2) },
                )
            assertTrue(multiManager.hasMultipleCameras)

            multiManager.getPreviewImage()
            val toggleRes = multiManager.toggleLens()
            assertTrue(toggleRes.isSuccess)
            Mockito.verify(cam1).close()
            assertTrue(cam2Open)

            // Single camera setup
            val singleManager =
                JvmCameraManager(
                    defaultWebcamProvider = { null },
                    webcamsProvider = { listOf(cam1) },
                )
            assertFalse(singleManager.hasMultipleCameras)
            assertTrue(singleManager.toggleLens().isSuccess)

            // Exception in webcamsProvider
            val throwingManager =
                JvmCameraManager(
                    defaultWebcamProvider = { null },
                    webcamsProvider = { throw IllegalStateException("Driver failed") }, // allow-exception
                )
            assertFalse(throwingManager.hasMultipleCameras)
        }

    /**
     * Verifies exception handling during image acquisition and release.
     */
    @Test
    fun testJvmCameraManagerErrorHandling() =
        runTest {
            val errorCam = Mockito.mock(Webcam::class.java)
            Mockito.`when`(errorCam.isOpen).thenReturn(true)
            Mockito.`when`(errorCam.image).thenThrow(RuntimeException("Frame capture failed"))
            Mockito.`when`(errorCam.close()).thenThrow(RuntimeException("Close failed"))

            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { errorCam },
                    webcamsProvider = { emptyList() },
                )
            assertNull(manager.getPreviewImage())
            assertNull(manager.captureImage())

            // Release throwing exception is caught in onFailure
            manager.release()
        }

    /**
     * Verifies native driver initialization branches.
     */
    @Test
    fun testInitializeNativeDriverBranches() {
        var actionInvoked = false
        // isTest = true skips
        JvmCameraManager.initializeNativeDriver(isTest = true, isInitialized = false) { actionInvoked = true }
        assertFalse(actionInvoked)

        // isInitialized = true skips
        JvmCameraManager.initializeNativeDriver(isTest = false, isInitialized = true) { actionInvoked = true }
        assertFalse(actionInvoked)

        // Not test and not initialized invokes action
        JvmCameraManager.initializeNativeDriver(isTest = false, isInitialized = false) { actionInvoked = true }
        assertTrue(actionInvoked)

        // Action throwing is safely caught in onFailure
        JvmCameraManager.initializeNativeDriver(isTest = false, isInitialized = false) {
            throw RuntimeException("Driver init failed") // allow-exception
        }

        // Test default arguments of initializeNativeDriver
        JvmCameraManager.initializeNativeDriver()
    }

    /**
     * Verifies getPreviewImage and captureImage when webcam is null.
     */
    @Test
    fun testGetPreviewImageNullCam() =
        runTest {
            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { null },
                    webcamsProvider = { emptyList() },
                )
            assertNull(manager.getPreviewImage())
            assertNull(manager.captureImage())
            manager.release()
        }

    /**
     * Verifies lens toggle when next camera is already open.
     */
    @Test
    fun testToggleLensWhenNextCameraAlreadyOpen() =
        runTest {
            val cam1 = Mockito.mock(Webcam::class.java)
            val cam2 = Mockito.mock(Webcam::class.java)
            Mockito.`when`(cam2.isOpen).thenReturn(true)

            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { cam1 },
                    webcamsProvider = { listOf(cam1, cam2) },
                )
            manager.getPreviewImage()
            val res = manager.toggleLens()
            assertTrue(res.isSuccess)
            Mockito.verify(cam2, Mockito.never()).open()
        }

    /**
     * Verifies capture failure when image writer throws.
     */
    @Test
    fun testCaptureImageWriterFailure() =
        runTest {
            val mockCam = Mockito.mock(Webcam::class.java)
            Mockito.`when`(mockCam.isOpen).thenReturn(true)
            val testImg = BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB)
            Mockito.`when`(mockCam.image).thenReturn(testImg)

            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { mockCam },
                    webcamsProvider = { emptyList() },
                    imageWriter = { _, _, _ -> throw java.io.IOException("Encoding failure") }, // allow-exception
                )
            assertNull(manager.captureImage())
        }

    /**
     * Verifies default providers invocation.
     */
    @Test
    fun testDefaultProvidersInvocation() =
        runTest {
            val manager = JvmCameraManager()
            assertNotNull(manager.hasMultipleCameras)
            assertTrue(manager.toggleLens().isSuccess)
            manager.release()

            // Directly exercise default constructor lambda implementations
            manager.defaultWebcamProvider.invoke()
            manager.webcamsProvider.invoke()
            val baos = java.io.ByteArrayOutputStream()
            val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
            manager.imageWriter.invoke(img, "PNG", baos)
            assertTrue(baos.size() > 0)

            // Test createDefaultWebcam branches
            assertNull(JvmCameraManager.createDefaultWebcam(isTest = true))
            runCatching { JvmCameraManager.createDefaultWebcam(isTest = false) }
            assertTrue(JvmCameraManager.createDefaultWebcams(isTest = true).isEmpty())
            runCatching { JvmCameraManager.createDefaultWebcams(isTest = false) }
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
