/**
 * @file CaptureScreen.kt
 * Contains declarations for CaptureScreen.kt.
 *
 * UI components for capturing photos.
 * Provides the main screen and overlays for the camera feature.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.camera_permission_required
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.capturing_photo
import chartcam.chartcam.generated.resources.cd_camera_preview
import chartcam.chartcam.generated.resources.cd_photo_captured_review
import chartcam.chartcam.generated.resources.cd_review
import chartcam.chartcam.generated.resources.cd_switch_camera
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.confirm
import chartcam.chartcam.generated.resources.discard_capture_message
import chartcam.chartcam.generated.resources.discard_capture_title
import chartcam.chartcam.generated.resources.error_camera_capture_failed
import chartcam.chartcam.generated.resources.error_capture_empty_image
import chartcam.chartcam.generated.resources.error_capture_save_failed
import chartcam.chartcam.generated.resources.open_settings
import chartcam.chartcam.generated.resources.retake
import chartcam.chartcam.generated.resources.step_count_format
import chartcam.chartcam.generated.resources.take_photo
import chartcam.chartcam.generated.resources.unknown_error
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.PermissionStatus
import io.healthplatform.chartcam.camera.rememberCameraManager
import io.healthplatform.chartcam.camera.rememberPermissionManager
import io.healthplatform.chartcam.capture.CaptureError
import io.healthplatform.chartcam.capture.CaptureUiState
import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.fhir.getLocalizedText
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sensors.SensorManager
import io.healthplatform.chartcam.sensors.rememberSensorManager
import io.healthplatform.chartcam.ui.components.LevelerOverlay
import io.healthplatform.chartcam.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/**
 * Extracts a flattened list of [PhotoStep]s from a list of Questionnaire items, localized to the given language.
 *
 * @param items The items to extract steps from.
 * @param language The BCP-47 language tag for localization.
 * @return The list of extracted photo steps.
 */
internal fun extractSteps(
    items: List<Questionnaire.Item>,
    language: String = currentLanguageState.value,
): List<PhotoStep> {
    val result = mutableListOf<PhotoStep>()
    for (item in items) {
        if (item.type.value == Questionnaire.QuestionnaireItemType.Attachment) {
            result.add(PhotoStep(item.linkId.value ?: "", item.getLocalizedText(language)))
        }
        if (item.item.isNotEmpty()) {
            result.addAll(extractSteps(item.item, language))
        }
    }
    return result
}

/**
 * Internal helper function.
 * @param onOpenSettings The onOpenSettings.
 * @param onCancel The onCancel.
 */
