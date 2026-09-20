/**
 * @file AudioMemoControlJvmTest.kt
 * Contains declarations for AudioMemoControlJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.media.DefaultAudioRecorderManager
import io.healthplatform.chartcam.ui.setAppLanguage
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
            setAppLanguage("en")
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

            // Verify initial UI elements and localized title
            onNodeWithTag("AudioMemoControlCard").assertIsDisplayed()
            onNodeWithText("Clinical Voice Memo", useUnmergedTree = true).assertIsDisplayed()
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

            // Verify review controls: Play and Redo
            onNodeWithTag("PlayAudioPreviewButton").assertIsDisplayed()
            onNodeWithTag("DeleteAudioRecordButton").assertIsDisplayed()

            // Click play / pause toggle
            onNodeWithTag("PlayAudioPreviewButton").performClick()
            onNodeWithText("Pause", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("PlayAudioPreviewButton").performClick()
            onNodeWithText("Play", useUnmergedTree = true).assertIsDisplayed()

            // Click redo to reset back to initial record state
            onNodeWithTag("DeleteAudioRecordButton").performClick()
            onNodeWithTag("StartAudioRecordButton").assertIsDisplayed()

            // Verify close button triggers dismissal
            onNodeWithTag("CloseAudioMemoButton").performClick()
            assertTrue(dismissed)
        }
}
