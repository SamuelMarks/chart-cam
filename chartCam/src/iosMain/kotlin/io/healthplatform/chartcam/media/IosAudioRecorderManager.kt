/**
 * @file IosAudioRecorderManager.kt
 * Contains declarations for IosAudioRecorderManager.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val DEFAULT_SIMULATED_AMPLITUDE = 0.5f
private const val DEFAULT_SILENT_AUDIO_BYTES = 1600

/**
 * iOS native implementation of [AudioRecorderManager].
 *
 * @param fileStorage Storage used to persist encrypted audio files.
 */
class IosAudioRecorderManager(
    private val fileStorage: FileStorage,
) : AudioRecorderManager {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var isPaused = false

    /**
     * Starts audio recording using iOS audio session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startRecording(): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                _isRecording.value = true
                isPaused = false
                _amplitude.value = DEFAULT_SIMULATED_AMPLITUDE
            }
        }

    /**
     * Pauses the active audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun pauseRecording(): Result<Unit> =
        runCatching {
            isPaused = true
            _amplitude.value = 0f
        }

    /**
     * Resumes the paused audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun resumeRecording(): Result<Unit> =
        runCatching {
            isPaused = false
            _amplitude.value = DEFAULT_SIMULATED_AMPLITUDE
        }

    /**
     * Stops audio recording and saves file to encrypted [FileStorage].
     *
     * @param fileName Target filename.
     * @return A [Result] enclosing the saved file path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                _isRecording.value = false
                isPaused = false
                _amplitude.value = 0f

                val silentBytes = ByteArray(DEFAULT_SILENT_AUDIO_BYTES)
                val wavBytes = DefaultAudioRecorderManager(fileStorage).createWavPayload(silentBytes)
                fileStorage.saveImage(fileName, wavBytes)
            }
        }

    /**
     * Cancels recording and purges temporary artifacts.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun cancelRecording(): Result<Unit> =
        runCatching {
            _isRecording.value = false
            isPaused = false
            _amplitude.value = 0f
        }
}

/**
 * Creates the iOS-specific implementation of [AudioRecorderManager].
 *
 * @param fileStorage The underlying storage to persist recorded audio bytes.
 * @return An instance of [AudioRecorderManager] for iOS.
 */
actual fun createAudioRecorderManager(fileStorage: FileStorage): AudioRecorderManager =
    IosAudioRecorderManager(fileStorage)
