/**
 * @file EncounterDetailScreen.kt
 * Contains declarations for EncounterDetailScreen.kt.
 *
 * Screen displaying the details of a patient's encounter (visit).
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.action_dicom_viewer
import chartcam.chartcam.generated.resources.action_voice_memo
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.captured_photos_format
import chartcam.chartcam.generated.resources.cd_action_view_photo
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_more_options
import chartcam.chartcam.generated.resources.cd_patient_photo
import chartcam.chartcam.generated.resources.cd_questionnaire_selector
import chartcam.chartcam.generated.resources.close
import chartcam.chartcam.generated.resources.create_new
import chartcam.chartcam.generated.resources.delete_visit
import chartcam.chartcam.generated.resources.delete_visit_message
import chartcam.chartcam.generated.resources.delete_visit_title
import chartcam.chartcam.generated.resources.edit_visit
import chartcam.chartcam.generated.resources.finalize_visit
import chartcam.chartcam.generated.resources.image_load_error
import chartcam.chartcam.generated.resources.loading
import chartcam.chartcam.generated.resources.mrn_date_format
import chartcam.chartcam.generated.resources.no
import chartcam.chartcam.generated.resources.no_notes
import chartcam.chartcam.generated.resources.provider_format
import chartcam.chartcam.generated.resources.questionnaire
import chartcam.chartcam.generated.resources.questionnaire_format
import chartcam.chartcam.generated.resources.recovered_form
import chartcam.chartcam.generated.resources.select_questionnaire
import chartcam.chartcam.generated.resources.syncing_to_server
import chartcam.chartcam.generated.resources.take_photos
import chartcam.chartcam.generated.resources.unknown
import chartcam.chartcam.generated.resources.visit_detail
import chartcam.chartcam.generated.resources.yes
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.fhir.getLocalizedTitle
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.media.createAudioRecorderManager
import io.healthplatform.chartcam.models.encounterDate
import io.healthplatform.chartcam.models.getFullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.ui.components.AudioMemoControl
import io.healthplatform.chartcam.ui.components.DemoModeBanner
import io.healthplatform.chartcam.ui.theme.AppSpacing
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
 * @property onOpenDicomViewer Optional callback to open a photo in the DICOM viewer.
 * @property onRecordAudioMemo Optional callback to trigger clinical voice memo recording.
 */
