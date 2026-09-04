/**
 * @file EncounterDetailScreen.kt
 * Contains declarations for EncounterDetailScreen.kt.
 *
 * Screen displaying the details of a patient's encounter (visit).
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.captured_photos_format
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_more_options
import chartcam.chartcam.generated.resources.cd_patient_photo
import chartcam.chartcam.generated.resources.cd_questionnaire_selector
import chartcam.chartcam.generated.resources.create_new
import chartcam.chartcam.generated.resources.delete_visit
import chartcam.chartcam.generated.resources.delete_visit_message
import chartcam.chartcam.generated.resources.delete_visit_title
import chartcam.chartcam.generated.resources.edit_visit
import chartcam.chartcam.generated.resources.finalize_visit
import chartcam.chartcam.generated.resources.image_load_error
import chartcam.chartcam.generated.resources.mrn_date_format
import chartcam.chartcam.generated.resources.no
import chartcam.chartcam.generated.resources.no_notes
import chartcam.chartcam.generated.resources.provider_format
import chartcam.chartcam.generated.resources.questionnaire
import chartcam.chartcam.generated.resources.questionnaire_format
import chartcam.chartcam.generated.resources.select_questionnaire
import chartcam.chartcam.generated.resources.syncing_to_server
import chartcam.chartcam.generated.resources.take_photos
import chartcam.chartcam.generated.resources.unknown
import chartcam.chartcam.generated.resources.visit_detail
import chartcam.chartcam.generated.resources.yes
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.models.encounterDate
import io.healthplatform.chartcam.models.fullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.viewmodel.EncounterUiState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/**
 * Dependencies for the EncounterDetailScreen.
 *
 * @property photoSessionManager The photo session manager.
 * @property fhirRepository The FHIR repository.
 * @property authRepository The authentication repository.
 * @property questionnaireRepository The questionnaire repository.
 */
data class EncounterDetailDependencies(
    val photoSessionManager: PhotoSessionManager,
    val fhirRepository: FhirRepository,
    val authRepository: AuthRepository,
    val questionnaireRepository: QuestionnaireRepository,
)

/**
 * Actions for the EncounterDetailScreen.
 *
 * @property onBack Callback to navigate back.
 * @property onTakePhotos Callback to launch camera for photos.
 * @property onCreateNewQuestionnaire Callback to navigate to questionnaire builder.
 * @property onFinalized Callback when the encounter is finalized.
 * @property onVisitCreated Callback when a new visit is created.
 * @property onNewlyCreatedQuestionnaireHandled Callback when a new questionnaire has been handled.
 */
data class EncounterDetailActions(
    val onBack: () -> Unit,
    val onTakePhotos: (String?, String?) -> Unit,
    val onCreateNewQuestionnaire: () -> Unit = {},
    val onFinalized: () -> Unit,
    val onVisitCreated: ((String) -> Unit)? = null,
    val onNewlyCreatedQuestionnaireHandled: () -> Unit = {},
)

/**
 * Encapsulates the UI for detailing an encounter.
 *
 * @param patientId ID of the patient.
 * @param visitId ID of the visit.
 * @param dependencies Dependencies required for the screen.
 * @param actions Actions triggered from the screen.
 * @param newlyCreatedQuestionnaireId Optional ID of a newly created questionnaire.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun EncounterDetailScreen(
    patientId: String,
    visitId: String,
    dependencies: EncounterDetailDependencies,
    actions: EncounterDetailActions,
    newlyCreatedQuestionnaireId: String? = null,
) {
    val viewModel =
        androidx.lifecycle.viewmodel.compose.viewModel {
            EncounterDetailViewModel(
                dependencies.fhirRepository,
                dependencies.authRepository,
                dependencies.questionnaireRepository,
            )
        }

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    EncounterEffects(
        params =
            EncounterEffectParams(
                patientId,
                visitId,
                newlyCreatedQuestionnaireId,
                dependencies.photoSessionManager,
            ),
        actions = actions,
        viewModel = viewModel,
        state = state,
    )

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteEncounter { actions.onBack() }
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { EncounterTopBar(state, actions, viewModel) { showDeleteConfirmDialog = true } },
    ) { padding ->
        EncounterDetailContent(state, padding, actions, viewModel)
    }
}

/**
 * Internal helper.
 */
