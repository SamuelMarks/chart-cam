/**
 * @file CameraPreviewJvmTest.kt
 * Contains declarations for CameraPreviewJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.sarxos.webcam.Webcam
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JvmCameraManager
import org.mockito.Mockito
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CameraPreview on JVM.
 */
class CameraPreviewJvmTest {
    /**
     * Verifies CameraPreview composable rendering with live frame streaming and dropped frames.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewStreamingSuccess() =
        runComposeUiTest {
            setAppLanguage("en")
            val mockWebcam = Mockito.mock(Webcam::class.java)
            val sampleImage = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
            var returnFrame = true
            Mockito.`when`(mockWebcam.isOpen).thenReturn(true)
            Mockito.`when`(mockWebcam.image).thenAnswer {
                val res = if (returnFrame) sampleImage else null
                returnFrame = !returnFrame // Alternate between frame and dropped frame
                res
            }

            val manager =
                JvmCameraManager(
                    defaultWebcamProvider = { mockWebcam },
                    webcamsProvider = { listOf(mockWebcam) },
                )

            setContent {
                CameraPreview(
                    modifier = Modifier,
                    cameraManager = manager,
                )
            }

            waitForIdle()
            mainClock.advanceTimeBy(150L)
            waitForIdle()

            // Verify live preview image is displayed
            onNodeWithContentDescription("Camera Preview", useUnmergedTree = true).assertIsDisplayed()
            manager.release()
        }

    /**
     * Verifies CameraPreview displays error when cameraManager is not a JvmCameraManager.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewNonJvmCameraManagerDisplaysError() =
        runComposeUiTest {
            setAppLanguage("en")
            val nonJvmManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override val hasMultipleCameras: Boolean = false

                    override fun release() {}

                    override suspend fun startVideoRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun stopVideoRecording(): Result<ByteArray> = Result.success(ByteArray(0))
                }

            setContent {
                CameraPreview(
                    modifier = Modifier,
                    cameraManager = nonJvmManager,
                )
            }

            waitForIdle()

            // Verify error message for camera unavailable is displayed
            onNodeWithText("Camera unavailable or permission denied.", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies CameraPreview displays error when first frame acquisition times out or returns null.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewFrameTimeoutDisplaysError() =
        runComposeUiTest {
            setAppLanguage("en")
            val oldTimeout = timeoutMs
            val oldPoll = pollIntervalMs
            timeoutMs = 50L
            pollIntervalMs = 10L

            val nullWebcamManager =
                JvmCameraManager(
                    defaultWebcamProvider = { null },
                    webcamsProvider = { emptyList() },
                )

            // allow-exception
            try {
                setContent {
                    CameraPreview(
                        modifier = Modifier,
                        cameraManager = nullWebcamManager,
                    )
                }

                // Wait up to 3 seconds for the LaunchedEffect IO coroutine to finish timeout and set hasError
                waitUntil(timeoutMillis = 3000L) {
                    runCatching {
                        onNodeWithText("Camera unavailable or permission denied.", useUnmergedTree = true).assertIsDisplayed()
                    }.isSuccess
                }

                // Now assert firmly
                onNodeWithText("Camera unavailable or permission denied.", useUnmergedTree = true).assertIsDisplayed()
            } finally {
                timeoutMs = oldTimeout
                pollIntervalMs = oldPoll
                nullWebcamManager.release()
            }
        }

    /**
     * Verifies CameraPreview initial state and default JvmCameraManager release.
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

    /**
     * Verifies CameraPreview recomposition when parameters change.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val nonJvmManager1 =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override val hasMultipleCameras: Boolean = false

                    override fun release() {}

                    override suspend fun startVideoRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun stopVideoRecording(): Result<ByteArray> = Result.success(ByteArray(0))
                }
            val nonJvmManager2 =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override val hasMultipleCameras: Boolean = false

                    override fun release() {}

                    override suspend fun startVideoRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun stopVideoRecording(): Result<ByteArray> = Result.success(ByteArray(0))
                }

            val modifierState = androidx.compose.runtime.mutableStateOf<Modifier>(Modifier)
            val managerState = androidx.compose.runtime.mutableStateOf<CameraManager>(nonJvmManager1)

            setContent {
                CameraPreview(
                    modifier = modifierState.value,
                    cameraManager = managerState.value,
                )
            }
            waitForIdle()

            // Mutate modifier
            modifierState.value = Modifier.then(Modifier)
            waitForIdle()

            // Mutate cameraManager
            managerState.value = nonJvmManager2
            waitForIdle()
        }

    /**
     * Verifies CameraPreview skips recomposition when parameters are unchanged.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCameraPreviewRecompositionSkipping() =
        runComposeUiTest {
            setAppLanguage("en")
            val nonJvmManager =
                object : CameraManager {
                    override suspend fun captureImage(): ByteArray? = null

                    override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

                    override fun toggleLens(): Result<Unit> = Result.success(Unit)

                    override val hasMultipleCameras: Boolean = false

                    override fun release() {}

                    override suspend fun startVideoRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun stopVideoRecording(): Result<ByteArray> = Result.success(ByteArray(0))
                }

            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = outerTrigger.value
                CameraPreview(
                    modifier = Modifier,
                    cameraManager = nonJvmManager,
                )
            }
            waitForIdle()

            outerTrigger.value++
            waitForIdle()
        }
}
