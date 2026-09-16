/**
 * @file AudioMemoControlJvmTest.kt
 * Contains declarations for AudioMemoControlJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.media.DefaultAudioRecorderManager
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JVM unit tests for AudioMemoControl.
 */
@OptIn(ExperimentalTestApi::class)
class AudioMemoControlJvmTest {
    /**
     * Tests recording and stopping audio memo lifecycle in AudioMemoControl.
     */
    @Test
    fun testAudioMemoControlLifecycle() =
        runComposeUiTest {
            val storage = createFileStorage()
            val recorder = DefaultAudioRecorderManager(storage)
            var recordedPath: String? = null
            var dismissed = false

            setContent {
                AudioMemoControl(
                    recorder = recorder,
                    onMemoRecorded = { path -> recordedPath = path },
                    onDismiss = { dismissed = true },
                )
            }

            // Verify initial UI elements
            onNodeWithTag("AudioMemoControlCard").assertIsDisplayed()
            onNodeWithTag("StartAudioRecordButton").assertIsDisplayed()
            onNodeWithTag("AudioDurationCounter").assertIsDisplayed()

            // Click start recording
            onNodeWithTag("StartAudioRecordButton").performClick()

            // Verify stop button is displayed
            onNodeWithTag("StopAudioRecordButton").assertIsDisplayed()

            // Click stop and save
            onNodeWithTag("StopAudioRecordButton").performClick()
            waitForIdle()

            // Verify callback invoked with saved path
            assertNotNull(recordedPath)

            // Verify close button triggers dismissal
            onNodeWithTag("CloseAudioMemoButton").performClick()
            assertTrue(dismissed)
        }
}
