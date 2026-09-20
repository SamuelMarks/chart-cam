/**
 * @file AndroidAudioRecorderManagerAndroidTest.kt
 * Contains declarations for AndroidAudioRecorderManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.media

import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for AndroidAudioRecorderManager on Android host.
 */
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AndroidAudioRecorderManagerAndroidTest {
    /**
     * Set up AndroidAppInit with Robolectric application context.
     */
    @Before
    fun setUp() {
        AndroidAppInit.init(RuntimeEnvironment.getApplication())
    }

    /**
     * Tests full recording lifecycle on Android.
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

            val stopRes = recorder.stopRecording("test_memo_android.wav")
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
