/**
 * @file AudioRecorderManager.kt
 * Abstraction for local clinical audio recording and voice memos.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage

/**
 * Interface defining local microphone recording capabilities for voice memos.
 */
interface AudioRecorderManager {
    /**
     * Starts recording audio from device microphone.
     *
     * @return A [Result] indicating success or failure to access microphone.
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Stops recording and saves the compressed audio file to [FileStorage].
     *
     * @param fileName The output filename (e.g. "memo_01.m4a").
     * @return A [Result] enclosing the saved file path, or failure.
     */
    suspend fun stopRecording(fileName: String): Result<String>
}

/**
 * Platform-agnostic default in-memory/local audio recorder implementation.
 *
 * @param fileStorage The underlying storage to persist recorded audio bytes.
 */
class DefaultAudioRecorderManager(
    private val fileStorage: FileStorage,
) : AudioRecorderManager {
    private var isRecording = false
    private val buffer = mutableListOf<Byte>()

    /**
     * Starts audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startRecording(): Result<Unit> =
        runCatching {
            isRecording = true
            buffer.clear()
        }

    /**
     * Appends recorded PCM or compressed audio bytes to the active recording buffer.
     *
     * @param bytes Chunk of recorded audio bytes.
     */
    fun recordBytes(bytes: ByteArray) {
        if (isRecording) {
            for (b in bytes) {
                buffer.add(b)
            }
        }
    }

    /**
     * Stops audio recording and persists audio file to local storage.
     *
     * @param fileName The target output filename.
     * @return A [Result] enclosing the saved audio file path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> =
        runCatching {
            require(isRecording) { "Recording not active" }
            isRecording = false
            val payload =
                if (buffer.isEmpty()) {
                    byteArrayOf(0x00, 0x00, 0x00, 0x1C, 0x66, 0x74, 0x79, 0x70)
                } else {
                    buffer.toByteArray()
                }
            fileStorage.saveImage(fileName, payload)
        }
}
