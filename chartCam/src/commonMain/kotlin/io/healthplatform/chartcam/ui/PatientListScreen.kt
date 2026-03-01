package io.healthplatform.chartcam.ui

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*


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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import io.healthplatform.chartcam.ui.setAppLanguage
import io.healthplatform.chartcam.ui.currentLanguageState
import androidx.compose.runtime.rememberCoroutineScope

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
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
                title = { Text(stringResource(Res.string.patient_directory)) },
                actions = {
                    var showLanguageMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Translate, contentDescription = stringResource(Res.string.cd_switch_language))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = { setAppLanguage("en"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Español") },
                                onClick = { setAppLanguage("es"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("日本語") },
                                onClick = { setAppLanguage("ja"); showLanguageMenu = false }
                            )
                        }
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (state.showAllPatients) stringResource(Res.string.show_my_patients_only) else stringResource(Res.string.show_all_patients)) },
                            onClick = {
                                viewModel.setShowAllPatients(!state.showAllPatients)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.export_data)) },
                            onClick = {
                                showExportPasswordDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.import_title)) },
                            onClick = {
                                showImportDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.logout)) },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.about)) },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete_my_account), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setCreateDialogVisible(true) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_add_patient))
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
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.cd_search_icon)) },
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
                            Text(stringResource(Res.string.no_patients_found), color = MaterialTheme.colorScheme.secondary)
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
            title = { Text(stringResource(Res.string.export_password_title)) },
            text = {
                Column {
                    TextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text(stringResource(Res.string.export_password_label)) },
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
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable(role = Role.Checkbox) { exportAllVisits = !exportAllVisits },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = exportAllVisits,
                            onCheckedChange = { exportAllVisits = it }
                        )
                        Text(stringResource(Res.string.export_all_patients), modifier = Modifier.padding(start = 8.dp))
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
                    Text(stringResource(Res.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
    
    if (state.exportedData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportData() },
            title = { Text(stringResource(Res.string.data_exported_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.data_exported_message))
                    TextButton(onClick = {
                        state.exportPassword?.let { shareService.shareText(it) }
                    }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(Res.string.share_password))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bytes = state.exportedData!!.encodeToByteArray()
                    val path = fileStorage.saveImage("export.enc", bytes)
                    shareService.shareFile(path)
                }) {
                    Text(stringResource(Res.string.share_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportData() }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(Res.string.about_title)) },
            text = { Text(stringResource(Res.string.version_text, "0.0.1")) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.delete_account_title)) },
            text = { Text(stringResource(Res.string.delete_account_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAccount {
                        onLogout()
                    }
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(Res.string.import_title)) },
            text = {
                Column {
                    TextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text(stringResource(Res.string.paste_data_here)) },
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
                        label = { Text(stringResource(Res.string.password)) },
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
                    Text(stringResource(Res.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportDialog = false
                    viewModel.clearError()
                }) {
                    Text(stringResource(Res.string.cancel))
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
            Text(stringResource(Res.string.mrn_dob_format, patient.mrn ?: "", patient.customBirthDate ?: ""), style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.clickable(role = Role.Button) { onClick() }
    )
}
