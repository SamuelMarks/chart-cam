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
            assertTrue(startRes.isFailure)
            assertFalse(recorder.isRecording.value)

            val stopRes = recorder.stopRecording("test_memo_ios.wav")
            assertTrue(stopRes.isFailure)

            val pauseRes = recorder.pauseRecording()
            assertTrue(pauseRes.isFailure)

            val resumeRes = recorder.resumeRecording()
            assertTrue(resumeRes.isFailure)
        }
}
