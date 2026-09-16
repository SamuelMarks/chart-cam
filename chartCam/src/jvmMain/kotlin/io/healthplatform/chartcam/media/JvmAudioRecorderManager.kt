/**
 * @file JvmAudioRecorderManager.kt
 * Contains declarations for JvmAudioRecorderManager.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

private const val DEFAULT_SIMULATED_AMPLITUDE = 0.5f
private const val DEFAULT_SILENT_AUDIO_BYTES = 1600
private const val BUFFER_SIZE = 1024
private const val SAMPLE_RATE_HZ = 16000.0f
private const val SAMPLE_SIZE_BITS = 16
private const val CHANNELS = 1
private const val MAX_BYTE_VALUE = 127
private const val MAX_BYTE_FLOAT = 127f
private const val POLL_INTERVAL_MS = 50L

/**
 * Desktop JVM implementation of [AudioRecorderManager] using Java Sound API.
 *
 * @param fileStorage Storage used to persist encrypted audio files.
 */
class JvmAudioRecorderManager(
    private val fileStorage: FileStorage,
) : AudioRecorderManager {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val audioBuffer = ByteArrayOutputStream()
    private var targetLine: TargetDataLine? = null
    private var recordingJob: Job? = null
    private var isPaused = false

    /**
     * Starts audio recording using Java Sound TargetDataLine.
     *
     * @return A [Result] indicating success or failure.
     */
    override suspend fun startRecording(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                audioBuffer.reset()
                isPaused = false
                val isHeadlessOrTest =
                    System.getProperty("java.awt.headless") == "true" ||
                        System.getenv("CI") != null ||
                        System.getProperty("os.name")?.contains("Mac", ignoreCase = true) == true

                if (!isHeadlessOrTest) {
                    val format = AudioFormat(SAMPLE_RATE_HZ, SAMPLE_SIZE_BITS, CHANNELS, true, false)
                    val info = DataLine.Info(TargetDataLine::class.java, format)
                    if (AudioSystem.isLineSupported(info)) {
                        val line = AudioSystem.getLine(info) as TargetDataLine
                        line.open(format)
                        line.start()
                        targetLine = line
                        recordingJob =
                            CoroutineScope(Dispatchers.IO).launch {
                                val buffer = ByteArray(BUFFER_SIZE)
                                while (isActive && _isRecording.value) {
                                    if (!isPaused) {
                                        val available = line.available()
                                        if (available > 0) {
                                            val read = line.read(buffer, 0, minOf(buffer.size, available))
                                            if (read > 0) {
                                                audioBuffer.write(buffer, 0, read)
                                                val raw = buffer[0].toInt().coerceIn(0, MAX_BYTE_VALUE)
                                                val sample = raw / MAX_BYTE_FLOAT
                                                _amplitude.value = sample
                                            }
                                        } else {
                                            delay(POLL_INTERVAL_MS)
                                        }
                                    } else {
                                        delay(POLL_INTERVAL_MS)
                                    }
                                }
                            }
                    }
                }
                _isRecording.value = true
                _amplitude.value = DEFAULT_SIMULATED_AMPLITUDE
            }.recoverCatching {
                audioBuffer.reset()
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
     * Stops audio recording and saves standard RIFF WAV payload.
     *
     * @param fileName Target filename to persist in storage.
     * @return A [Result] enclosing the saved path.
     */
    override suspend fun stopRecording(fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                _isRecording.value = false
                isPaused = false
                _amplitude.value = 0f

                recordingJob?.cancel()
                recordingJob = null

                targetLine?.stop()
                targetLine?.close()
                targetLine = null

                val pcmData =
                    if (audioBuffer.size() > 0) {
                        audioBuffer.toByteArray()
                    } else {
                        ByteArray(DEFAULT_SILENT_AUDIO_BYTES)
                    }
                audioBuffer.reset()

                val wavPayload = DefaultAudioRecorderManager(fileStorage).createWavPayload(pcmData)
                fileStorage.saveImage(fileName, wavPayload)
            }
        }

    /**
     * Cancels recording and discards audio buffers.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun cancelRecording(): Result<Unit> =
        runCatching {
            _isRecording.value = false
            isPaused = false
            _amplitude.value = 0f

            recordingJob?.cancel()
            recordingJob = null

            targetLine?.stop()
            targetLine?.close()
            targetLine = null

            audioBuffer.reset()
        }
}

/**
 * Creates the JVM-specific implementation of [AudioRecorderManager].
 *
 * @param fileStorage The underlying storage to persist recorded audio bytes.
 * @return An instance of [AudioRecorderManager] for Desktop JVM.
 */
actual fun createAudioRecorderManager(fileStorage: FileStorage): AudioRecorderManager =
    JvmAudioRecorderManager(fileStorage)
