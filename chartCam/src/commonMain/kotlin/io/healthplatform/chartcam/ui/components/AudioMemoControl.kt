/**
 * @file AudioMemoControl.kt
 * Contains declarations for AudioMemoControl.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.action_pause
import chartcam.chartcam.generated.resources.action_play
import chartcam.chartcam.generated.resources.action_record
import chartcam.chartcam.generated.resources.action_redo
import chartcam.chartcam.generated.resources.action_stop_and_save
import chartcam.chartcam.generated.resources.audio_recording_duration_format
import chartcam.chartcam.generated.resources.cd_close_audio_memo
import chartcam.chartcam.generated.resources.cd_pause_audio_preview
import chartcam.chartcam.generated.resources.cd_play_audio_preview
import chartcam.chartcam.generated.resources.cd_redo_voice_recording
import chartcam.chartcam.generated.resources.cd_start_voice_recording
import chartcam.chartcam.generated.resources.cd_stop_voice_recording
import chartcam.chartcam.generated.resources.title_clinical_voice_memo
import io.healthplatform.chartcam.media.AudioRecorderManager
import io.healthplatform.chartcam.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * UI Component for recording and previewing clinical voice memos.
 *
 * **State & Side Effects:**
 * Interacts with [AudioRecorderManager] via coroutines, managing internal recording state
 * and duration timer side-effects.
 *
 * @param recorder The audio recorder manager.
 * @param onMemoRecorded Callback invoked with the recorded file path when complete.
 * @param onDismiss Callback invoked when dismissing or canceling the recorder.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun AudioMemoControl(
    recorder: AudioRecorderManager,
    onMemoRecorded: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRecording by remember { mutableStateOf(false) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRecording) {
        if (isRecording) {
            durationSeconds = 0
            while (isActive && isRecording) {
                delay(1000L)
                durationSeconds++
            }
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted =
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val durationA11yText = stringResource(Res.string.audio_recording_duration_format, timeFormatted)
    val startVoiceCd = stringResource(Res.string.cd_start_voice_recording)
    val stopVoiceCd = stringResource(Res.string.cd_stop_voice_recording)
    val redoVoiceCd = stringResource(Res.string.cd_redo_voice_recording)

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm)
                .testTag("AudioMemoControlCard"),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.title_clinical_voice_memo),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("CloseAudioMemoButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cd_close_audio_memo),
                    )
                }
            }

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.headlineMedium,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("AudioDurationCounter")
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = durationA11yText
                        },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isRecording && recordedFilePath == null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                recorder.startRecording().onSuccess {
                                    isRecording = true
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("StartAudioRecordButton")
                                .semantics {
                                    contentDescription = startVoiceCd
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(stringResource(Res.string.action_record))
                    }
                } else if (isRecording) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val fileName = "voice_memo_${Clock.System.now().toEpochMilliseconds()}.m4a"
                                recorder
                                    .stopRecording(fileName)
                                    .onSuccess { path ->
                                        isRecording = false
                                        recordedFilePath = path
                                        onMemoRecorded(path)
                                    }.onFailure {
                                        isRecording = false
                                    }
                            }
                        },
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("StopAudioRecordButton")
                                .semantics {
                                    contentDescription = stopVoiceCd
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(stringResource(Res.string.action_stop_and_save))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        val previewActionText =
                            stringResource(
                                if (isPlayingPreview) Res.string.action_pause else Res.string.action_play,
                            )
                        val previewActionCd =
                            stringResource(
                                if (isPlayingPreview) {
                                    Res.string.cd_pause_audio_preview
                                } else {
                                    Res.string.cd_play_audio_preview
                                },
                            )
                        Button(
                            onClick = {
                                isPlayingPreview = !isPlayingPreview
                            },
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("PlayAudioPreviewButton")
                                    .semantics {
                                        contentDescription = previewActionCd
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(previewActionText)
                        }

                        OutlinedButton(
                            onClick = {
                                isPlayingPreview = false
                                recordedFilePath = null
                                durationSeconds = 0
                            },
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("DeleteAudioRecordButton")
                                    .semantics {
                                        contentDescription = redoVoiceCd
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(stringResource(Res.string.action_redo))
                        }
                    }
                }
            }
        }
    }
}
