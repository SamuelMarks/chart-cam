package io.healthplatform.chartcam.ui

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*


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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.viewmodel.PatientDetailViewModel

import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.fullName
import io.healthplatform.chartcam.models.encounterDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    fhirRepository: FhirRepository,
    onBack: () -> Unit,
    onNewVisit: () -> Unit,
    onVisitSelected: (String) -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { PatientDetailViewModel(fhirRepository) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPatientData(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.patient_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete_patient), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text(stringResource(Res.string.delete_patient)) },
                            text = { Text(stringResource(Res.string.delete_patient_message)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    viewModel.deletePatient {
                                        onBack()
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewVisit) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_new_visit))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            state.patient?.let { patient ->
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(Res.string.mrn_dob_format, patient.mrn ?: "", patient.customBirthDate ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(Res.string.visit_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.encounters) { encounter ->
                    ListItem(
                        headlineContent = { Text(encounter.encounterDate) },
                        supportingContent = { Text(encounter.text?.div?.value?.removePrefix("<div>")?.removeSuffix("</div>") ?: stringResource(Res.string.no_notes)) },
                        modifier = Modifier.clickable(role = Role.Button) { onVisitSelected(encounter.id ?: "") }
                    )
                    HorizontalDivider()
                }
                
                if (state.encounters.isEmpty() ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(Res.string.no_visits_found), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}