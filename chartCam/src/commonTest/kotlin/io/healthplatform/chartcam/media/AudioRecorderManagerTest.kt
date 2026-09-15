/**
 * @file AudioRecorderManagerTest.kt
 * Tests for AudioRecorderManager.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests verifying audio recording lifecycle and storage persistence.
 */
class AudioRecorderManagerTest {
    /**
     * In-memory storage mock.
     */
    private val mockStorage =
        object : FileStorage {
            val files = mutableMapOf<String, ByteArray>()

            override fun saveImage(
                fileName: String,
                bytes: ByteArray,
            ): String {
                files[fileName] = bytes
                return fileName
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
     * Tests recording start and stop saving an audio file.
     */
    @Test
    fun testStartAndStopRecording() =
        runTest {
            val recorder = DefaultAudioRecorderManager(mockStorage)
            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)

            val stopRes = recorder.stopRecording("test_memo.m4a")
            assertTrue(stopRes.isSuccess)
            assertEquals("test_memo.m4a", stopRes.getOrNull())
            assertTrue(mockStorage.files.containsKey("test_memo.m4a"))
        }

    /**
     * Tests recording with custom recorded byte stream payload.
     */
    @Test
    fun testRecordingWithNonEmptyBuffer() =
        runTest {
            val recorder = DefaultAudioRecorderManager(mockStorage)
            // Call recordBytes before startRecording to cover false branch
            recorder.recordBytes(byteArrayOf(0x99.toByte()))

            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)

            val customAudio = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            recorder.recordBytes(customAudio)

            val stopRes = recorder.stopRecording("custom_memo.m4a")
            assertTrue(stopRes.isSuccess)
            kotlin.test.assertContentEquals(customAudio, mockStorage.files["custom_memo.m4a"])
        }

    /**
     * Tests stopping without starting returns failure.
     */
    @Test
    fun testStopWithoutStartFails() =
        runTest {
            val recorder = DefaultAudioRecorderManager(mockStorage)
            val stopRes = recorder.stopRecording("unstarted.m4a")
            assertTrue(stopRes.isFailure)
        }
}