@Composable
private fun PermissionDeniedScreen(
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .statusBarsPadding()
                .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(AppSpacing.lg),
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.camera_permission_required),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .padding(bottom = AppSpacing.md)
                            .semantics { heading() },
                )
                Button(onClick = onOpenSettings) {
                    Text(stringResource(Res.string.open_settings))
                }
                Button(
                    onClick = onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    modifier = Modifier.padding(top = AppSpacing.sm),
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

/**
 * State for the ControlsLayer composable.
 *
 * @property stepName The name or title of the current capture step.
 * @property count The number of photos already captured.
 * @property total The total number of photos needed.
 * @property isCapturing True if the camera is currently in the process of taking a picture.
 * @property hasMultipleCameras True if the device has multiple cameras.
 * @property isVideoMode True if currently in video recording mode.
 * @property isRecordingVideo True if video is actively recording.
 * @property videoDurationFormatted Formatted string of current video recording duration.
 */
data class ControlsState(
    val stepName: String,
    val count: Int,
    val total: Int,
    val isCapturing: Boolean,
    val hasMultipleCameras: Boolean = true,
    val isVideoMode: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val videoDurationFormatted: String = "",
)

/**
 * Actions for the CaptureBox composable.
 *
 * @property onCapture Callback triggered when the capture button is clicked.
 * @property onRetake Callback triggered when the user rejects the photo.
 * @property onConfirm Callback triggered when the user accepts the photo.
 * @property onCancel Callback triggered when the cancel button is clicked.
 * @property onDismissError Callback triggered when dismissing an error banner.
 * @property onToggleVideoMode Callback triggered when switching between photo and video modes.
 */
private data class CaptureActions(
    val onCapture: () -> Unit,
    val onRetake: () -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
    val onDismissError: () -> Unit = {},
    val onToggleVideoMode: () -> Unit = {},
)

/**
 * Main Composable for the photo capture workflow.
 * Handles permissions, camera preview, leveler, capturing, and reviewing photos.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state.
 *
 * @param questionnaireId The ID of the questionnaire defining the photo steps.
 * @param linkId An optional linkId within the questionnaire to target a specific capture sequence.
 * @param questionnaireRepository Repository to fetch the questionnaire details.
 * @param onFinished Callback invoked when photos are captured, mapping linkIds to file paths.
 * @param onCancel Callback invoked if the user cancels the capture process.
 */
@Composable
fun CaptureScreen(
    questionnaireId: String,
    linkId: String? = null,
    questionnaireRepository: QuestionnaireRepository,
    onFinished: (Map<String, String>) -> Unit,
    onCancel: () -> Unit = {},
) {
    val currentLang by currentLanguageState.collectAsState()

    key(currentLang) {
        val permissionManager = rememberPermissionManager()
        var permissionGranted by remember {
            mutableStateOf(permissionManager.getCameraPermissionStatus() == PermissionStatus.GRANTED)
        }

        LaunchedEffect(Unit) {
            if (!permissionGranted) {
                permissionGranted = permissionManager.requestCameraPermission().isSuccess
            }
        }

        if (!permissionGranted) {
            PermissionDeniedScreen(
                onOpenSettings = { permissionManager.openSettings() },
                onCancel = onCancel,
            )
        } else {
            CaptureScreenContent(
                questionnaireId = questionnaireId,
                linkId = linkId,
                questionnaireRepository = questionnaireRepository,
                onFinished = onFinished,
                onCancel = onCancel,
            )
        }
    }
}

/**
 * Internal helper function.
 * @param questionnaireId The questionnaireId.
 * @param linkId The linkId.
 * @param questionnaireRepository The questionnaireRepository.
 * @param onFinished The onFinished.
 * @param onCancel The onCancel.
 */
@Composable
private fun CaptureScreenContent(
    questionnaireId: String,
    linkId: String? = null,
    questionnaireRepository: QuestionnaireRepository,
    onFinished: (Map<String, String>) -> Unit,
    onCancel: () -> Unit = {},
) {
    val cameraManager = rememberCameraManager()
    val sensorManager = rememberSensorManager()
    val fileStorage = remember { createFileStorage() }
    val viewModel = remember { CaptureViewModel(cameraManager, fileStorage) }

    val currentLang by currentLanguageState.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val q = questionnaireRepository.getQuestionnaire(questionnaireId)
        val allSteps = q?.item?.let { extractSteps(it, currentLang) } ?: emptyList()
        val steps = if (linkId != null) allSteps.filter { it.id == linkId } else allSteps
        viewModel.initSteps(steps)
    }

    if (state.isFinished) {
        val output = viewModel.getResultPaths().mapKeys { it.key.id }
        onFinished(output)
        return
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleCancel = {
        val currentResults = viewModel.getResultPaths()
        if (currentResults.isNotEmpty()) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(Res.string.discard_capture_title)) },
            text = { Text(stringResource(Res.string.discard_capture_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardPendingPhotos()
                        onCancel()
                    },
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    var isVideoMode by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoDurationSeconds by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoDurationSeconds = 0
            while (isActive && isRecordingVideo) {
                delay(1000L)
                videoDurationSeconds++
            }
        }
    }

    val minutes = videoDurationSeconds / 60
    val seconds = videoDurationSeconds % 60
    val videoDurationFormatted =
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val handleCapture: () -> Unit = {
        if (!isVideoMode) {
            viewModel.onCapture()
        } else {
            if (!isRecordingVideo) {
                coroutineScope.launch {
                    val result = cameraManager.startVideoRecording()
                    if (result.isSuccess) {
                        isRecordingVideo = true
                    }
                }
            } else {
                coroutineScope.launch {
                    val result = cameraManager.stopVideoRecording()
                    isRecordingVideo = false
                    if (result.isSuccess) {
                        val fileName = "video_${io.healthplatform.chartcam.utils.UUID.randomUUID()}.mp4"
                        val path = fileStorage.saveImage(fileName, result.getOrNull()!!)
                        viewModel.onVideoRecorded(path)
                    }
                }
            }
        }
    }

    val actions =
        remember(viewModel, isVideoMode, isRecordingVideo) {
            CaptureActions(
                onCapture = handleCapture,
                onRetake = { viewModel.onRetake() },
                onConfirm = { viewModel.onConfirm() },
                onCancel = handleCancel,
                onDismissError = { viewModel.clearError() },
                onToggleVideoMode = {
                    if (!isRecordingVideo) {
                        isVideoMode = !isVideoMode
                    }
                },
            )
        }

    CaptureBox(
        state = state,
        cameraManager = cameraManager,
        sensorManager = sensorManager,
        focusRequester = focusRequester,
        actions = actions,
        isVideoMode = isVideoMode,
        isRecordingVideo = isRecordingVideo,
        videoDurationFormatted = videoDurationFormatted,
    )
}

