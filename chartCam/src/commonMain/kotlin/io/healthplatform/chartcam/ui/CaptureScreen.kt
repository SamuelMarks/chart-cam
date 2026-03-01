package io.healthplatform.chartcam.ui

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*


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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.camera.rememberCameraManager
import io.healthplatform.chartcam.camera.rememberPermissionManager
import io.healthplatform.chartcam.camera.PermissionStatus
import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sensors.rememberSensorManager
import io.healthplatform.chartcam.ui.components.LevelerOverlay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.google.fhir.model.r4.Questionnaire
import kotlinx.coroutines.launch

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CaptureScreen(
    questionnaireId: String,
    questionnaireRepository: QuestionnaireRepository,
    onFinished: (Map<String, String>) -> Unit,
    onCancel: () -> Unit = {}
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.camera_permission_required), color = Color.White, modifier = Modifier.padding(16.dp))
                Button(onClick = { 
                    permissionManager.openSettings()
                }) {
                    Text(stringResource(Res.string.open_settings))
                }
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
        return
    }

    val cameraManager = rememberCameraManager()
    val sensorManager = rememberSensorManager()
    val fileStorage = remember { createFileStorage() }
    val viewModel = remember { CaptureViewModel(cameraManager, fileStorage) }

    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val q = questionnaireRepository.getQuestionnaire(questionnaireId)
        val steps = q?.item?.filter { it.type.value == Questionnaire.QuestionnaireItemType.Attachment }?.map {
            PhotoStep(it.linkId.value ?: "", it.text?.value ?: "")
        } ?: emptyList()
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
            val output = currentResults.mapKeys { it.key.id }
            onFinished(output)
        } else {
            onCancel()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
        .focusRequester(focusRequester)
        .onKeyEvent {
            if (it.key == Key.Escape) {
                handleCancel()
                true
            } else {
                false
            }
        }
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraManager = cameraManager
        )

        if (state.reviewImageBytes == null && !state.isCapturing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        viewModel.onCapture()
                    }
            )
        }

        LevelerOverlay(sensorManager)

        if (state.reviewImageBytes != null) {
            ReviewLayer(
                bytes = state.reviewImageBytes!!,
                onRetake = { viewModel.onRetake() },
                onConfirm = { viewModel.onConfirm() }
            )
        } else {
            val title = state.currentStep?.title ?: ""
            ControlsLayer(
                stepName = title,
                count = state.capturedCount,
                total = state.totalSteps,
                isCapturing = state.isCapturing,
                onCapture = { viewModel.onCapture() },
                onToggleLens = { cameraManager.toggleLens() },
                onCancel = handleCancel,
                hasMultipleCameras = cameraManager.hasMultipleCameras
            )
        }
    }
}

@Composable
fun ControlsLayer(
    stepName: String,
    count: Int,
    total: Int,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onToggleLens: () -> Unit,
    onCancel: () -> Unit,
    hasMultipleCameras: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stepName, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.step_count_format, count.toString(), total.toString()), color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Button(
                onClick = onCapture,
                modifier = Modifier.padding(bottom = 16.dp),
                enabled = !isCapturing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(Res.string.take_photo))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                
                if (hasMultipleCameras) {
                    IconButton(
                        onClick = onToggleLens, 
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) { 
                        Icon(
                            imageVector = Icons.Default.Cameraswitch, 
                            contentDescription = stringResource(Res.string.cd_switch_camera), 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewLayer(
    bytes: ByteArray,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    val bitmap = remember(bytes) { bytes.decodeToImageBitmap() }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(Res.string.cd_review),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(32.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            ) {
                Text(stringResource(Res.string.retake))
            }
            
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(stringResource(Res.string.confirm))
            }
        }
    }
}