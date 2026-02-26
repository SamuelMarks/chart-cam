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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.fullName
import io.healthplatform.chartcam.models.encounterDate

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
    onVisitCreated: ((String) -> Unit)? = null
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        EncounterDetailViewModel(fhirRepository, authRepository, syncManager, questionnaireRepository)
    }
    
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingPhotos by photoSessionManager.pendingPhotos.collectAsState()
    
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
        var newTitle by remember { mutableStateOf("") }
        var newPhotosCount by remember { mutableStateOf("4") }
        var newLabels by remember { mutableStateOf("0, 1, 2, 3") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Questionnaire") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.semantics { contentDescription = "Questionnaire Title Input" }.onKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = newPhotosCount,
                        onValueChange = { 
                            newPhotosCount = it.filter { c -> c.isDigit() }
                            val count = newPhotosCount.toIntOrNull() ?: 0
                            newLabels = (0 until count).joinToString(", ")
                        },
                        label = { Text("Number of Photos") },
                        singleLine = true,
                        modifier = Modifier.padding(top = 8.dp).semantics { contentDescription = "Questionnaire Photos Count Input" }.onKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = newLabels,
                        onValueChange = { newLabels = it },
                        label = { Text("Labels (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.padding(top = 8.dp).semantics { contentDescription = "Questionnaire Labels Input" }.onKeyEvent {
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
                            } else false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            val count = newPhotosCount.toIntOrNull() ?: 0
                            if (newTitle.isNotBlank() && count > 0) {
                                viewModel.createAndSelectQuestionnaire(newTitle, count, newLabels)
                            }
                            showCreateDialog = false
                        })
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
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Visit Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isLoading && !state.isSyncing) {
                FloatingActionButton(onClick = { 
                    viewModel.finalizeEncounter() 
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Finalize Encounter")
                }
            }
        }
    ) { padding ->
        if (state.isLoading || state.isSyncing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (state.isSyncing) {
                        Text("Syncing to Server...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                state.patient?.let { patient ->
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "MRN: ${patient.mrn} | ${state.encounter?.encounterDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                state.practitioner?.let { prac ->
                    Text(
                        text = "Provider: Dr. ${prac.fullName}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Questionnaire Selection
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = "Questionnaire Selector" }
                ) {
                    OutlinedTextField(
                        value = state.selectedQuestionnaire?.title?.value ?: "Select Questionnaire",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Questionnaire") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        state.availableQuestionnaires.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q.title?.value ?: "") },
                                onClick = {
                                    viewModel.selectQuestionnaire(q)
                                    expanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Create new...") },
                            onClick = {
                                expanded = false
                                showCreateDialog = true
                            }
                        )
                    }
                }
                
                state.selectedQuestionnaire?.let { q ->
                    DynamicQuestionnaireForm(
                        questionnaire = q,
                        answers = state.answers,
                        onAnswerChanged = { linkId, value -> viewModel.onAnswerChanged(linkId, value) }
                    )
                }

                val targetPhotosCount = state.selectedQuestionnaire?.item?.count { it.type.value == Questionnaire.QuestionnaireItemType.Attachment } ?: 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Captured Photos (${state.photos.size}/$targetPhotosCount)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(onClick = { onTakePhotos(state.selectedQuestionnaire?.id) }) {
                        Text("Take Photos")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp)
                ) {
                    items(state.photos) { photo ->
                        PhotoGridItem(photo)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun PhotoGridItem(doc: DocumentReference) {
    ElevatedCard {
        Column {
            val bytes = remember(doc.content.firstOrNull()?.attachment?.url?.value ?: "") {
                try {
                    val storage = createFileStorage()
                    storage.readImage(doc.content.firstOrNull()?.attachment?.url?.value ?: "")
                } catch (e: Exception) { ByteArray(0) }
            }
            
            if (bytes.isNotEmpty()) {
                Image(
                    bitmap = bytes.decodeToImageBitmap(),
                    contentDescription = doc.description?.value ?: "Patient Photo",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().height(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Image Load Error")
                }
            }
            
            Text(
                text = doc.description?.value ?: "Photo",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}