private data class EncounterEffectParams(
    val patientId: String,
    val visitId: String,
    val newQId: String?,
    val photoManager: PhotoSessionManager,
)

/**
 * Internal helper.
 * @param params The params.
 * @param actions The actions.
 * @param viewModel The viewModel.
 * @param state The state.
 */
@Composable
private fun EncounterEffects(
    params: EncounterEffectParams,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
    state: EncounterUiState,
) {
    LaunchedEffect(params.newQId) {
        if (params.newQId != null) {
            viewModel.selectQuestionnaireById(params.newQId)
            actions.onNewlyCreatedQuestionnaireHandled()
        }
    }

    val pendingPhotos by params.photoManager.pendingPhotos.collectAsState()

    LaunchedEffect(params.patientId, params.visitId) {
        viewModel.initialize(params.patientId, params.visitId, params.photoManager.getAndClear())
    }

    LaunchedEffect(pendingPhotos) {
        if (pendingPhotos.isNotEmpty()) {
            viewModel.addPhotos(pendingPhotos)
            params.photoManager.getAndClear()
        }
    }

    LaunchedEffect(state.encounter?.id) {
        val encounterId = state.encounter?.id
        if (params.visitId == "new" && encounterId != null) {
            actions.onVisitCreated?.invoke(encounterId)
        }
    }

    LaunchedEffect(state.isFinalized) {
        if (state.isFinalized) {
            actions.onFinalized()
            viewModel.resetFinalized()
        }
    }
}

/**
 * Internal helper.
 * @param onConfirm The onConfirm.
 * @param onDismiss The onDismiss.
 */
@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.delete_visit_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(Res.string.delete_visit_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.delete_visit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Internal helper.
 * @param state The state.
 * @param actions The actions.
 * @param viewModel The viewModel.
 * @param onDeleteRequest The onDeleteRequest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncounterTopBar(
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
    onDeleteRequest: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(Res.string.visit_detail),
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.cd_back),
                )
            }
        },
        actions = {
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more_options))
            }
            androidx.compose.material3.DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                val status = state.encounter?.status?.value
                val isFinished = status == com.google.fhir.model.r4.Encounter.EncounterStatus.Finished
                if (isFinished || state.isFinalized) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.edit_visit)) },
                        onClick = {
                            showMenu = false
                            viewModel.reopenEncounter()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete_visit)) },
                    onClick = {
                        showMenu = false
                        onDeleteRequest()
                    },
                )
            }
        },
    )
}

/**
 * Internal helper.
 * @param state The state.
 * @param padding The padding.
 * @param actions The actions.
 * @param viewModel The viewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncounterDetailContent(
    state: EncounterUiState,
    padding: PaddingValues,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
) {
    if (state.isLoading || state.isSyncing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                if (state.isSyncing) {
                    Text(
                        stringResource(Res.string.syncing_to_server),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding =
                PaddingValues(
                    top = 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                ),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EncounterDetailHeader(state, actions, viewModel)
            }

            items(state.photos) { photo ->
                PhotoGridItem(photo)
            }

            if (canFinalizeEncounter(state)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val yesStr = stringResource(Res.string.yes)
                    val noStr = stringResource(Res.string.no)
                    val noNotesStr = stringResource(Res.string.no_notes)
                    Button(
                        onClick = { viewModel.finalizeEncounter(yesStr, noStr, noNotesStr) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    ) {
                        Text(stringResource(Res.string.finalize_visit))
                    }
                }
            }
        }
    }
}

/**
 * Internal helper.
 * @param state The state.
 * @param actions The actions.
 * @param viewModel The viewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncounterDetailHeader(
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
) {
    Column {
        PatientAndPractitionerInfo(state)
        QuestionnaireSelector(state, actions, viewModel)
        QuestionnaireFormArea(state, actions, viewModel)
    }
}

/**
 * Internal helper.
 * @param state The state.
 */
