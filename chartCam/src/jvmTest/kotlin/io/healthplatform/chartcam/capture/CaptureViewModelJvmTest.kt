/**
 * Test definitions for the photo capture workflows.
 *
 * This test suite focuses on ensuring that [CaptureViewModel] orchestrates
 * camera operations and local file storage correctly across an entire multi-step workflow.
 */
package io.healthplatform.chartcam.capture

import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates state transitions and side effects of [CaptureViewModel].
 *
 * Tests the entire capture lifecycle, confirming that images are captured, verified,
 * saved to mock storage, and steps correctly advance or reset on retakes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelJvmTest {
    /**
     * Dispatcher used to execute all coroutines deterministically during testing.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Stub implementation of [CameraManager] tracking camera invocation counts.
     */
    private lateinit var mockCamera: MockCameraManager

    /**
     * Stub implementation of [FileStorage] capturing mock images into an in-memory map.
     */
    private lateinit var mockStorage: MockFileStorage

    /**
     * The [CaptureViewModel] instance being subjected to tests.
     */
    private lateinit var viewModel: CaptureViewModel

    /**
     * The sequence of steps the [CaptureViewModel] is configured to execute.
     */
    private val steps = PhotoStep.STANDARD_STEPS

    /**
     * Instantiates mock implementations and prepares the view model before each test run.
     *
     * Hooks up the standard test coroutine dispatcher and initializes the view model
     * with the default standard steps sequence.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockCamera = MockCameraManager()
        mockStorage = MockFileStorage()
        viewModel = CaptureViewModel(mockCamera, mockStorage)
        viewModel.initSteps(steps)
    }

    /**
     * Restores environmental conditions post-test execution.
     *
     * Resets the coroutines main dispatcher to avoid interference with subsequent tests.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that the view model initializes into a proper waiting state.
     *
     * Ensures that when the view model is constructed, it targets the first step,
     * is not actively capturing, and has no residual image bytes loaded.
     */
    @Test
    fun `initial state is correct`() =
        runTest {
            val state = viewModel.uiState.value
            assertEquals(steps.first(), state.currentStep)
            assertFalse(state.isCapturing)
            assertNull(state.reviewImageBytes)
        }

    /**
     * Verifies that initializing with an empty list sets the state to finished immediately.
     */
    @Test
    fun `initSteps with empty list sets isFinished to true`() =
        runTest {
            val emptyViewModel = CaptureViewModel(mockCamera, mockStorage)
            emptyViewModel.initSteps(emptyList())
            val state = emptyViewModel.uiState.value
            assertTrue(state.isFinished)
            assertNull(state.currentStep)
        }

    /**
     * Verifies that initializing steps when they are already initialized does not overwrite them.
     */
    @Test
    fun `initSteps with existing steps does not overwrite`() =
        runTest {
            viewModel.initSteps(listOf(PhotoStep("new", "New Step")))
            val state = viewModel.uiState.value
            assertEquals(steps.first(), state.currentStep)
            assertEquals(steps.size, state.totalSteps)
        }

    /**
     * Verifies that invoking capture while already capturing ignores the redundant call.
     */
    @Test
    fun `onCapture when already capturing ignores second call`() =
        runTest {
            // Suspend the mock camera briefly so it stays in the 'capturing' state.
            val originalCaptureCount = mockCamera.captureCount
            viewModel.onCapture()
            assertTrue(viewModel.uiState.value.isCapturing)

            // Call it again immediately
            viewModel.onCapture()

            testDispatcher.scheduler.advanceUntilIdle()

            // Ensure the camera was only asked to capture once.
            assertEquals(originalCaptureCount + 1, mockCamera.captureCount)
        }

    /**
     * Verifies that if the camera fails to capture (returns null), the capture state is reset.
     */
    @Test
    fun `onCapture when camera returns null resets capturing state`() =
        runTest {
            mockCamera.returnNullNextTime = true
            viewModel.onCapture()

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isCapturing)
            assertNull(state.reviewImageBytes)
        }

    /**
     * Checks that confirming without a current step does nothing.
     */
    @Test
    fun `onConfirm without currentStep does nothing`() =
        runTest {
            val emptyViewModel = CaptureViewModel(mockCamera, mockStorage)
            // No initSteps called, so currentStep is null

            // Manually set reviewImageBytes using reflection or just know it won't crash
            // Actually, we can't manually set it via public API.
            // We'll call onCapture, it will mock capture and set review bytes.
            emptyViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(emptyViewModel.uiState.value.reviewImageBytes)

            emptyViewModel.onConfirm() // currentStep is null

            assertEquals(0, mockStorage.savedFiles.size)
            // reviewImageBytes is not cleared
            assertNotNull(emptyViewModel.uiState.value.reviewImageBytes)
        }

    /**
     * Tests the capture operation to ensure it triggers the camera and caches results.
     *
     * Asserts that initiating a capture toggles the capture state flag and eventually
     * sets the review image buffer once the camera successfully mock-returns an image.
     */
    @Test
    fun `onCapture triggers camera and updates review image`() =
        runTest {
            viewModel.onCapture()
            assertTrue(viewModel.uiState.value.isCapturing)

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isCapturing)
            assertNotNull(state.reviewImageBytes)
            assertEquals(1, mockCamera.captureCount)
        }

    /**
     * Verifies that confirming a captured image persists it and advances the step.
     *
     * Ensures that validating an image stores it using the injected [mockStorage]
     * and sets the view model's focus to the subsequent photo step.
     */
    @Test
    fun `onConfirm saves image and advances step`() =
        runTest {
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onConfirm()

            assertEquals(1, mockStorage.savedFiles.size)
            val state = viewModel.uiState.value
            assertEquals(steps[1], state.currentStep)
            assertNull(state.reviewImageBytes)
        }

    /**
     * Checks that confirming an empty image buffer is gracefully handled as a no-op.
     *
     * Asserts that attempting to proceed without first taking a valid photo does not
     * alter the current step nor does it inadvertently save corrupt/missing data.
     */
    @Test
    fun `onConfirm without review bytes does nothing`() =
        runTest {
            viewModel.onConfirm() // No capture beforehand
            assertEquals(0, mockStorage.savedFiles.size)
            assertEquals(steps.first(), viewModel.uiState.value.currentStep)
        }

    /**
     * Validates that requesting a retake purges the current image buffer.
     *
     * Verifies that discarding an unconfirmed image properly nullifies the review buffer
     * while retaining the current workflow step.
     */
    @Test
    fun `onRetake clears review bytes`() =
        runTest {
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.reviewImageBytes)

            viewModel.onRetake()
            assertNull(viewModel.uiState.value.reviewImageBytes)
            assertEquals(steps.first(), viewModel.uiState.value.currentStep)
        }

    /**
     * Ensures an entire multi-step capture process can execute uninterrupted.
     *
     * Simulates rapidly capturing and confirming an image for every required step,
     * asserting that the final state correctly reflects completion and that all images were saved.
     */
    @Test
    fun `full sequence completes successfully`() =
        runTest {
            val stepsCount = steps.size
            for (i in 0 until stepsCount) {
                viewModel.onCapture()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.onConfirm()
            }

            val state = viewModel.uiState.value
            assertTrue(state.isFinished)
            assertEquals(stepsCount, state.capturedCount)
            assertEquals(stepsCount, mockStorage.savedFiles.size)

            val paths = viewModel.getResultPaths()
            assertEquals(stepsCount, paths.size)
        }

    /**
     * Verifies that if captureImage throws an exception (e.g. permission permanently denied),
     * the capture state resets cleanly.
     */
    @Test
    fun `onCapture handles exceptions gracefully`() =
        runTest {
            mockCamera.throwExceptionNextTime = true
            viewModel.onCapture()

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isCapturing)
            assertNull(state.reviewImageBytes)
        }

    /**
     * Verifies that if saveImage throws an exception (e.g. file storage full),
     * the confirm action handles it by clearing reviewBytes and not advancing the step.
     */
    @Test
    fun `onConfirm handles save exceptions gracefully`() =
        runTest {
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            mockStorage.throwExceptionNextTime = true
            viewModel.onConfirm()

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(steps.first(), state.currentStep) // step did not advance
            assertNull(state.reviewImageBytes) // bytes cleared to retry
            assertEquals(0, mockStorage.savedFiles.size)
        }
}

