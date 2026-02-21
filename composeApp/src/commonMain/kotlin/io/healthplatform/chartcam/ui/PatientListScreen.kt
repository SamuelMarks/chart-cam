package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.viewmodel.PatientListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    fhirRepository: FhirRepository,
    exportImportService: ExportImportService,
    onPatientSelected: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { PatientListViewModel(fhirRepository, exportImportService) }
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Directory") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Data") },
                            onClick = {
                                viewModel.exportData()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Data") },
                            onClick = {
                                showImportDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setCreateDialogVisible(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Patient")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search Name or MRN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {}

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.patients) { patient ->
                    PatientListItem(
                        patient = patient,
                        onClick = { onPatientSelected(patient.id) }
                    )
                    HorizontalDivider()
                }
                if (state.patients.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No patients found", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    if (state.isCreatingPatient) {
        CreatePatientDialog(
            onDismissRequest = { viewModel.setCreateDialogVisible(false) },
            onConfirm = { f, l, mrn, dob, g ->
                viewModel.createPatient(f, l, mrn, dob, g) { newPatientId ->
                    onPatientSelected(newPatientId)
                }
            }
        )
    }

    if (state.exportedData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportData() },
            title = { Text("Data Exported") },
            text = { Text("Data has been generated. Do you want to copy it to the clipboard?") },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(state.exportedData!!))
                    viewModel.clearExportData()
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportData() }) {
                    Text("Close")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Data") },
            text = {
                TextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste JSON here") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(importText)
                    showImportDialog = false
                    importText = ""
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PatientListItem(
    patient: io.healthplatform.chartcam.models.Patient,
    onClick: () -> Unit
) {
    val name = patient.name.firstOrNull()
    val fullName = "${name?.family ?: "Unknown"}, ${name?.given?.joinToString(" ") ?: ""}"
    
    ListItem(
        headlineContent = { Text(fullName, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { 
            Text("MRN: ${patient.mrn} | DOB: ${patient.birthDate}", style = MaterialTheme.typography.bodyMedium) 
        },
        modifier = Modifier.clickable { onClick() }
    )
}