package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.utils.createShareService
import io.healthplatform.chartcam.viewmodel.PatientListViewModel

import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.fullName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    fhirRepository: FhirRepository,
    exportImportService: ExportImportService,
    authRepository: AuthRepository,
    onPatientSelected: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { PatientListViewModel(fhirRepository, exportImportService, authRepository) }
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadPatients()
    }
    var showMenu by remember { mutableStateOf(false) }
    
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportAllVisits by remember { mutableStateOf(true) }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    
    val shareService = remember { createShareService() }
    val fileStorage = remember { createFileStorage() }

    val focusManager = LocalFocusManager.current

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
                            text = { Text(if (state.showAllPatients) "Show My Patients Only" else "Show All Patients") },
                            onClick = {
                                viewModel.setShowAllPatients(!state.showAllPatients)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Data") },
                            onClick = {
                                showExportPasswordDialog = true
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
                        onClick = { onPatientSelected(patient.id ?: "") }
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

    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Export Password") },
            text = {
                Column {
                    TextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Enter a password to encrypt data") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().onKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.clearFocus()
                                viewModel.exportData(exportPassword, exportAllVisits)
                                showExportPasswordDialog = false
                                exportPassword = ""
                                exportAllVisits = true
                                true
                            } else false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.exportData(exportPassword, exportAllVisits)
                            showExportPasswordDialog = false
                            exportPassword = ""
                            exportAllVisits = true
                        }),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable { exportAllVisits = !exportAllVisits },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = exportAllVisits,
                            onCheckedChange = { exportAllVisits = it }
                        )
                        Text("Export all visits of all patients", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.exportData(exportPassword, exportAllVisits)
                    showExportPasswordDialog = false
                    exportPassword = ""
                    exportAllVisits = true
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (state.exportedData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportData() },
            title = { Text("Data Exported") },
            text = {
                Column {
                    Text("Data has been encrypted. Share the file and the password separately.")
                    TextButton(onClick = {
                        state.exportPassword?.let { shareService.shareText(it) }
                    }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Share Password")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bytes = state.exportedData!!.encodeToByteArray()
                    val path = fileStorage.saveImage("export.enc", bytes)
                    shareService.shareFile(path)
                }) {
                    Text("Share File")
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
                Column {
                    TextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste Data Here") },
                        modifier = Modifier.fillMaxWidth().onKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else false
                        }
                    )
                    TextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).onKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.clearFocus()
                                viewModel.importData(importText, importPassword) {
                                    showImportDialog = false
                                    importText = ""
                                    importPassword = ""
                                }
                                true
                            } else false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.importData(importText, importPassword) {
                                showImportDialog = false
                                importText = ""
                                importPassword = ""
                            }
                        }),
                        singleLine = true
                    )
                    if (state.error != null) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(importText, importPassword) {
                        showImportDialog = false
                        importText = ""
                        importPassword = ""
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportDialog = false
                    viewModel.clearError()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PatientListItem(
    patient: com.google.fhir.model.r4.Patient,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(patient.fullName, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { 
            Text("MRN: ${patient.mrn} | DOB: ${patient.customBirthDate}", style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.clickable { onClick() }
    )
}