/**
 * A testing double for the [CameraManager] providing deterministic image bytes.
 *
 * Tracks the amount of times a capture has been requested during a test.
 */
class MockCameraManager : CameraManager {
    /**
     * Accumulates the number of distinct capture attempts.
     */
    var captureCount = 0

    /**
     * Flag to force the next capture attempt to return null.
     */
    var returnNullNextTime = false

    /**
     * Flag to force the next capture attempt to throw an Exception.
     */
    var throwExceptionNextTime = false

    /**
     * Fakes an image capture by yielding predefined byte data.
     *
     * @return A fake byte array payload meant to represent a captured photo.
     */
    override suspend fun captureImage(): ByteArray? {
        captureCount++
        if (throwExceptionNextTime) {
            throwExceptionNextTime = false
            throw Exception("Simulated camera failure")
        }
        if (returnNullNextTime) {
            returnNullNextTime = false
            return null
        }
        return byteArrayOf(1, 2, 3) // Mock photo
    }

    /**
     * Stub implementation ignoring flash settings.
     *
     * @param on Boolean flag to enable or disable the flash.
     */
    override fun setFlash(on: Boolean) {}

    /**
     * Stub implementation ignoring lens toggle requests.
     */
    override fun toggleLens() {}

    /**
     * Stub implementation ignoring hardware release operations.
     */
    override fun release() {}
}

/**
 * A testing double for [FileStorage] keeping saved files strictly in-memory.
 */
class MockFileStorage : FileStorage {
    /**
     * Contains all mocked files keyed by their storage file path names.
     */
    val savedFiles = mutableMapOf<String, ByteArray>()

    var throwExceptionNextTime = false

    /**
     * Mock-saves a file in-memory.
     *
     * @param fileName The intended file name suffix.
     * @param bytes The raw file payload to be stored.
     * @return A mock path mimicking the real disk storage absolute path.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        if (throwExceptionNextTime) {
            throwExceptionNextTime = false
            throw Exception("Simulated storage full error")
        }
        savedFiles[fileName] = bytes
        return "mock_path/$fileName"
    }

    /**
     * Yields a mock-saved image payload.
     *
     * @param path The mock path the file was saved under.
     * @return The cached byte array payload.
     */
    override fun readImage(path: String): ByteArray = savedFiles[path.substringAfterLast("/")] ?: ByteArray(0)

    /**
     * Purges all mock data from the in-memory cache.
     */
    override fun clearCache() {
        savedFiles.clear()
    }
}
