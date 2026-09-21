/**
 * @file FacialSeriesCaptureViewModelTest.kt
 * Contains tests for CaptureViewModel with 3-angle facial series and silhouette progression.
 */
package io.healthplatform.chartcam.capture

import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests verifying the sequential progression, silhouette updating, and ghost image passing for facial series.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FacialSeriesCaptureViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    /** Mock camera manager. */
    private class FakeCamera : CameraManager {
        var captureBytes: ByteArray = byteArrayOf(1, 2, 3, 4)

        override suspend fun captureImage(): ByteArray? = captureBytes

        override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

        override fun toggleLens(): Result<Unit> = Result.success(Unit)

        override fun release() {}
    }

    /** Mock file storage. */
    private class FakeStorage : FileStorage {
        val savedFiles = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            savedFiles[fileName] = bytes
            return "/mock/path/$fileName"
        }

        override fun readImage(path: String): ByteArray = savedFiles.values.firstOrNull() ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

        override fun clearCache() {
            savedFiles.clear()
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFacialSeriesProgressionAndGhostHydration() =
        runTest(testDispatcher) {
            val camera = FakeCamera()
            val storage = FakeStorage()
            val viewModel = CaptureViewModel(camera, storage)

            val steps =
                listOf(
                    PhotoStep("step_left", "Left Profile", silhouette = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT),
                    PhotoStep("step_front", "Front View", silhouette = SilhouetteType.FRONTAL_FACE),
                    PhotoStep("step_right", "Right Profile", silhouette = SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT),
                )

            viewModel.initSteps(steps)
            advanceUntilIdle()

            // Step 1: Left Profile
            val state1 = viewModel.uiState.value
            assertEquals("step_left", state1.currentStep?.id)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_LEFT, state1.silhouetteType)
            assertNull(state1.ghostImageBytes)

            // Capture Step 1
            viewModel.onCapture()
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.reviewImageBytes)

            // Confirm Step 1
            viewModel.onConfirm()
            advanceUntilIdle()

            // Step 2: Front View
            val state2 = viewModel.uiState.value
            assertEquals("step_front", state2.currentStep?.id)
            assertEquals(SilhouetteType.FRONTAL_FACE, state2.silhouetteType)
            assertNull(state2.ghostImageBytes)

            // Capture and Confirm Step 2
            viewModel.onCapture()
            advanceUntilIdle()
            viewModel.onConfirm()
            advanceUntilIdle()

            // Step 3: Right Profile (should have ghost image from step 1)
            val state3 = viewModel.uiState.value
            assertEquals("step_right", state3.currentStep?.id)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT, state3.silhouetteType)
            assertNotNull(state3.ghostImageBytes)
            assertTrue(state3.ghostImageBytes.contentEquals(camera.captureBytes))

            // Capture and Confirm Step 3
            viewModel.onCapture()
            advanceUntilIdle()
            viewModel.onConfirm()
            advanceUntilIdle()

            // All finished
            val finalState = viewModel.uiState.value
            assertTrue(finalState.isFinished)
            assertEquals(3, finalState.capturedCount)
            assertEquals(SilhouetteType.NONE, finalState.silhouetteType)
        }
}
