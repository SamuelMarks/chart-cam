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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import chartcam.chartcam.generated.resources.captured_photos_format
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_finalize_encounter
import chartcam.chartcam.generated.resources.cd_patient_photo
import chartcam.chartcam.generated.resources.cd_questionnaire_selector
import chartcam.chartcam.generated.resources.create_new
import chartcam.chartcam.generated.resources.image_load_error
import chartcam.chartcam.generated.resources.mrn_date_format
import chartcam.chartcam.generated.resources.provider_format
import chartcam.chartcam.generated.resources.questionnaire
import chartcam.chartcam.generated.resources.select_questionnaire
import chartcam.chartcam.generated.resources.syncing_to_server
import chartcam.chartcam.generated.resources.take_photos
import chartcam.chartcam.generated.resources.title
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
    syncManager: SyncManager,
    questionnaireRepository: QuestionnaireRepository,
    newlyCreatedQuestionnaireId: String? = null,
    onBack: () -> Unit,
    onTakePhotos: (String?) -> Unit,
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
            EncounterDetailViewModel(fhirRepository, authRepository, syncManager, questionnaireRepository)
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

    LaunchedEffect(state.isFinalized) {
        if (state.isFinalized) {
            onFinalized()
            viewModel.resetFinalized()
        }
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
                                onCreateNewQuestionnaire()
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
