/**
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.*
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
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/**
 * Screen for viewing and managing details of a clinical encounter.
 * Displays patient details, practitioner, and an interactive questionnaire form.
 *
 * @param patientId The unique identifier of the patient for this encounter.
 * @param visitId The unique identifier of the encounter. If "new", it will create a new encounter.
 * @param photoSessionManager Manager to retrieve captured photos.
 * @param fhirRepository Repository for FHIR operations.
 * @param authRepository Repository handling user authentication.
 * @param syncManager Manager responsible for syncing with remote FHIR servers.
 * @param questionnaireRepository Repository supplying questionnaire forms.
 * @param onBack Callback invoked when the user navigates back.
 * @param onTakePhotos Callback invoked when the user requests to take clinical photos. Passes the selected questionnaire ID.
 * @param onFinalized Callback invoked when the encounter is finalized.
 * @param onVisitCreated Optional callback invoked with the newly created encounter ID.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun EncounterDetailScreen(
    patientId: String,
    visitId: String,
    photoSessionManager: PhotoSessionManager,
    fhirRepository: FhirRepository,
    authRepository: AuthRepository,
    syncManager: SyncManager,
    questionnaireRepository: QuestionnaireRepository,
    onBack: () -> Unit,
    onTakePhotos: (String?) -> Unit,
    onFinalized: () -> Unit,
    onVisitCreated: ((String) -> Unit)? = null,
) {
    /**
     * The view model handling business logic for the EncounterDetailScreen.
     */
    val viewModel =
        androidx.lifecycle.viewmodel.compose.viewModel {
            EncounterDetailViewModel(fhirRepository, authRepository, syncManager, questionnaireRepository)
        }

    /**
     * State capturing the UI's data and interaction status.
     */
    val state by viewModel.uiState.collectAsState()

    /**
     * Controls the visibility of the "Create Questionnaire" dialog.
     */
    var showCreateDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(state.isFinalized) {
        if (state.isFinalized) {
            onFinalized()
            viewModel.resetFinalized()
        }
    }

    if (showCreateDialog) {
        /** Content description string for the custom questionnaire title input. */
        val titleInputCd = stringResource(Res.string.cd_questionnaire_title_input)

        /** Content description string for the photos count input. */
        val photosInputCd = stringResource(Res.string.cd_questionnaire_photos_input)

        /** Content description string for the photo labels input. */
        val labelsInputCd = stringResource(Res.string.cd_questionnaire_labels_input)

        /** State storing the new custom questionnaire title. */
        var newTitle by remember { mutableStateOf("") }

        /** State storing the requested number of photos. */
        var newPhotosCount by remember { mutableStateOf("4") }

        /** State storing the labels for each photo separated by commas. */
        var newLabels by remember { mutableStateOf("0, 1, 2, 3") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(Res.string.create_questionnaire)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(Res.string.title)) },
                        singleLine = true,
                        modifier =
                            Modifier.semantics { contentDescription = titleInputCd }.onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                    true
                                } else {
                                    false
                                }
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    OutlinedTextField(
                        value = newPhotosCount,
                        onValueChange = {
                            newPhotosCount = it.filter { c -> c.isDigit() }
                            val count = newPhotosCount.toIntOrNull() ?: 0
                            newLabels = (0 until count).joinToString(", ")
                        },
                        label = { Text(stringResource(Res.string.number_of_photos)) },
                        singleLine = true,
                        modifier =
                            Modifier.padding(top = 8.dp).semantics { contentDescription = photosInputCd }.onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                    true
                                } else {
                                    false
                                }
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    OutlinedTextField(
                        value = newLabels,
                        onValueChange = { newLabels = it },
                        label = { Text(stringResource(Res.string.labels_comma)) },
                        singleLine = true,
                        modifier =
                            Modifier.padding(top = 8.dp).semantics { contentDescription = labelsInputCd }.onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                    focusManager.clearFocus()
                                    val count = newPhotosCount.toIntOrNull() ?: 0
                                    if (newTitle.isNotBlank() && count > 0) {
                                        viewModel.createAndSelectQuestionnaire(newTitle, count, newLabels)
                                    }
                                    showCreateDialog = false
                                    true
                                } else {
                                    false
                                }
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                val count = newPhotosCount.toIntOrNull() ?: 0
                                if (newTitle.isNotBlank() && count > 0) {
                                    viewModel.createAndSelectQuestionnaire(newTitle, count, newLabels)
                                }
                                showCreateDialog = false
                            }),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = newPhotosCount.toIntOrNull() ?: 0
                    if (newTitle.isNotBlank() && count > 0) {
                        viewModel.createAndSelectQuestionnaire(newTitle, count, newLabels)
                    }
                    showCreateDialog = false
                }) { Text(stringResource(Res.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.visit_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.isLoading && !state.isSyncing) {
                FloatingActionButton(onClick = {
                    viewModel.finalizeEncounter()
                }) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.cd_finalize_encounter))
                }
            }
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
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                state.patient?.let { patient ->
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(Res.string.mrn_date_format, patient.mrn ?: "", state.encounter?.encounterDate ?: ""),
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                                text = { Text(q.title?.value ?: "") },
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
                                showCreateDialog = true
                            },
                        )
                    }
                }

                state.selectedQuestionnaire?.let { q ->
                    DynamicQuestionnaireForm(
                        questionnaire = q,
                        answers = state.answers,
                        onAnswerChanged = { linkId, value -> viewModel.onAnswerChanged(linkId, value) },
                    )
                }

                val targetPhotosCount =
                    state.selectedQuestionnaire?.item?.count { it.type.value == Questionnaire.QuestionnaireItemType.Attachment } ?: 0

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
                    Button(onClick = { onTakePhotos(state.selectedQuestionnaire?.id) }) {
                        Text(stringResource(Res.string.take_photos))
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp),
                ) {
                    items(state.photos) { photo ->
                        PhotoGridItem(photo)
                    }
                }
            }
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
    ElevatedCard {
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
