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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.viewmodel.TriageViewModel

import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.fullName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    capturedPhotoPaths: Map<String, String>,
    fhirRepository: FhirRepository,
    onProceedToEncounter: (String, Map<String, String>) -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { TriageViewModel(fhirRepository) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(capturedPhotoPaths) {
        viewModel.setPaths(capturedPhotoPaths)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Triage: Select Patient") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            state.selectedPatient?.let { patient ->
                ListItem(
                    headlineContent = { Text(patient.fullName, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Selected. ${state.capturedPhotoPaths.size} photos ready.") },
                    trailingContent = {
                        IconButton(onClick = { onProceedToEncounter(patient.id ?: "", state.capturedPhotoPaths) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Proceed")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
                HorizontalDivider()
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search MRN or Name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                ) {}
                
                IconButton(onClick = { viewModel.showCreatePatient(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Patient")
                }
            }

            LazyColumn {
                items(state.searchResults) { patient ->
                    ListItem(
                        headlineContent = { Text(patient.fullName) },
                        supportingContent = { Text("MRN: ${patient.mrn} | DOB: ${patient.customBirthDate}") },
                        modifier = Modifier.clickable { viewModel.selectPatient(patient) }
                    )
                    HorizontalDivider()
                }
                
                if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No patients found.")
                        }
                    }
                }
            }
        }
    }

    if (state.isCreatingPatient) {
        CreatePatientDialog(
            onDismissRequest = { viewModel.showCreatePatient(false) },
            onConfirm = { f, l, mrn, dob, g ->
                viewModel.createPatient(f, l, mrn, dob, g)
            }
        )
    }
}