@Composable
private fun PatientAndPractitionerInfo(state: EncounterUiState) {
    state.patient?.let { patient ->
        Text(
            text = patient.fullName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        val encDate = state.encounter?.encounterDate ?: ""
        Text(
            text = stringResource(Res.string.mrn_date_format, patient.mrn, encDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    state.practitioner?.let { prac ->
        Text(
            text = stringResource(Res.string.provider_format, prac.fullName),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

/**
 * Internal helper.
 * @param state The state.
 * @param actions The actions.
 * @param viewModel The viewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionnaireSelector(
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorCd = stringResource(Res.string.cd_questionnaire_selector)
    val status = state.encounter?.status?.value
    val isFinished = status == com.google.fhir.model.r4.Encounter.EncounterStatus.Finished
    val isLocked = state.answers.isNotEmpty() || isFinished || state.isFinalized

    if (isLocked) {
        Text(
            text =
                stringResource(
                    Res.string.questionnaire_format,
                    state.selectedQuestionnaire?.title?.value ?: "",
                ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = selectorCd },
        ) {
            OutlinedTextField(
                value =
                    state.selectedQuestionnaire?.title?.value
                        ?: stringResource(Res.string.select_questionnaire),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.questionnaire)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier =
                    Modifier
                        .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
            )
            QuestionnaireDropdownMenu(
                expanded = expanded,
                state = state,
                actions = actions,
                viewModel = viewModel,
                onDismiss = { expanded = false },
            )
        }
    }
}

/**
 * Internal helper.
 * @param state The state.
 * @param actions The actions.
 * @param viewModel The viewModel.
 */
@Composable
private fun QuestionnaireFormArea(
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
) {
    state.selectedQuestionnaire?.let { q ->
        io.healthplatform.chartcam.sdc.SdcQuestionnaireForm(
            questionnaire = q,
            answers = state.answers,
            config =
                io.healthplatform.chartcam.sdc
                    .SdcFormConfig(attachments = state.photos),
            onFormUpdated = { newAnswers, _ ->
                viewModel.onFormUpdated(newAnswers)
            },
            onTakePhotoRequested = { linkId -> actions.onTakePhotos(q.id, linkId) },
        )
    }

    val targetPhotosCount =
        state.selectedQuestionnaire?.item?.count {
            it.type.value == Questionnaire.QuestionnaireItemType.Attachment
        } ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.captured_photos_format, state.photos.size, targetPhotosCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(onClick = { actions.onTakePhotos(state.selectedQuestionnaire?.id, null) }) {
            Text(stringResource(Res.string.take_photos))
        }
    }
}

/**
 * Renders a single photo thumbnail mapped from a FHIR DocumentReference.
 *
 * @param doc The DocumentReference resource representing the photo.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun PhotoGridItem(doc: DocumentReference) {
    ElevatedCard(
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Column {
            val bytes =
                remember(
                    doc.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value ?: "",
                ) {
                    try {
                        val storage = createFileStorage()
                        storage.readImage(
                            doc.content
                                .firstOrNull()
                                ?.attachment
                                ?.url
                                ?.value ?: "",
                        )
                    } catch (ignored: IllegalArgumentException) {
                        ByteArray(0)
                    }
                }

            if (bytes.isNotEmpty()) {
                Image(
                    bitmap = bytes.decodeToImageBitmap(),
                    contentDescription =
                        doc.description?.value
                            ?: stringResource(Res.string.cd_patient_photo),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.image_load_error))
                }
            }

            Text(
                text = doc.description?.value ?: stringResource(Res.string.cd_patient_photo),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

/**
 * Internal helper.
 * @param state The state.
 * @return The result.
 */
private fun canFinalizeEncounter(state: EncounterUiState): Boolean =
    !state.isLoading &&
        !state.isSyncing &&
        !state.isFinalized &&
        state.encounter?.status?.value != com.google.fhir.model.r4.Encounter.EncounterStatus.Finished

/**
 * Internal helper.
 * @param expanded The expanded.
 * @param state The state.
 * @param actions The actions.
 * @param viewModel The viewModel.
 * @param onDismiss The onDismiss.
 */
@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
private fun androidx.compose.material3.ExposedDropdownMenuBoxScope.QuestionnaireDropdownMenu(
    expanded: Boolean,
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
    onDismiss: () -> Unit,
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        state.availableQuestionnaires.forEach { q ->
            DropdownMenuItem(
                text = { Text(q.title?.value ?: stringResource(Res.string.unknown)) },
                onClick = {
                    viewModel.selectQuestionnaire(q)
                    onDismiss()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.create_new)) },
            onClick = {
                onDismiss()
                actions.onCreateNewQuestionnaire()
            },
        )
    }
}
