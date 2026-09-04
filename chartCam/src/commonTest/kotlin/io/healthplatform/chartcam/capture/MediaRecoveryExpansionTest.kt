/**
 * @file MediaRecoveryExpansionTest.kt
 * Contains declarations for MediaRecoveryExpansionTest.kt.
 */
package io.healthplatform.chartcam.capture

import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.files.ImageMetadataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Unit tests covering Section 5: Zero-Length & Corrupted Media Recovery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaRecoveryExpansionTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Set Main dispatcher for coroutines.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Reset Main dispatcher after test.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Fake camera manager for controlling capture output.
     */
    private class FakeCameraManager : CameraManager {
        var returnBytes: ByteArray? = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        override suspend fun captureImage(): ByteArray? = returnBytes

        override fun setFlash(on: Boolean) {}

        override fun toggleLens() {}

        override fun release() {}
    }

    /**
     * Fake file storage for controlling disk full errors.
     */
    private class FakeFileStorage : FileStorage {
        var shouldThrowDiskFull = false
        val savedFiles = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            if (shouldThrowDiskFull) {
                throw IllegalStateException("No space left on device")
            }
            savedFiles[fileName] = bytes
            return "/storage/$fileName"
        }

        override fun readImage(path: String): ByteArray = savedFiles[path.substringAfterLast("/")] ?: ByteArray(0)

        override fun clearCache() {
            savedFiles.clear()
        }
    }

    /**
     * Verify cleanup and alerting when camera capture returns zero-byte / empty data.
     */
    @Test
    fun testInterruptedPhotoCapture() =
        runTest(testDispatcher) {
            val camera = FakeCameraManager()
            val storage = FakeFileStorage()
            val viewModel = CaptureViewModel(camera, storage)

            val steps = listOf(PhotoStep("step1", "Step 1"))
            viewModel.initSteps(steps)

            // Simulate interrupted/empty capture
            camera.returnBytes = ByteArray(0)
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isCapturing)
            assertNull(state.reviewImageBytes)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage.contains("empty") || state.errorMessage.contains("interrupted"))
            assertEquals(0, state.capturedCount)
        }

    /**
     * Verify handling and user alerting when device storage is completely full prior to or during capture.
     */
    @Test
    fun testDiskSpaceExhaustion() =
        runTest(testDispatcher) {
            val camera = FakeCameraManager()
            val storage = FakeFileStorage()
            val viewModel = CaptureViewModel(camera, storage)

            val steps = listOf(PhotoStep("step1", "Step 1"))
            viewModel.initSteps(steps)

            // Valid capture
            camera.returnBytes = byteArrayOf(1, 2, 3)
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            // Confirm when disk is completely full
            storage.shouldThrowDiskFull = true
            viewModel.onConfirm()

            val state = viewModel.uiState.value
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage.contains("storage") || state.errorMessage.contains("disk"))
            assertNull(state.reviewImageBytes)
            assertEquals(0, state.capturedCount)

            // Clear error restores clean UI state
            viewModel.clearError()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    /**
     * Test parser and repository resilience against malformed EXIF headers and unreadable image blobs.
     */
    @Test
    fun testCorruptedExifAndMetadata() {
        // 1. Null and empty inputs
        val nullMeta = ImageMetadataParser.parse(null)
        assertTrue(nullMeta.isCorrupted)

        val emptyMeta = ImageMetadataParser.parse(ByteArray(0))
        assertTrue(emptyMeta.isCorrupted)

        // 2. Truncated / garbage headers
        val garbageBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val garbageMeta = ImageMetadataParser.parse(garbageBytes)
        assertTrue(garbageMeta.isCorrupted)

        // 3. Truncated JPEG (SOI marker present, followed by corrupt incomplete EXIF marker)
        val truncatedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00.toByte(), 0x20.toByte())
        val truncatedMeta = ImageMetadataParser.parse(truncatedJpeg)
        assertTrue(truncatedMeta.isCorrupted)

        // 4. Valid PNG header recognition
        val pngHeader =
            byteArrayOf(
                0x89.toByte(),
                'P'.code.toByte(),
                'N'.code.toByte(),
                'G'.code.toByte(),
                0x0D.toByte(),
                0x0A.toByte(),
                0x1A.toByte(),
                0x0A.toByte(),
            )
        val pngMeta = ImageMetadataParser.parse(pngHeader)
        assertFalse(pngMeta.isCorrupted)
        assertEquals("image/png", pngMeta.mimeType)

        // 5. Minimal valid JPEG with SOF0 dimensions
        val validJpegWithSof =
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(), // SOI
                0xFF.toByte(),
                0xC0.toByte(), // SOF0
                0x00.toByte(),
                0x11.toByte(), // length = 17
                0x08.toByte(), // precision = 8
                0x01.toByte(),
                0x00.toByte(), // height = 256
                0x02.toByte(),
                0x00.toByte(), // width = 512
                0x03.toByte(), // components = 3
                0x01.toByte(),
                0x11.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x11.toByte(),
                0x01.toByte(),
                0x03.toByte(),
                0x11.toByte(),
                0x01.toByte(),
                0xFF.toByte(),
                0xD9.toByte(), // EOI
            )
        val validMeta = ImageMetadataParser.parse(validJpegWithSof)
        assertFalse(validMeta.isCorrupted)
        assertEquals(512, validMeta.width)
        assertEquals(256, validMeta.height)
    }

    /**
     * Validate recovery of pending photo queue and encounter associations after unexpected process termination.
     */
    @Test
    fun testSessionRestorationAfterCrash() =
        runTest(testDispatcher) {
            val camera = FakeCameraManager()
            val storage = FakeFileStorage()

            val step1 = PhotoStep("front", "Front")
            val step2 = PhotoStep("right", "Right Side")
            val step3 = PhotoStep("back", "Back")
            val steps = listOf(step1, step2, step3)

            // 1. Initial run: complete step 1, then "crash"
            val initialVm = CaptureViewModel(camera, storage)
            initialVm.initSteps(steps)
            camera.returnBytes = byteArrayOf(10, 20, 30)
            initialVm.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            initialVm.onConfirm()

            val savedBeforeCrash = initialVm.getResultPaths()
            assertEquals(1, savedBeforeCrash.size)
            assertTrue(savedBeforeCrash.containsKey(step1))

            // 2. Fresh ViewModel following process restart
            val restoredVm = CaptureViewModel(camera, storage)
            restoredVm.initSteps(steps)

            // Restore previously persisted capture map
            restoredVm.restoreSession(savedBeforeCrash)

            val restoredState = restoredVm.uiState.value
            assertEquals(1, restoredState.capturedCount)
            assertEquals(step2, restoredState.currentStep)
            assertFalse(restoredState.isFinished)

            // Complete remaining steps
            camera.returnBytes = byteArrayOf(40, 50)
            restoredVm.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            restoredVm.onConfirm()

            camera.returnBytes = byteArrayOf(60, 70)
            restoredVm.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            restoredVm.onConfirm()

            val finalState = restoredVm.uiState.value
            assertTrue(finalState.isFinished)
            assertEquals(3, finalState.capturedCount)

            val finalPaths = restoredVm.getResultPaths()
            assertEquals(3, finalPaths.size)
            assertTrue(finalPaths.containsKey(step1))
            assertTrue(finalPaths.containsKey(step2))
            assertTrue(finalPaths.containsKey(step3))
        }
}
