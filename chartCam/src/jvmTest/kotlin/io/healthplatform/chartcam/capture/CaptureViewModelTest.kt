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

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockCamera: MockCameraManager
    private lateinit var mockStorage: MockFileStorage
    private lateinit var viewModel: CaptureViewModel
    private val steps = PhotoStep.STANDARD_STEPS

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockCamera = MockCameraManager()
        mockStorage = MockFileStorage()
        viewModel = CaptureViewModel(mockCamera, mockStorage)
        viewModel.initSteps(steps)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(steps.first(), state.currentStep)
        assertFalse(state.isCapturing)
        assertNull(state.reviewImageBytes)
        assertNull(state.ghostImageBytes)
    }

    @Test
    fun `onCapture triggers camera and updates review image`() = runTest {
        viewModel.onCapture()
        assertTrue(viewModel.uiState.value.isCapturing)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCapturing)
        assertNotNull(state.reviewImageBytes)
        assertEquals(1, mockCamera.captureCount)
    }

    @Test
    fun `onConfirm saves image and advances step`() = runTest {
        viewModel.onCapture()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onConfirm()

        assertEquals(1, mockStorage.savedFiles.size)
        val state = viewModel.uiState.value
        assertEquals(steps[1], state.currentStep)
        assertNull(state.reviewImageBytes)
        
        // Front -> Front Ruler means ghosting should be active
        assertNotNull(state.ghostImageBytes)
    }

    @Test
    fun `onConfirm without review bytes does nothing`() = runTest {
        viewModel.onConfirm() // No capture beforehand
        assertEquals(0, mockStorage.savedFiles.size)
        assertEquals(steps.first(), viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onRetake clears review bytes`() = runTest {
        viewModel.onCapture()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.reviewImageBytes)

        viewModel.onRetake()
        assertNull(viewModel.uiState.value.reviewImageBytes)
        assertEquals(steps.first(), viewModel.uiState.value.currentStep)
    }

    @Test
    fun `full sequence completes successfully`() = runTest {
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
}

class MockCameraManager : CameraManager {
    var captureCount = 0
    override suspend fun captureImage(): ByteArray? {
        captureCount++
        return byteArrayOf(1, 2, 3) // Mock photo
    }
    override fun setFlash(on: Boolean) {}
    override fun toggleLens() {}
    override fun release() {}
}

class MockFileStorage : FileStorage {
    val savedFiles = mutableMapOf<String, ByteArray>()
    override fun saveImage(fileName: String, bytes: ByteArray): String {
        savedFiles[fileName] = bytes
        return "mock_path/$fileName"
    }
    override fun readImage(path: String): ByteArray {
        return savedFiles[path.substringAfterLast("/")] ?: ByteArray(0)
    }
    override fun clearCache() {
        savedFiles.clear()
    }
}
