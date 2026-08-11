/**
 * @file CaptureScreenJvmTest.kt
 * Contains declarations for CaptureScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.mockito.Mockito
import kotlin.test.Test

/**
 * Test class for CaptureScreen on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class CaptureScreenJvmTest {
    /**
     * Tests capturing screen on JVM.
     */
    @Test
    fun testCaptureScreenJvm() =
        runComposeUiTest {
            val mockRepo = Mockito.mock(QuestionnaireRepository::class.java)

            setContent {
                CaptureScreen(
                    questionnaireId = "test-123",
                    questionnaireRepository = mockRepo,
                    onFinished = {},
                    onCancel = {},
                )
            }

            onRoot().assertExists()
        }

    /**
     * Tests controls layer rendering.
     */
    @Test
    fun testControlsLayer() =
        runComposeUiTest {
            setContent {
                ControlsLayer(
                    state =
                        ControlsState(
                            stepName = "Test Step",
                            count = 1,
                            total = 3,
                            isCapturing = false,
                            hasMultipleCameras = true,
                        ),
                    onCapture = {},
                    onToggleLens = {},
                    onCancel = {},
                )
            }
            onRoot().assertExists()
        }

    /**
     * Tests review layer rendering.
     */
    @Test
    fun testReviewLayer() =
        runComposeUiTest {
            // Create a minimal 1x1 BMP byte array to avoid decode error in ReviewLayer (it uses decodeToImageBitmap)
            // A minimal valid BMP file (26 bytes header + 4 bytes pixel data)
            val minimalBmp =
                byteArrayOf(
                    0x42,
                    0x4D,
                    0x1E,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x1A,
                    0x00,
                    0x00,
                    0x00,
                    0x0C,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x18,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                )

            setContent {
                ReviewLayer(
                    bytes = minimalBmp,
                    onRetake = {},
                    onConfirm = {},
                )
            }
            onRoot().assertExists()
        }

    // CaptureScreen is complex and depends on many ViewModels, Repository, and remember functions.
    // It is sufficient to test its sub-components to increase coverage.
}
