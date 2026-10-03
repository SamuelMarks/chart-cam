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
import platform.AVFAudio.AVAudioRecorder

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

    private var recorder: AVAudioRecorder? = null
    private var isPaused = false

    /**
     * Starts audio recording using iOS audio session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startRecording(): Result<Unit> =
        withContext(Dispatchers.Default) {
            // Return hardware unavailable for now since full native implementation
            // requires delegates and robust permission handling that exceeds
            // standard KMP standard library.
            Result.failure(AudioError.HardwareUnavailable)
        }

    /**
     * Pauses the active audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun pauseRecording(): Result<Unit> {
        if (!_isRecording.value) {
            return Result.failure(AudioError.InvalidState("Not recording"))
        }
        isPaused = true
        return Result.success(Unit)
    }

    /**
     * Resumes the paused audio recording session.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun resumeRecording(): Result<Unit> {
        if (!_isRecording.value) {
            return Result.failure(AudioError.InvalidState("Not recording"))
        }
        isPaused = false
        return Result.success(Unit)
    }

    /**
     * Stops audio recording and saves file to encrypted [FileStorage].
     *
     * @param fileName Target filename.
     * @return A [Result] enclosing the saved file path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> =
        withContext(Dispatchers.Default) {
            if (!_isRecording.value) {
                return@withContext Result.failure(AudioError.InvalidState("Not recording"))
            }
            Result.failure(AudioError.HardwareUnavailable)
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
            recorder?.stop()
            recorder = null
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
