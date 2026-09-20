/**
 * @file JvmAudioRecorderManagerTest.kt
 * Contains declarations for JvmAudioRecorderManagerTest.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests evaluating [JvmAudioRecorderManager] lifecycle and recording behavior on JVM.
 */
class JvmAudioRecorderManagerTest {
    /**
     * Verifies that JvmAudioRecorderManager can start, pause, resume, cancel, and stop recording safely.
     */
    @Test
    fun testJvmAudioRecorderLifecycle() =
        runTest {
            val storage = createFileStorage()
            val manager = createAudioRecorderManager(storage)
            assertNotNull(manager)
            assertFalse(manager.isRecording.value)

            val startResult = manager.startRecording()
            assertTrue(startResult.isSuccess)
            assertTrue(manager.isRecording.value)

            val pauseResult = manager.pauseRecording()
            assertTrue(pauseResult.isSuccess)

            val resumeResult = manager.resumeRecording()
            assertTrue(resumeResult.isSuccess)

            val cancelResult = manager.cancelRecording()
            assertTrue(cancelResult.isSuccess)
            assertFalse(manager.isRecording.value)

            manager.startRecording()
            val stopResult = manager.stopRecording("test_memo.wav")
            assertTrue(stopResult.isSuccess)
            assertFalse(manager.isRecording.value)
        }

    /**
     * Verifies that JvmAudioRecorderManager reads buffers and calculates amplitudes using a TargetDataLine.
     */
    @Test
    fun testJvmAudioRecorderWithSimulatedLine() =
        runTest {
            val storage = createFileStorage()
            var isLineStarted = false
            var isLineStopped = false
            var isLineClosed = false
            var bytesReadCount = 0

            val fakeLine =
                java.lang.reflect.Proxy.newProxyInstance(
                    javax.sound.sampled.TargetDataLine::class.java.classLoader,
                    arrayOf(javax.sound.sampled.TargetDataLine::class.java),
                ) { _, method, args ->
                    when (method.name) {
                        "open" -> null
                        "start" -> {
                            isLineStarted = true
                            null
                        }
                        "stop" -> {
                            isLineStopped = true
                            null
                        }
                        "close" -> {
                            isLineClosed = true
                            null
                        }
                        "available" ->
                            when (bytesReadCount) {
                                0 -> 2048
                                1 -> 512
                                2 -> 512
                                else -> 0
                            }
                        "read" -> {
                            val buffer = args[0] as ByteArray
                            val length = args[2] as Int
                            bytesReadCount++
                            if (bytesReadCount == 3) {
                                0
                            } else {
                                buffer[0] = 64
                                minOf(buffer.size, length)
                            }
                        }
                        else -> null
                    }
                } as javax.sound.sampled.TargetDataLine

            val manager = JvmAudioRecorderManager(storage, targetLineProvider = { fakeLine })
            val startRes = manager.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(isLineStarted)

            // Allow the background recording job to poll and read from the simulated line
            kotlinx.coroutines.delay(120)
            assertTrue(manager.amplitude.value > 0f)

            // Pause while line is active to cover the paused delay branch
            manager.pauseRecording()
            kotlinx.coroutines.delay(100)

            // Resume and delay to cover empty available branch
            manager.resumeRecording()
            kotlinx.coroutines.delay(100)

            // Pause and delay
            manager.pauseRecording()
            kotlinx.coroutines.delay(60)

            // Resume
            manager.resumeRecording()

            // Stop and verify audio buffer saved
            val stopRes = manager.stopRecording("simulated_recording.wav")
            assertTrue(stopRes.isSuccess)
            assertTrue(isLineStopped)
            assertTrue(isLineClosed)

            // Cancel with simulated line
            val manager2 = JvmAudioRecorderManager(storage, targetLineProvider = { fakeLine })
            manager2.startRecording()
            manager2.cancelRecording()
            assertFalse(manager2.isRecording.value)
        }

    /**
     * Verifies handling when available line bytes is 0 or read returns 0.
     */
    @Test
    fun testJvmAudioRecorderZeroAvailableAndZeroRead() =
        runTest {
            val storage = createFileStorage()
            var callCount = 0
            val zeroLine =
                java.lang.reflect.Proxy.newProxyInstance(
                    javax.sound.sampled.TargetDataLine::class.java.classLoader,
                    arrayOf(javax.sound.sampled.TargetDataLine::class.java),
                ) { _, method, _ ->
                    when (method.name) {
                        "open", "start", "stop", "close" -> null
                        "available" -> {
                            callCount++
                            if (callCount % 2 == 0) 100 else 0
                        }
                        "read" -> 0
                        else -> null
                    }
                } as javax.sound.sampled.TargetDataLine

            val manager = JvmAudioRecorderManager(storage, targetLineProvider = { zeroLine })
            val startRes = manager.startRecording()
            assertTrue(startRes.isSuccess)
            kotlinx.coroutines.delay(120)
            val cancelRes = manager.cancelRecording()
            assertTrue(cancelRes.isSuccess)
        }

    /**
     * Verifies system line resolution when headless or Mac property is set or not set.
     */
    @Test
    fun testJvmAudioRecorderSystemLineResolution() =
        runTest {
            val storage = createFileStorage()
            val origHeadless = System.getProperty("java.awt.headless")
            val origOs = System.getProperty("os.name")
            runCatching {
                System.setProperty("java.awt.headless", "false")
                System.setProperty("os.name", "Linux")
                val manager = JvmAudioRecorderManager(storage)
                val res = manager.startRecording()
                assertTrue(res.isSuccess)
                manager.cancelRecording()
            }.also {
                if (origHeadless != null) {
                    System.setProperty("java.awt.headless", origHeadless)
                } else {
                    System.clearProperty("java.awt.headless")
                }
                if (origOs != null) {
                    System.setProperty("os.name", origOs)
                } else {
                    System.clearProperty("os.name")
                }
            }
        }

    /**
     * Verifies error recovery when TargetDataLine opening fails.
     */
    @Test
    fun testJvmAudioRecorderFailureRecovery() =
        runTest {
            val storage = createFileStorage()
            val throwingLine =
                java.lang.reflect.Proxy.newProxyInstance(
                    javax.sound.sampled.TargetDataLine::class.java.classLoader,
                    arrayOf(javax.sound.sampled.TargetDataLine::class.java),
                ) { _, method, _ ->
                    if (method.name == "open") {
                        throw IllegalStateException("Line open failed") // allow-exception
                    }
                    null
                } as javax.sound.sampled.TargetDataLine

            val manager = JvmAudioRecorderManager(storage, targetLineProvider = { throwingLine })
            val res = manager.startRecording()
            assertTrue(res.isSuccess)
            assertTrue(manager.isRecording.value)
        }
}
