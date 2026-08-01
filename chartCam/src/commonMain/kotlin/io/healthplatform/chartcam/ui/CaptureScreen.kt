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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.camera_permission_required
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_camera_preview
import chartcam.chartcam.generated.resources.cd_review
import chartcam.chartcam.generated.resources.cd_switch_camera
import chartcam.chartcam.generated.resources.confirm
import chartcam.chartcam.generated.resources.open_settings
import chartcam.chartcam.generated.resources.retake
import chartcam.chartcam.generated.resources.step_count_format
import chartcam.chartcam.generated.resources.take_photo
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.PermissionStatus
import io.healthplatform.chartcam.camera.rememberCameraManager
import io.healthplatform.chartcam.camera.rememberPermissionManager
import io.healthplatform.chartcam.capture.CaptureUiState
import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sensors.SensorManager
import io.healthplatform.chartcam.sensors.rememberSensorManager
import io.healthplatform.chartcam.ui.components.LevelerOverlay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

// Extracts a flattened list of [PhotoStep]s from a list of Questionnaire items.

/**
 * Internal helper function.
 * @param items The items.
 * @return The result.
 */
private fun extractSteps(items: List<Questionnaire.Item>): List<PhotoStep> {
    val result = mutableListOf<PhotoStep>()
    for (item in items) {
        if (item.type.value == Questionnaire.QuestionnaireItemType.Attachment) {
            result.add(PhotoStep(item.linkId.value ?: "", item.text?.value ?: ""))
        }
        if (item.item.isNotEmpty()) {
            result.addAll(extractSteps(item.item))
        }
    }
    return result
}

@Composable
/**
 * Internal helper function.
 * @param onOpenSettings The onOpenSettings.
 * @param onCancel The onCancel.
 */
private fun PermissionDeniedScreen(
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.camera_permission_required),
                color = Color.White,
                modifier = Modifier.padding(16.dp),
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
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(Res.string.cancel))
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
 */
data class ControlsState(
    val stepName: String,
    val count: Int,
    val total: Int,
    val isCapturing: Boolean,
    val hasMultipleCameras: Boolean = true,
)

/**
 * Actions for the CaptureBox composable.
 *
 * @property onCapture Callback triggered when the capture button is clicked.
 * @property onRetake Callback triggered when the user rejects the photo.
 * @property onConfirm Callback triggered when the user accepts the photo.
 * @property onCancel Callback triggered when the cancel button is clicked.
 */
private data class CaptureActions(
    val onCapture: () -> Unit,
    val onRetake: () -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
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
    val permissionManager = rememberPermissionManager()
    var permissionGranted by remember {
        mutableStateOf(permissionManager.getCameraPermissionStatus() == PermissionStatus.GRANTED)
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionGranted = permissionManager.requestCameraPermission()
        }
    }

    if (!permissionGranted) {
        PermissionDeniedScreen(
            onOpenSettings = { permissionManager.openSettings() },
            onCancel = onCancel,
        )
        return
    }

    CaptureScreenContent(
        questionnaireId = questionnaireId,
        linkId = linkId,
        questionnaireRepository = questionnaireRepository,
        onFinished = onFinished,
        onCancel = onCancel,
    )
}

@Composable
/**
 * Internal helper function.
 * @param questionnaireId The questionnaireId.
 * @param linkId The linkId.
 * @param questionnaireRepository The questionnaireRepository.
 * @param onFinished The onFinished.
 * @param onCancel The onCancel.
 */
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

    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val q = questionnaireRepository.getQuestionnaire(questionnaireId)
        val allSteps = q?.item?.let { extractSteps(it) } ?: emptyList()
        val steps = if (linkId != null) allSteps.filter { it.id == linkId } else allSteps
        viewModel.initSteps(steps)
    }

    if (state.isFinished) {
        val output = viewModel.getResultPaths().mapKeys { it.key.id }
        onFinished(output)
        return
    }

    val handleCancel = {
        val currentResults = viewModel.getResultPaths()
        if (currentResults.isNotEmpty()) {
            onFinished(currentResults.mapKeys { it.key.id })
        } else {
            onCancel()
        }
    }

    val actions =
        remember(viewModel) {
            CaptureActions(
                onCapture = { viewModel.onCapture() },
                onRetake = { viewModel.onRetake() },
                onConfirm = { viewModel.onConfirm() },
                onCancel = handleCancel,
            )
        }

    CaptureBox(
        state = state,
        cameraManager = cameraManager,
        sensorManager = sensorManager,
        focusRequester = focusRequester,
        actions = actions,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
/**
 * Internal helper function.
 * @param state The state.
 * @param cameraManager The cameraManager.
 * @param sensorManager The sensorManager.
 * @param focusRequester The focusRequester.
 * @param actions The actions.
 */
private fun CaptureBox(
    state: CaptureUiState,
    cameraManager: CameraManager,
    sensorManager: SensorManager,
    focusRequester: FocusRequester,
    actions: CaptureActions,
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
        val interactionSource = remember { MutableInteractionSource() }

        CameraPreview(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = cdCameraPreview },
            cameraManager = cameraManager,
        )

        if (state.reviewImageBytes == null && !state.isCapturing) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                        ) { actions.onCapture() },
            )
        }

        LevelerOverlay(sensorManager)

        if (state.reviewImageBytes != null) {
            ReviewLayer(
                bytes = state.reviewImageBytes,
                onRetake = actions.onRetake,
                onConfirm = actions.onConfirm,
            )
        } else {
            ControlsLayer(
                state =
                    ControlsState(
                        stepName = state.currentStep?.title ?: "",
                        count = state.capturedCount,
                        total = state.totalSteps,
                        isCapturing = state.isCapturing,
                        hasMultipleCameras = cameraManager.hasMultipleCameras,
                    ),
                onCapture = actions.onCapture,
                onToggleLens = { cameraManager.toggleLens() },
                onCancel = actions.onCancel,
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
 */
@Composable
fun ControlsLayer(
    state: ControlsState,
    onCapture: () -> Unit,
    onToggleLens: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ControlsTopBar(state.stepName, state.count, state.total)
        ControlsBottomBar(
            isCapturing = state.isCapturing,
            hasMultipleCameras = state.hasMultipleCameras,
            onCapture = onCapture,
            onCancel = onCancel,
            onToggleLens = onToggleLens,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
/**
 * Internal helper function.
 * @param stepName The stepName.
 * @param count The count.
 * @param total The total.
 */
private fun ControlsTopBar(
    stepName: String,
    count: Int,
    total: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stepName, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(Res.string.step_count_format, count.toString(), total.toString()),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
/**
 * Internal helper function.
 * @param isCapturing The isCapturing.
 * @param hasMultipleCameras The hasMultipleCameras.
 * @param onCapture The onCapture.
 * @param onCancel The onCancel.
 * @param onToggleLens The onToggleLens.
 */
private fun ControlsBottomBar(
    isCapturing: Boolean,
    hasMultipleCameras: Boolean,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    onToggleLens: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Button(
            onClick = onCapture,
            modifier = Modifier.padding(bottom = 16.dp),
            enabled = !isCapturing,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(Res.string.take_photo))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onCancel,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Text(stringResource(Res.string.cancel))
            }

            if (hasMultipleCameras) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(Res.string.cd_review),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Button(
                onClick = onRetake,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(Res.string.retake))
            }

            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(Res.string.confirm))
            }
        }
    }
}
