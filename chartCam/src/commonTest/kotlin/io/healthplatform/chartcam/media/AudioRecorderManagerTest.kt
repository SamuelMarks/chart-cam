/**
 * @file AudioRecorderManagerTest.kt
 * Contains declarations for AudioRecorderManagerTest.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AudioRecorderManager and DefaultAudioRecorderManager.
 */
class AudioRecorderManagerTest {
    /**
     * In-memory mock [FileStorage] implementation for testing.
     */
    private class FakeAudioFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return "/tmp/$fileName"
        }

        override fun readImage(path: String): ByteArray = files[path] ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> {
            files.remove(path)
            return Result.success(Unit)
        }

        override fun clearCache() {
            files.clear()
        }
    }

    /**
     * Verifies the full audio recording lifecycle including pause, resume, byte recording, and WAV creation.
     */
    @Test
    fun testAudioRecordingLifecycle() =
        runTest {
            val storage = FakeAudioFileStorage()
            val recorder = DefaultAudioRecorderManager(storage)

            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)

            // Feed bytes when not recording (should be ignored)
            recorder.recordBytes(byteArrayOf(5, 6))
            assertEquals(0f, recorder.amplitude.value)

            // Start recording
            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)

            // Feed PCM bytes
            recorder.recordBytes(byteArrayOf(10, 20, 30, 40))
            assertTrue(recorder.amplitude.value > 0f)

            // Pause
            val pauseRes = recorder.pauseRecording()
            assertTrue(pauseRes.isSuccess)
            assertEquals(0f, recorder.amplitude.value)

            // Feed bytes when paused (should be ignored)
            recorder.recordBytes(byteArrayOf(7, 8))
            assertEquals(0f, recorder.amplitude.value)

            // Resume
            val resumeRes = recorder.resumeRecording()
            assertTrue(resumeRes.isSuccess)

            // Stop and save
            val fileName = "test_memo_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.wav"
            val stopRes = recorder.stopRecording(fileName)
            assertTrue(stopRes.isSuccess)
            assertNotNull(stopRes.getOrNull())
            assertFalse(recorder.isRecording.value)

            // Verify WAV header
            val wavBytes = recorder.createWavPayload(byteArrayOf(1, 2, 3, 4))
            assertEquals('R'.code.toByte(), wavBytes[0])
            assertEquals('I'.code.toByte(), wavBytes[1])
            assertEquals('F'.code.toByte(), wavBytes[2])
            assertEquals('F'.code.toByte(), wavBytes[3])
            assertEquals('W'.code.toByte(), wavBytes[8])
            assertEquals('A'.code.toByte(), wavBytes[9])
            assertEquals('V'.code.toByte(), wavBytes[10])
            assertEquals('E'.code.toByte(), wavBytes[11])
        }

    /**
     * Verifies canceling an audio recording session.
     */
    @Test
    fun testCancelRecording() =
        runTest {
            val storage = FakeAudioFileStorage()
            val recorder = DefaultAudioRecorderManager(storage)

            recorder.startRecording()
            assertTrue(recorder.isRecording.value)

            val cancelRes = recorder.cancelRecording()
            assertTrue(cancelRes.isSuccess)
            assertFalse(recorder.isRecording.value)
        }

    /**
     * Verifies invalid state handling when pausing or stopping an unstarted recording.
     */
    @Test
    fun testAudioRecorderErrors() =
        runTest {
            val storage = FakeAudioFileStorage()
            val recorder = DefaultAudioRecorderManager(storage)

            assertTrue(recorder.pauseRecording().isFailure)
            assertTrue(recorder.resumeRecording().isFailure)
            assertTrue(recorder.stopRecording("test.wav").isFailure)

            recorder.startRecording()
            recorder.recordBytes(byteArrayOf())
            assertTrue(recorder.stopRecording("empty.wav").isSuccess)
        }
}
