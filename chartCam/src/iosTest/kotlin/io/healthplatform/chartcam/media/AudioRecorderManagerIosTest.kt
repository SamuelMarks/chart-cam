/**
 * @file AudioRecorderManagerIosTest.kt
 * Contains declarations for AudioRecorderManagerIosTest.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for AudioRecorderManager on iOS.
 */
class AudioRecorderManagerIosTest {
    /**
     * Tests full recording lifecycle on iOS.
     */
    @Test
    fun testAudioRecordingLifecycle() =
        runTest {
            val storage = createFileStorage()
            val recorder = createAudioRecorderManager(storage)
            assertNotNull(recorder)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)

            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)
            assertEquals(0.5f, recorder.amplitude.value)

            val pauseRes = recorder.pauseRecording()
            assertTrue(pauseRes.isSuccess)
            assertEquals(0f, recorder.amplitude.value)

            val resumeRes = recorder.resumeRecording()
            assertTrue(resumeRes.isSuccess)
            assertEquals(0.5f, recorder.amplitude.value)

            val stopRes = recorder.stopRecording("test_memo_ios.wav")
            assertTrue(stopRes.isSuccess)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)

            val restartRes = recorder.startRecording()
            assertTrue(restartRes.isSuccess)
            assertTrue(recorder.isRecording.value)

            val cancelRes = recorder.cancelRecording()
            assertTrue(cancelRes.isSuccess)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)
        }
}
