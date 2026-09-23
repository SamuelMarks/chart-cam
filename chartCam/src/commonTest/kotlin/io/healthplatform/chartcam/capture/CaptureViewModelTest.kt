/**
 * @file CaptureViewModelTest.kt
 * Contains declarations for CaptureViewModelTest.kt.
 */
package io.healthplatform.chartcam.capture

import io.healthplatform.chartcam.camera.CameraManager
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [CaptureViewModel] business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    /** Test dispatcher for coroutine tests. */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Mock implementation of [CameraManager] for testing.
     */
    class MockCameraManager : CameraManager {
        /** Simulate failure indicator. */
        var simulateFailure = false

        /** Exception to throw, if any. */
        var exceptionToThrow: Exception? = null

        /**
         * Capture image method.
         * @return Mock byte array or null.
         */
        override suspend fun captureImage(): ByteArray? {
            if (exceptionToThrow != null) throw exceptionToThrow!! // allow-exception
            return if (simulateFailure) null else ByteArray(10)
        }

        /**
         * Toggle flash.
         * @param on Flash state.
         * @return A [Result] indicating success.
         */
        override fun setFlash(on: Boolean): Result<Unit> = Result.success(Unit)

        /**
         * Toggle lens.
         * @return A [Result] indicating success.
         */
        override fun toggleLens(): Result<Unit> = Result.success(Unit)

        /** Release resources. */
        override fun release() {}
    }

    /**
     * Mock implementation of [FileStorage].
     */
    class MockFileStorage : FileStorage {
        /** Internal storage map. */
        val files = mutableMapOf<String, ByteArray>()

        /** Simulate failure indicator. */
        var simulateFailure = false

        /**
         * Save an image to map.
         * @param fileName The name.
         * @param bytes The bytes.
         * @return The path.
         */
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            if (simulateFailure) throw IllegalStateException("Storage full") // allow-exception
            files[fileName] = bytes
            return "path/to/$fileName"
        }

        /**
         * Read image from map.
         * @param path The path.
         * @return The bytes.
         */
        override fun readImage(path: String): ByteArray = files[path.substringAfterLast("/")] ?: ByteArray(0)

        /**
         * Delete image from map.
         * @param path The path.
         * @return Result indicating success or failure.
         */
        override fun deleteImage(path: String): Result<Unit> =
            if (files.remove(path.substringAfterLast("/")) != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("File not found: $path"))
            }

        /** Clear cache logic. */
        override fun clearCache() {
            files.clear()
        }
    }

    /** Setup test coroutine dispatcher. */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /** Tear down test coroutine dispatcher. */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Tests step initialization logic. */
    @Test
    fun testInitSteps() {
        val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())

        vm.initSteps(emptyList())
        assertTrue(vm.uiState.value.isFinished)

        val steps = listOf(PhotoStep("1", "A"), PhotoStep("2", "B"))
        vm.initSteps(steps)
        assertEquals(steps.first(), vm.uiState.value.currentStep)
        assertEquals(2, vm.uiState.value.totalSteps)
    }

    /** Tests capture success path. */
    @Test
    fun testOnCaptureSuccess() =
        runTest {
            val camera = MockCameraManager()
            val vm = CaptureViewModel(camera, MockFileStorage())

            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()

            // Assert capturing state before idle
            assertTrue(vm.uiState.value.isCapturing)

            advanceUntilIdle()

            assertFalse(vm.uiState.value.isCapturing)
            assertNotNull(vm.uiState.value.reviewImageBytes)
        }

    /** Tests capture failure due to null response. */
    @Test
    fun testOnCaptureFailure() =
        runTest {
            val camera = MockCameraManager().apply { simulateFailure = true }
            val vm = CaptureViewModel(camera, MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isCapturing)
            assertNull(vm.uiState.value.reviewImageBytes)
        }

    /** Tests capture logic when an exception is thrown. */
    @Test
    fun testOnCaptureException() =
        runTest {
            val camera = MockCameraManager().apply { exceptionToThrow = IllegalStateException("Camera crashed") }
            val vm = CaptureViewModel(camera, MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isCapturing)
            assertNull(vm.uiState.value.reviewImageBytes)
        }

    /** Tests confirming an image moves to the next step. */
    @Test
    fun testOnConfirmAdvancesStep() =
        runTest {
            val camera = MockCameraManager()
            val storage = MockFileStorage()
            val vm = CaptureViewModel(camera, storage)

            vm.initSteps(listOf(PhotoStep("1", "A"), PhotoStep("2", "B")))

            vm.onCapture()
            advanceUntilIdle()

            vm.onConfirm()

            assertEquals(PhotoStep("2", "B"), vm.uiState.value.currentStep)
            assertNull(vm.uiState.value.reviewImageBytes)
            assertEquals(1, vm.uiState.value.capturedCount)
            assertEquals(1, vm.getResultPaths().size)
        }

    /** Tests confirming image on last step finishes session. */
    @Test
    fun testOnConfirmFinishesSequence() =
        runTest {
            val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            vm.onConfirm()

            assertTrue(vm.uiState.value.isFinished)
            assertNull(vm.uiState.value.reviewImageBytes)
            assertEquals(1, vm.uiState.value.capturedCount)
        }

    /** Tests failure in storage during confirm action. */
    @Test
    fun testOnConfirmStorageFailure() =
        runTest {
            val storage = MockFileStorage().apply { simulateFailure = true }
            val vm = CaptureViewModel(MockCameraManager(), storage)
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            vm.onConfirm()

            assertNull(vm.uiState.value.reviewImageBytes)
            assertEquals(0, vm.uiState.value.capturedCount)
            assertFalse(vm.uiState.value.isFinished)
        }

    /** Tests retaking an image discards current selection. */
    @Test
    fun testOnRetake() =
        runTest {
            val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.reviewImageBytes)

            vm.onRetake()

            assertNull(vm.uiState.value.reviewImageBytes)
        }

    /** Tests triggering capture while already capturing has no effect. */
    @Test
    fun testOnCaptureWhileCapturing() =
        runTest {
            val camera = MockCameraManager()
            val vm = CaptureViewModel(camera, MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            assertTrue(vm.uiState.value.isCapturing)

            // This should hit the early return
            vm.onCapture()

            advanceUntilIdle()
        }

    /** Tests confirm logic aborts gracefully if no image is available. */
    @Test
    fun testOnConfirmWithoutBytes() {
        val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())
        vm.initSteps(listOf(PhotoStep("1", "A")))
        vm.onConfirm() // Should early return
        assertEquals(0, vm.uiState.value.capturedCount)
    }

    /** Tests video recording advancing step and finishing session. */
    @Test
    fun testOnVideoRecorded() {
        val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())
        // When currentStep is null
        vm.onVideoRecorded("path/video.mp4")
        assertEquals(0, vm.uiState.value.capturedCount)

        val step1 = PhotoStep("1", "Step 1")
        val step2 = PhotoStep("2", "Step 2")
        vm.initSteps(listOf(step1, step2))

        // First video recorded: advances to nextStep
        vm.onVideoRecorded("path/step1.mp4")
        assertEquals(1, vm.uiState.value.capturedCount)
        assertEquals(step2, vm.uiState.value.currentStep)
        assertFalse(vm.uiState.value.isFinished)

        // Second video recorded: final step finishes session
        vm.onVideoRecorded("path/step2.mp4")
        assertEquals(2, vm.uiState.value.capturedCount)
        assertTrue(vm.uiState.value.isFinished)
    }

    /** Tests camera capture exception with null message. */
    @Test
    fun testCaptureCameraExceptionWithNullMessage() =
        runTest {
            val camera = MockCameraManager()
            camera.exceptionToThrow = Exception(null as String?)
            val vm = CaptureViewModel(camera, MockFileStorage())
            vm.initSteps(listOf(PhotoStep("1", "A")))

            vm.onCapture()
            advanceUntilIdle()

            val err = vm.uiState.value.error
            assertTrue(err is CaptureError.CameraFailed)
            assertEquals("Unknown error", err.detail)
        }
}