/**
 * Internal helper function.
 * @param state The state.
 * @param cameraManager The cameraManager.
 * @param sensorManager The sensorManager.
 * @param focusRequester The focusRequester.
 * @param actions The actions.
 * @param isVideoMode Whether in video recording mode.
 * @param isRecordingVideo Whether video is actively recording.
 * @param videoDurationFormatted Formatted video timer string.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun CaptureBox(
    state: CaptureUiState,
    cameraManager: CameraManager,
    sensorManager: SensorManager,
    focusRequester: FocusRequester,
    actions: CaptureActions,
    isVideoMode: Boolean = false,
    isRecordingVideo: Boolean = false,
    videoDurationFormatted: String = "",
) {
    val cdCameraPreview = stringResource(Res.string.cd_camera_preview)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .focusRequester(focusRequester)
                .onKeyEvent {
                    if (it.key == Key.Escape) {
                        actions.onCancel()
                        true
                    } else {
                        false
                    }
                },
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = cdCameraPreview },
            cameraManager = cameraManager,
        )

        LevelerOverlay(sensorManager)

        val localizedErrorText =
            when (val err = state.error) {
                is CaptureError.EmptyImage -> stringResource(Res.string.error_capture_empty_image)
                is CaptureError.SaveFailed -> stringResource(Res.string.error_capture_save_failed)
                is CaptureError.CameraFailed -> stringResource(Res.string.error_camera_capture_failed, err.detail)
                null ->
                    state.errorMessageResource?.let { stringResource(it) }
                        ?: state.errorMessage?.let { stringResource(Res.string.unknown_error) }
            }

        if (localizedErrorText != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(AppSpacing.md)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = localizedErrorText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = actions.onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.clear),
                        )
                    }
                }
            }
        }

        if (state.reviewImageBytes != null) {
            ReviewLayer(
                bytes = state.reviewImageBytes,
                onRetake = actions.onRetake,
                onConfirm = actions.onConfirm,
            )
        } else {
            val stepTitle =
                state.currentStep?.let { step ->
                    step.titleRes?.let { stringResource(it) } ?: step.title
                } ?: ""
            ControlsLayer(
                state =
                    ControlsState(
                        stepName = stepTitle,
                        count = state.capturedCount,
                        total = state.totalSteps,
                        isCapturing = state.isCapturing,
                        hasMultipleCameras = cameraManager.hasMultipleCameras,
                        isVideoMode = isVideoMode,
                        isRecordingVideo = isRecordingVideo,
                        videoDurationFormatted = videoDurationFormatted,
                    ),
                onCapture = actions.onCapture,
                onToggleLens = { cameraManager.toggleLens() },
                onCancel = actions.onCancel,
                onToggleVideoMode = actions.onToggleVideoMode,
            )
        }
    }
}

/**
 * Overlay layer displaying controls for camera capture.
 *
 * @param state The state containing step name, counts, and camera status.
 * @param onCapture Callback triggered when the capture button is clicked.
 * @param onToggleLens Callback triggered when the switch camera button is clicked.
 * @param onCancel Callback triggered when the cancel button is clicked.
 * @param onToggleVideoMode Callback triggered when switching between photo and video modes.
 */
