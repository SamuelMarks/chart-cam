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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
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
import chartcam.chartcam.generated.resources.provider_format
import chartcam.chartcam.generated.resources.questionnaire
import chartcam.chartcam.generated.resources.questionnaire_format
import chartcam.chartcam.generated.resources.select_questionnaire
import chartcam.chartcam.generated.resources.syncing_to_server
import chartcam.chartcam.generated.resources.take_photos
import chartcam.chartcam.generated.resources.title
import chartcam.chartcam.generated.resources.unknown
import chartcam.chartcam.generated.resources.visit_detail
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
import io.healthplatform.chartcam.sync.SyncWorker
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/**
 * Screen for viewing and managing details of a clinical encounter.
 * Displays patient details, practitioner, and an interactive questionnaire form.
 *
 * Note: The UI has been updated to use a standard "Finalize Visit" submit button
 * at the bottom of the scrollable form area, instead of a floating action button,
 * to better follow standard form submission UX patterns.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param patientId The unique identifier of the patient for this encounter.
 * @param visitId The unique identifier of the encounter. If "new", it will create a new encounter.
 * @param photoSessionManager Manager to retrieve captured photos.
 * @param fhirRepository Repository for FHIR operations.
 * @param authRepository Repository handling user authentication.
 * @param syncWorker Worker responsible for syncing with remote FHIR servers.
 * @param questionnaireRepository Repository supplying questionnaire forms.
 * @param newlyCreatedQuestionnaireId The ID of a newly created questionnaire to select automatically.
 * @param onBack Callback invoked when the user navigates back.
 * @param onTakePhotos Callback invoked when the user requests to take clinical photos. Passes the selected questionnaire ID.
 * @param onCreateNewQuestionnaire Callback invoked to navigate to the questionnaire builder.
 * @param onFinalized Callback invoked when the encounter is finalized.
 * @param onVisitCreated Optional callback invoked with the newly created encounter ID.
 * @param onNewlyCreatedQuestionnaireHandled Callback invoked after the newly created questionnaire has been handled.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun EncounterDetailScreen(
    patientId: String,
    visitId: String,
    photoSessionManager: PhotoSessionManager,
    fhirRepository: FhirRepository,
    authRepository: AuthRepository,
    syncWorker: SyncWorker,
    questionnaireRepository: QuestionnaireRepository,
    newlyCreatedQuestionnaireId: String? = null,
    onBack: () -> Unit,
    onTakePhotos: (String?, String?) -> Unit,
    onCreateNewQuestionnaire: () -> Unit = {},
    onFinalized: () -> Unit,
    onVisitCreated: ((String) -> Unit)? = null,
    onNewlyCreatedQuestionnaireHandled: () -> Unit = {},
) {
    /**
     * The view model handling business logic for the EncounterDetailScreen.
     */
    val viewModel =
        androidx.lifecycle.viewmodel.compose.viewModel {
            EncounterDetailViewModel(fhirRepository, authRepository, syncWorker, questionnaireRepository)
        }

    /**
     * State capturing the UI's data and interaction status.
     */
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(newlyCreatedQuestionnaireId) {
        if (newlyCreatedQuestionnaireId != null) {
            viewModel.selectQuestionnaireById(newlyCreatedQuestionnaireId)
            onNewlyCreatedQuestionnaireHandled()
        }
    }

    /**
     * State managing the display of snackbars.
     */
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * Observes pending photos from the camera flow.
     */
    val pendingPhotos by photoSessionManager.pendingPhotos.collectAsState()

    /**
     * Focus manager used to control form input traversal.
     */
    val focusManager = LocalFocusManager.current

    LaunchedEffect(patientId, visitId) {
        viewModel.initialize(patientId, visitId, photoSessionManager.getAndClear())
    }

    LaunchedEffect(pendingPhotos) {
        if (pendingPhotos.isNotEmpty()) {
            viewModel.addPhotos(pendingPhotos)
            photoSessionManager.getAndClear()
        }
    }

    LaunchedEffect(state.encounter?.id) {
        if (visitId == "new" && state.encounter?.id != null) {
            onVisitCreated?.invoke(state.encounter?.id!!)
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isFinalized) {
        if (state.isFinalized) {
            onFinalized()
            viewModel.resetFinalized()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(Res.string.delete_visit_title), modifier = Modifier.semantics { heading() }) },
            text = { Text(stringResource(Res.string.delete_visit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.deleteEncounter {
                        onBack()
                    }
                }) {
                    Text(stringResource(Res.string.delete_visit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.visit_detail), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
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
                        if (state.encounter?.status?.value == com.google.fhir.model.r4.Encounter.EncounterStatus.Finished ||
                            state.isFinalized
                        ) {
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
                                showDeleteConfirmDialog = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading || state.isSyncing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (state.isSyncing) {
                        Text(stringResource(Res.string.syncing_to_server), modifier = Modifier.padding(top = 16.dp))
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
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                    ),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        state.patient?.let { patient ->
                            Text(
                                text = patient.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = stringResource(Res.string.mrn_date_format, patient.mrn, state.encounter?.encounterDate ?: ""),
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

                        // Questionnaire Selection
                        var expanded by remember { mutableStateOf(false) }
                        val selectorCd = stringResource(Res.string.cd_questionnaire_selector)

                        /**
                         * Locks the questionnaire selector if there are already answers present,
                         * or if the encounter is already completed/finalized. This prevents users
                         * from changing the underlying questionnaire template for an existing visit
                         * and causing schema mismatches with the existing data.
                         */
                        val isLocked =
                            state.answers.isNotEmpty() ||
                                state.encounter?.status?.value == com.google.fhir.model.r4.Encounter.EncounterStatus.Finished ||
                                state.isFinalized

                        if (isLocked) {
                            Text(
                                text =
                                    stringResource(
                                        Res.string.questionnaire_format,
                                        state.selectedQuestionnaire?.title?.value ?: "",
                                    ), // Updated for i18n
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = selectorCd },
                            ) {
                                OutlinedTextField(
                                    value = state.selectedQuestionnaire?.title?.value ?: stringResource(Res.string.select_questionnaire),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(Res.string.questionnaire)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier =
                                        Modifier
                                            .menuAnchor(
                                                androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                            ).fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    state.availableQuestionnaires.forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q.title?.value ?: stringResource(Res.string.unknown)) },
                                            onClick = {
                                                viewModel.selectQuestionnaire(q)
                                                expanded = false
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.create_new)) },
                                        onClick = {
                                            expanded = false
                                            onCreateNewQuestionnaire()
                                        },
                                    )
                                }
                            }
                        }

                        state.selectedQuestionnaire?.let { q ->
                            io.healthplatform.chartcam.sdc.SdcQuestionnaireForm(
                                questionnaire = q,
                                answers = state.answers,
                                onFormUpdated = { newAnswers, newResponse ->
                                    viewModel.onFormUpdated(newAnswers, newResponse)
                                },
                                attachments = state.photos,
                                onTakePhotoRequested = { linkId -> onTakePhotos(q.id, linkId) },
                            )
                        }

                        val targetPhotosCount =
                            state.selectedQuestionnaire?.item?.count { it.type.value == Questionnaire.QuestionnaireItemType.Attachment }
                                ?: 0

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
                            Button(onClick = { onTakePhotos(state.selectedQuestionnaire?.id, null) }) {
                                Text(stringResource(Res.string.take_photos))
                            }
                        }
                    }
                }

                items(state.photos) { photo ->
                    PhotoGridItem(photo)
                }

                if (!state.isLoading &&
                    !state.isSyncing &&
                    state.encounter?.status?.value != com.google.fhir.model.r4.Encounter.EncounterStatus.Finished &&
                    !state.isFinalized
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(
                            onClick = { viewModel.finalizeEncounter() },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        ) {
                            Text(stringResource(Res.string.finalize_visit))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single photo thumbnail mapped from a FHIR DocumentReference.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
            /** The raw byte data decoded from the image file URL. */
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
                    } catch (e: Exception) {
                        ByteArray(0)
                    }
                }

            if (bytes.isNotEmpty()) {
                Image(
                    bitmap = bytes.decodeToImageBitmap(),
                    contentDescription = doc.description?.value ?: stringResource(Res.string.cd_patient_photo),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxWidth().height(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
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