data class EncounterDetailActions(
    val onBack: () -> Unit,
    val onTakePhotos: (String?, String?) -> Unit,
    val onCreateNewQuestionnaire: () -> Unit = {},
    val onFinalized: () -> Unit,
    val onVisitCreated: ((String) -> Unit)? = null,
    val onNewlyCreatedQuestionnaireHandled: () -> Unit = {},
    val onOpenDicomViewer: ((String) -> Unit)? = null,
    val onRecordAudioMemo: (() -> Unit)? = null,
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
    val recoveredFormLabel = stringResource(Res.string.recovered_form)
    val viewModel =
        androidx.lifecycle.viewmodel.compose.viewModel {
            EncounterDetailViewModel(
                dependencies.fhirRepository,
                dependencies.authRepository,
                dependencies.questionnaireRepository,
                recoveredFormResolver = { recoveredFormLabel },
            )
        }

    val state by viewModel.uiState.collectAsState()
    val currentLang by currentLanguageState.collectAsState()
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

    key(currentLang) {
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
            val isDemoSession by dependencies.authRepository.isDemoSession.collectAsState()
            Column(modifier = Modifier.padding(padding)) {
                if (isDemoSession) {
                    DemoModeBanner(
                        onExitDemo = {
                            dependencies.authRepository.logout()
                            actions.onBack()
                        },
                    )
                }
                EncounterDetailContent(state, actions, viewModel)
            }
        }
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
                val isFinished = status == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished
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
 * @param actions The actions.
 * @param viewModel The viewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncounterDetailContent(
    state: EncounterUiState,
    actions: EncounterDetailActions,
    viewModel: EncounterDetailViewModel,
) {
    var showVoiceMemoDialog by remember { mutableStateOf(false) }

    val effectiveActions =
        remember(actions) {
            if (actions.onRecordAudioMemo != null) {
                actions
            } else {
                actions.copy(onRecordAudioMemo = { showVoiceMemoDialog = true })
            }
        }

    if (showVoiceMemoDialog) {
        val fileStorage = remember { createFileStorage() }
        val audioRecorder = remember { createAudioRecorderManager(fileStorage) }

        AlertDialog(
            onDismissRequest = { showVoiceMemoDialog = false },
            confirmButton = {},
            text = {
                AudioMemoControl(
                    recorder = audioRecorder,
                    onMemoRecorded = { path ->
                        showVoiceMemoDialog = false
                        viewModel.addVoiceMemo(path)
                    },
                    onDismiss = { showVoiceMemoDialog = false },
                )
            },
        )
    }

    if (state.isLoading || state.isSyncing) {
        val loadingText =
            if (state.isSyncing) {
                stringResource(Res.string.syncing_to_server)
            } else {
                stringResource(Res.string.loading)
            }
        Box(
            Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = loadingText
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingText },
                )
                if (state.isSyncing) {
                    Text(
                        stringResource(Res.string.syncing_to_server),
                        modifier = Modifier.padding(top = AppSpacing.md),
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            contentPadding = PaddingValues(vertical = AppSpacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EncounterDetailHeader(state, effectiveActions, viewModel)
            }

            items(state.photos) { photo ->
                PhotoGridItem(photo, actions.onOpenDicomViewer)
            }

            if (canFinalizeEncounter(state)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val yesStr = stringResource(Res.string.yes)
                    val noStr = stringResource(Res.string.no)
                    val noNotesStr = stringResource(Res.string.no_notes)
                    Button(
                        onClick = { viewModel.finalizeEncounter(yesStr, noStr, noNotesStr) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.md),
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
    val currentLang by currentLanguageState.collectAsState()
    state.patient?.let { patient ->
        Text(
            text = patient.getFullName(currentLang),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        val rawDate = state.encounter?.encounterDate ?: ""
        val encDate =
            if (rawDate.isNotEmpty()) {
                io.healthplatform.chartcam.utils
                    .formatLocalizedDate(rawDate, currentLang)
            } else {
                ""
            }
        Text(
            text = stringResource(Res.string.mrn_date_format, patient.mrn, encDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    state.practitioner?.let { prac ->
        Text(
            text = stringResource(Res.string.provider_format, prac.getFullName(currentLang)),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = AppSpacing.sm),
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
    val currentLang by currentLanguageState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val selectorCd = stringResource(Res.string.cd_questionnaire_selector)
    val status = state.encounter?.status?.value
    val isFinished = status == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished
    val isLocked = state.answers.isNotEmpty() || isFinished || state.isFinalized
    val qTitle =
        state.selectedQuestionnaire?.let { q ->
            q.getLocalizedTitle(currentLang).ifEmpty {
                q.title?.value
            }
        } ?: ""

    if (isLocked) {
        Text(
            text =
                stringResource(
                    Res.string.questionnaire_format,
                    qTitle,
                ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = AppSpacing.sm).semantics { heading() },
        )
    } else {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm)
                    .semantics { contentDescription = selectorCd },
        ) {
            OutlinedTextField(
                value = qTitle.ifEmpty { stringResource(Res.string.select_questionnaire) },
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

    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val formattedPhotosCount =
        io.healthplatform.chartcam.utils.formatLocalizedDecimal(
            state.photos.size.toDouble(),
            currentLang,
            decimalPlaces = 0,
        )
    val formattedTargetCount =
        io.healthplatform.chartcam.utils.formatLocalizedDecimal(
            targetPhotosCount.toDouble(),
            currentLang,
            decimalPlaces = 0,
        )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(
                Res.string.captured_photos_format,
                formattedPhotosCount,
                formattedTargetCount,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = AppSpacing.sm).semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Button(
                onClick = { actions.onRecordAudioMemo?.invoke() },
                modifier = Modifier.testTag("RecordVoiceMemoButton"),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(stringResource(Res.string.action_voice_memo))
            }
            Button(onClick = { actions.onTakePhotos(state.selectedQuestionnaire?.id, null) }) {
                Text(stringResource(Res.string.take_photos))
            }
        }
    }
}

/**
 * Renders a single photo thumbnail mapped from a FHIR DocumentReference.
 *
 * @param doc The DocumentReference resource representing the photo.
 * @param onOpenDicomViewer Optional callback to open the photo in the DICOM viewer.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun PhotoGridItem(
    doc: DocumentReference,
    onOpenDicomViewer: ((String) -> Unit)? = null,
) {
    var showFullPhoto by remember { mutableStateOf(false) }
    val photoDescription = doc.description?.value ?: stringResource(Res.string.cd_patient_photo)
    val viewPhotoLabel = stringResource(Res.string.cd_action_view_photo)
    val loadErrorText = stringResource(Res.string.image_load_error)
    val isAudio =
        doc.content
            .firstOrNull()
            ?.attachment
            ?.contentType
            ?.value
            ?.startsWith("audio/") == true

    val bytes =
        remember(
            doc.content
                .firstOrNull()
                ?.attachment
                ?.url
                ?.value ?: "",
        ) {
            runCatching {
                val storage = createFileStorage()
                storage.readImage(
                    doc.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value ?: "",
                )
            }.getOrDefault(ByteArray(0))
        }

    ElevatedCard(
        modifier =
            Modifier
                .minimumInteractiveComponentSize()
                .semantics(mergeDescendants = true) {
                    contentDescription = photoDescription
                }.clickable(
                    role = Role.Button,
                    onClickLabel = viewPhotoLabel,
                ) {
                    if (bytes.isNotEmpty()) {
                        showFullPhoto = true
                    }
                },
    ) {
        Column {
            if (isAudio) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = photoDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                }
            } else if (bytes.isNotEmpty()) {
                val bitmap = runCatching { bytes.decodeToImageBitmap() }.getOrNull()
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = loadErrorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier =
                                Modifier.semantics {
                                    error(loadErrorText)
                                },
                        )
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = loadErrorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier.semantics {
                                error(loadErrorText)
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }

            Text(
                text = photoDescription,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(AppSpacing.sm),
            )
        }
    }

    if (showFullPhoto && bytes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFullPhoto = false },
            title = {
                Text(
                    text = photoDescription,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Image(
                    bitmap = bytes.decodeToImageBitmap(),
                    contentDescription = photoDescription,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            dismissButton = {
                val filePath =
                    doc.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value
                if (onOpenDicomViewer != null && !filePath.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            showFullPhoto = false
                            onOpenDicomViewer(filePath)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text(stringResource(Res.string.action_dicom_viewer))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFullPhoto = false },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
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
        state.encounter?.status?.value != dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished

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
    val currentLang by currentLanguageState.collectAsState()
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        state.availableQuestionnaires.forEach { q ->
            val titleStr =
                q.getLocalizedTitle(currentLang).ifEmpty {
                    q.title?.value ?: stringResource(Res.string.unknown)
                }
            DropdownMenuItem(
                text = { Text(titleStr) },
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