@Composable
fun ControlsLayer(
    state: ControlsState,
    onCapture: () -> Unit,
    onToggleLens: () -> Unit,
    onCancel: () -> Unit,
    onToggleVideoMode: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(AppSpacing.md),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ControlsTopBar(state.stepName, state.count, state.total)
        ControlsBottomBar(
            state = state,
            onCapture = onCapture,
            onCancel = onCancel,
            onToggleLens = onToggleLens,
            onToggleVideoMode = onToggleVideoMode,
        )
    }
}

/**
 * Internal helper function.
 * @param stepName The stepName.
 * @param count The count.
 * @param total The total.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ControlsTopBar(
    stepName: String,
    count: Int,
    total: Int,
) {
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val formattedCount =
        io.healthplatform.chartcam.utils
            .formatLocalizedDecimal(count.toDouble(), currentLang, decimalPlaces = 0)
    val formattedTotal =
        io.healthplatform.chartcam.utils
            .formatLocalizedDecimal(total.toDouble(), currentLang, decimalPlaces = 0)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    MaterialTheme.shapes.large,
                ).padding(horizontal = AppSpacing.md, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stepName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .padding(end = AppSpacing.sm)
                    .semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.step_count_format, formattedCount, formattedTotal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Internal helper function.
 * @param state The controls state.
 * @param onCapture The onCapture.
 * @param onCancel The onCancel.
 * @param onToggleLens The onToggleLens.
 * @param onToggleVideoMode The onToggleVideoMode.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ControlsBottomBar(
    state: ControlsState,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleVideoMode: () -> Unit,
) {
    val capturingPhotoText = stringResource(Res.string.capturing_photo)
    val takePhotoText = stringResource(Res.string.take_photo)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isRecordingVideo) {
            Text(
                text = state.videoDurationFormatted,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(bottom = AppSpacing.sm)
                        .testTag("VideoDurationCounter")
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Video recording duration: ${state.videoDurationFormatted}"
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .minimumInteractiveComponentSize()
                    .padding(bottom = AppSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isCapturing) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = capturingPhotoText
                                liveRegion = LiveRegionMode.Polite
                            },
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                val buttonColor =
                    if (state.isVideoMode) {
                        if (state.isRecordingVideo) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                FloatingActionButton(
                    onClick = onCapture,
                    shape = CircleShape,
                    containerColor = buttonColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("CaptureShutterButton")
                            .semantics {
                                contentDescription =
                                    if (state.isVideoMode) {
                                        if (state.isRecordingVideo) "Stop Recording" else "Start Recording"
                                    } else {
                                        takePhotoText
                                    }
                            },
                ) {
                    val icon =
                        if (state.isVideoMode) {
                            if (state.isRecordingVideo) Icons.Default.Stop else Icons.Default.Videocam
                        } else {
                            Icons.Default.PhotoCamera
                        }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppSpacing.xl),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onCancel,
            ) {
                Text(stringResource(Res.string.cancel))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                IconButton(
                    onClick = onToggleVideoMode,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .testTag("ToggleVideoModeButton")
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(
                        imageVector = if (state.isVideoMode) Icons.Default.PhotoCamera else Icons.Default.Videocam,
                        contentDescription = if (state.isVideoMode) "Switch to Photo Mode" else "Switch to Video Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.hasMultipleCameras) {
                    IconButton(
                        onClick = onToggleLens,
                        modifier =
                            Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = stringResource(Res.string.cd_switch_camera),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overlay layer allowing the user to review a captured photo.
 *
 * @param bytes The image data of the captured photo.
 * @param onRetake Callback triggered when the user rejects the photo.
 * @param onConfirm Callback triggered when the user accepts the photo.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ReviewLayer(
    bytes: ByteArray,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
) {
    val bitmap = remember(bytes) { bytes.decodeToImageBitmap() }
    val reviewAnnouncement = stringResource(Res.string.cd_photo_captured_review)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = reviewAnnouncement
                },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(Res.string.cd_review),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppSpacing.xl),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            FilledTonalButton(
                onClick = onRetake,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.retake))
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.confirm))
            }
        }
    }
}
