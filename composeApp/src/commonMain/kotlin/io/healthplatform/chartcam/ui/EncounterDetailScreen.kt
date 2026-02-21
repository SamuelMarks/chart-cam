package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.models.DocumentReference
import io.healthplatform.chartcam.models.Questionnaire
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun EncounterDetailScreen(
    patientId: String,
    visitId: String,
    photosJson: String,
    fhirRepository: FhirRepository,
    authRepository: AuthRepository,
    syncManager: SyncManager,
    questionnaireRepository: QuestionnaireRepository,
    onBack: () -> Unit,
    onTakePhotos: (String?) -> Unit,
    onFinalized: () -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        EncounterDetailViewModel(fhirRepository, authRepository, syncManager, questionnaireRepository)
    }
    
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId, visitId) {
        try {
            val map = Json.decodeFromString<Map<String, String>>(photosJson)
            viewModel.initialize(patientId, visitId, map)
        } catch (e: Exception) {
            viewModel.initialize(patientId, visitId, emptyMap())
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
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Questionnaire") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") }
                    )
                    OutlinedTextField(
                        value = newPhotosCount,
                        onValueChange = { newPhotosCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Number of Photos") },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = newPhotosCount.toIntOrNull() ?: 0
                    if (newTitle.isNotBlank() && count > 0) {
                        viewModel.createAndSelectQuestionnaire(newTitle, count)
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
                FloatingActionButton(onClick = { viewModel.finalizeEncounter() }) {
                    Icon(Icons.Default.Check, contentDescription = "Finalize")
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
                        text = "${patient.name.firstOrNull()?.family}, ${patient.name.firstOrNull()?.given?.firstOrNull()}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "MRN: ${patient.mrn} | ${state.encounter?.period?.start?.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                state.practitioner?.let { prac ->
                    Text(
                        text = "Provider: Dr. ${prac.name.firstOrNull()?.family}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Questionnaire Selection
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = state.selectedQuestionnaire?.title ?: "Select Questionnaire",
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
                                text = { Text(q.title) },
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
                
                state.selectedQuestionnaire?.item?.find { it.type == "string" }?.let { notesItem ->
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text(notesItem.text) },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp),
                        maxLines = 5
                    )
                }

                val targetPhotosCount = state.selectedQuestionnaire?.item?.count { it.type == "attachment" } ?: 0

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
                    contentPadding = PaddingValues(bottom = 80.dp)
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
    Card(
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            val bytes = remember(doc.content.url) {
                try {
                    val storage = createFileStorage()
                    storage.readImage(doc.content.url)
                } catch (e: Exception) { ByteArray(0) }
            }
            
            if (bytes.isNotEmpty()) {
                Image(
                    bitmap = bytes.decodeToImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().height(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Image Load Error")
                }
            }
            
            Text(
                text = doc.description ?: "Photo",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
