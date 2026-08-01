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

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    class MockCameraManager : CameraManager {
        var simulateFailure = false
        var exceptionToThrow: Exception? = null

        override suspend fun captureImage(): ByteArray? {
            if (exceptionToThrow != null) throw exceptionToThrow!!
            return if (simulateFailure) null else ByteArray(10)
        }

        override fun setFlash(on: Boolean) {}

        override fun toggleLens() {}

        override fun release() {}
    }

    class MockFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()
        var simulateFailure = false

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            if (simulateFailure) throw IllegalStateException("Storage full")
            files[fileName] = bytes
            return "path/to/$fileName"
        }

        override fun readImage(path: String): ByteArray = files[path.substringAfterLast("/")] ?: ByteArray(0)

        override fun clearCache() {
            files.clear()
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    @Test
    fun testOnConfirmWithoutBytes() {
        val vm = CaptureViewModel(MockCameraManager(), MockFileStorage())
        vm.initSteps(listOf(PhotoStep("1", "A")))
        vm.onConfirm() // Should early return
        assertEquals(0, vm.uiState.value.capturedCount)
    }
}
