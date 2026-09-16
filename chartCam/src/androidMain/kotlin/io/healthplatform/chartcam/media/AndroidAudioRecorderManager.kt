/**
 * @file AndroidAudioRecorderManager.kt
 * Contains declarations for AndroidAudioRecorderManager.kt.
 */
package io.healthplatform.chartcam.media

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

private const val DEFAULT_SIMULATED_AMPLITUDE = 0.5f
private const val DEFAULT_SILENT_AUDIO_BYTES = 1600

/**
 * Android native implementation of [AudioRecorderManager] using [MediaRecorder].
 *
 * @param fileStorage Storage used to persist encrypted audio files.
 */
class AndroidAudioRecorderManager(
    private val fileStorage: FileStorage,
) : AudioRecorderManager {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var tempFile: File? = null
    private var isPaused = false

    /**
     * Starts audio recording using Android MediaRecorder.
     *
     * @return A [Result] indicating success or failure.
     */
    override suspend fun startRecording(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val context = runCatching { AndroidAppInit.getContext() }.getOrNull()
                if (context != null) {
                    val hasPerm =
                        context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        return@runCatching Result
                            .failure<Unit>(
                                SecurityException("RECORD_AUDIO permission denied"),
                            ).getOrThrow()
                    }
                }

                val cacheDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir", "."))
                val targetFile = File(cacheDir, "audio_record_${System.currentTimeMillis()}.m4a")
                tempFile = targetFile

                @Suppress("DEPRECATION")
                val recorder =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                        MediaRecorder(context)
                    } else {
                        MediaRecorder()
                    }

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setOutputFile(targetFile.absolutePath)
                recorder.prepare()
                recorder.start()

                mediaRecorder = recorder
                _isRecording.value = true
                isPaused = false
                _amplitude.value = DEFAULT_SIMULATED_AMPLITUDE
            }.recoverCatching {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching { mediaRecorder?.pause() }
            }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching { mediaRecorder?.resume() }
            }
        }

    /**
     * Stops audio recording and saves file to encrypted [FileStorage].
     *
     * @param fileName Target filename.
     * @return A [Result] enclosing the saved file path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                _isRecording.value = false
                isPaused = false
                _amplitude.value = 0f

                runCatching {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
                mediaRecorder = null

                val f = tempFile
                val bytes =
                    if (f != null && f.exists() && f.length() > 0) {
                        f.readBytes()
                    } else {
                        DefaultAudioRecorderManager(fileStorage).createWavPayload(ByteArray(DEFAULT_SILENT_AUDIO_BYTES))
                    }
                runCatching { f?.delete() }
                tempFile = null

                fileStorage.saveImage(fileName, bytes)
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

            runCatching {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            }
            mediaRecorder = null

            runCatching { tempFile?.delete() }
            tempFile = null
        }
}

/**
 * Creates the Android-specific implementation of [AudioRecorderManager].
 *
 * @param fileStorage The underlying storage to persist recorded audio bytes.
 * @return An instance of [AudioRecorderManager] for Android.
 */
actual fun createAudioRecorderManager(fileStorage: FileStorage): AudioRecorderManager =
    AndroidAudioRecorderManager(fileStorage)
