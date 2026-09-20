/**
 * @file AudioRecorderManager.kt
 * Abstraction for local clinical audio recording and voice memos.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.files.saveImageCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface defining local microphone recording capabilities for voice memos.
 */
interface AudioRecorderManager {
    /**
     * Observable state flow indicating whether audio recording is currently active.
     */
    val isRecording: StateFlow<Boolean>

    /**
     * Observable state flow indicating the current audio input amplitude.
     */
    val amplitude: StateFlow<Float>

    /**
     * Starts recording audio from device microphone.
     *
     * @return A [Result] indicating success or failure to access microphone.
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Pauses the active audio recording session.
     *
     * @return A [Result] indicating success or failure.
     */
    suspend fun pauseRecording(): Result<Unit>

    /**
     * Resumes a paused audio recording session.
     *
     * @return A [Result] indicating success or failure.
     */
    suspend fun resumeRecording(): Result<Unit>

    /**
     * Stops recording and saves the compressed audio file to [FileStorage].
     *
     * @param fileName The output filename (e.g. "memo_01.wav" or "memo_01.m4a").
     * @return A [Result] enclosing the saved file path, or failure.
     */
    suspend fun stopRecording(fileName: String): Result<String>

    /**
     * Cancels and discards the active audio recording without saving.
     *
     * @return A [Result] indicating success or failure.
     */
    suspend fun cancelRecording(): Result<Unit>
}

/**
 * Creates the platform-specific implementation of [AudioRecorderManager].
 *
 * @param fileStorage The underlying storage for persisting audio recordings.
 * @return An instance of [AudioRecorderManager].
 */
expect fun createAudioRecorderManager(fileStorage: FileStorage): AudioRecorderManager

/**
 * Platform-agnostic default in-memory/local audio recorder implementation.
 *
 * @param fileStorage The underlying storage to persist recorded audio bytes.
 */
@Suppress("MagicNumber")
class DefaultAudioRecorderManager(
    private val fileStorage: FileStorage,
) : AudioRecorderManager {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val buffer = mutableListOf<Byte>()
    private var isPaused = false

    /**
     * Starts audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startRecording(): Result<Unit> =
        runCatching {
            _isRecording.value = true
            isPaused = false
            buffer.clear()
            _amplitude.value = 0.5f
        }

    /**
     * Pauses the active recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun pauseRecording(): Result<Unit> {
        if (!_isRecording.value) {
            return Result.failure(IllegalStateException("Cannot pause when recording is not active"))
        }
        isPaused = true
        _amplitude.value = 0f
        return Result.success(Unit)
    }

    /**
     * Resumes the paused recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun resumeRecording(): Result<Unit> {
        if (!_isRecording.value) {
            return Result.failure(IllegalStateException("Cannot resume when recording is not active"))
        }
        isPaused = false
        _amplitude.value = 0.5f
        return Result.success(Unit)
    }

    /**
     * Cancels the active recording session and purges buffer.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun cancelRecording(): Result<Unit> =
        runCatching {
            _isRecording.value = false
            isPaused = false
            buffer.clear()
            _amplitude.value = 0f
        }

    /**
     * Appends recorded PCM or compressed audio bytes to the active recording buffer.
     *
     * @param bytes Chunk of recorded audio bytes.
     */
    fun recordBytes(bytes: ByteArray) {
        if (_isRecording.value && !isPaused) {
            for (b in bytes) {
                buffer.add(b)
            }
            if (bytes.isNotEmpty()) {
                _amplitude.value = (bytes[0].toInt().coerceIn(0, 127) / 127f)
            }
        }
    }

    /**
     * Encapsulates raw PCM byte samples inside a standard 44-byte RIFF WAV audio container.
     *
     * @param pcmData The raw PCM audio samples.
     * @param sampleRate The sampling frequency in Hertz.
     * @param channels Number of audio channels (1 for mono, 2 for stereo).
     * @param bitsPerSample Bit depth per sample (16 bits standard).
     * @return A complete valid WAV byte array.
     */
    fun createWavPayload(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Short = 1,
        bitsPerSample: Short = 16,
    ): ByteArray {
        val subChunk2Size = pcmData.size
        val chunkSize = 36 + subChunk2Size
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (chunkSize and 0xff).toByte()
        header[5] = ((chunkSize shr 8) and 0xff).toByte()
        header[6] = ((chunkSize shr 16) and 0xff).toByte()
        header[7] = ((chunkSize shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat: 1 = PCM
        header[21] = 0
        header[22] = (channels.toInt() and 0xff).toByte()
        header[23] = ((channels.toInt() shr 8) and 0xff).toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xff).toByte()
        header[34] = (bitsPerSample.toInt() and 0xff).toByte()
        header[35] = ((bitsPerSample.toInt() shr 8) and 0xff).toByte()

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (subChunk2Size and 0xff).toByte()
        header[41] = ((subChunk2Size shr 8) and 0xff).toByte()
        header[42] = ((subChunk2Size shr 16) and 0xff).toByte()
        header[43] = ((subChunk2Size shr 24) and 0xff).toByte()

        return header + pcmData
    }

    /**
     * Stops audio recording and persists audio file to local storage.
     *
     * @param fileName The target output filename.
     * @return A [Result] enclosing the saved audio file path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> {
        if (!_isRecording.value) {
            return Result.failure(IllegalStateException("Recording not active"))
        }
        _isRecording.value = false
        isPaused = false
        _amplitude.value = 0f
        val rawPcm =
            if (buffer.isEmpty()) {
                ByteArray(1600) // 50ms of quiet audio
            } else {
                buffer.toByteArray()
            }
        val payload = createWavPayload(rawPcm)
        return fileStorage.saveImageCatching(fileName, payload)
    }
}
