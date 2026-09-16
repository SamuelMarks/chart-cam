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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.media.AudioRecorderManager
import io.healthplatform.chartcam.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * UI Component for recording and previewing clinical voice memos.
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
                    text = "Clinical Voice Memo",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("CloseAudioMemoButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Audio Memo",
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
                            contentDescription = "Recording duration: $timeFormatted"
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
                                val result = recorder.startRecording()
                                if (result.isSuccess) {
                                    isRecording = true
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("StartAudioRecordButton")
                                .semantics {
                                    contentDescription = "Start Voice Memo Recording"
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text("Record")
                    }
                } else if (isRecording) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val fileName = "voice_memo_${Clock.System.now().toEpochMilliseconds()}.m4a"
                                val result = recorder.stopRecording(fileName)
                                isRecording = false
                                if (result.isSuccess) {
                                    val path = result.getOrNull()
                                    recordedFilePath = path
                                    if (path != null) {
                                        onMemoRecorded(path)
                                    }
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("StopAudioRecordButton")
                                .semantics {
                                    contentDescription = "Stop Voice Memo Recording"
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text("Stop & Save")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        Button(
                            onClick = {
                                isPlayingPreview = !isPlayingPreview
                            },
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("PlayAudioPreviewButton")
                                    .semantics {
                                        contentDescription = if (isPlayingPreview) "Pause Preview" else "Play Preview"
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(if (isPlayingPreview) "Pause" else "Play")
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
                                        contentDescription = "Delete and Redo Recording"
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text("Redo")
                        }
                    }
                }
            }
        }
    }
}
