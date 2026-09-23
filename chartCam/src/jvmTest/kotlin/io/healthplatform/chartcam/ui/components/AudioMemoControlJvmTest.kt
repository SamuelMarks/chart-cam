/**
 * @file AudioMemoControlJvmTest.kt
 * Contains declarations for AudioMemoControlJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.semantics.semantics
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
                    modifier = androidx.compose.ui.Modifier,
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

    /**
     * Tests recording failure handling and duration advancement in AudioMemoControl.
     */
    @Test
    fun testAudioMemoControlFailureBranches() =
        runComposeUiTest {
            setAppLanguage("en")
            val failingRecorder =
                object : io.healthplatform.chartcam.media.AudioRecorderManager {
                    var failStart = true
                    var failStop = true
                    private val _isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
                    private val _amplitude = kotlinx.coroutines.flow.MutableStateFlow(0f)

                    override val isRecording: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRecording
                    override val amplitude: kotlinx.coroutines.flow.StateFlow<Float> = _amplitude

                    override suspend fun startRecording(): Result<Unit> =
                        if (failStart) {
                            Result.failure(IllegalStateException("Simulated start failure"))
                        } else {
                            _isRecording.value = true
                            Result.success(Unit)
                        }

                    override suspend fun pauseRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun resumeRecording(): Result<Unit> = Result.success(Unit)

                    override suspend fun stopRecording(fileName: String): Result<String> =
                        if (failStop) {
                            Result.failure(IllegalStateException("Simulated stop failure"))
                        } else {
                            _isRecording.value = false
                            Result.success("recorded.m4a")
                        }

                    override suspend fun cancelRecording(): Result<Unit> = Result.success(Unit)
                }

            setContent {
                AudioMemoControl(
                    recorder = failingRecorder,
                    onMemoRecorded = {},
                    onDismiss = {},
                )
            }

            // Click start recording when start fails
            onNodeWithTag("StartAudioRecordButton").performClick()
            waitForIdle()
            // Still in start state because it failed
            onNodeWithTag("StartAudioRecordButton").assertIsDisplayed()

            // Allow start to succeed but stop to fail
            failingRecorder.failStart = false
            onNodeWithTag("StartAudioRecordButton").performClick()
            waitForIdle()
            onNodeWithTag("StopAudioRecordButton").assertIsDisplayed()

            // Wait briefly to allow the duration timer to tick (durationSeconds++)
            mainClock.advanceTimeBy(1500L)
            waitForIdle()

            // Click stop recording when stop fails
            onNodeWithTag("StopAudioRecordButton").performClick()
            waitForIdle()
            // After stop failure, isRecording becomes false
            onNodeWithTag("StartAudioRecordButton").assertIsDisplayed()
        }

    /**
     * Tests recomposition and skipping for [AudioMemoControl].
     */
    @Test
    fun testAudioMemoControlRecompositionAndSkipping() =
        runComposeUiTest {
            val storage = createFileStorage()
            val recorder1 = DefaultAudioRecorderManager(storage)
            val recorder2 = DefaultAudioRecorderManager(storage)
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val recorderState = androidx.compose.runtime.mutableStateOf<io.healthplatform.chartcam.media.AudioRecorderManager>(recorder1)
            val onMemoRecordedState = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({ _ -> })
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val modifierState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            setContent {
                val dummy = outerTrigger.value
                AudioMemoControl(
                    recorder = recorderState.value,
                    onMemoRecorded = onMemoRecordedState.value,
                    onDismiss = onDismissState.value,
                    modifier = modifierState.value,
                )
            }
            waitForIdle()

            // Test recomposition skipping
            outerTrigger.value++
            waitForIdle()

            // Mutate each parameter
            recorderState.value = recorder2
            waitForIdle()

            onMemoRecordedState.value = { println("recorded: $it") }
            waitForIdle()

            onDismissState.value = { println("dismissed") }
            waitForIdle()

            modifierState.value =
                androidx.compose.ui.Modifier
                    .semantics { }
            waitForIdle()
        }
}
