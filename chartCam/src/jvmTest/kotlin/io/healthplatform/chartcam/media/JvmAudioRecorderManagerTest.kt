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
}
