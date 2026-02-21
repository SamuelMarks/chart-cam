package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.viewmodel.TriageViewModel
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import kotlinx.serialization.json.Json

/**
 * Screen to assign captured photos to a Patient.
 *
 * @param photosJson JSON String of photos passed from Navigation.
 * @param fhirRepository Repository instance.
 * @param onPatientSelected Callback to navigate to Encounter Detail.
 */
@Composable
fun TriageScreen(
    photosJson: String,
    fhirRepository: FhirRepository,
    onPatientSelected: (String) -> Unit // Patient ID
) {
    // Manual VM composition
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { TriageViewModel(fhirRepository) }
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Init data (parse JSON simple map)
    LaunchedEffect(photosJson) {
        try {
            val map = Json.decodeFromString<Map<String, String>>(photosJson)
            viewModel.setPaths(map)
        } catch (_: Exception) {
            // Handle error / empty
        }
    }
    
    // Logic: If patient is selected, trigger navigation
    LaunchedEffect(state.selectedPatient) {
        state.selectedPatient?.let {
            onPatientSelected(it.id)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreatePatient(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Create Patient")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                "Triage Photos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "${state.capturedPhotoPaths.size} photos ready to assign.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search by Name or MRN") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).onKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                        focusManager.clearFocus()
                        true
                    } else false
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon = { Icon(Icons.Default.Person, null) }
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.searchResults) { patient ->
                    ListItem(
                        headlineContent = { 
                            Text("${patient.name.firstOrNull()?.family}, ${patient.name.firstOrNull()?.given?.firstOrNull()}") 
                        },
                        supportingContent = { Text("MRN: ${patient.mrn} | DOB: ${patient.birthDate}") },
                        modifier = Modifier.clickable { viewModel.selectPatient(patient) }
                    )
                }
                
                if (state.searchResults.isEmpty() && state.searchQuery.isNotEmpty()) {
                    item {
                        Text("No patients found.", modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
    
    if (state.isCreatingPatient) {
        // Simple Overlay for creation (Prompt Step 7 details this more, but we wire it here)
        CreatePatientDialog(
            onDismissRequest = { viewModel.showCreatePatient(false) },
            onConfirm = { f, l, mrn, dob, g -> viewModel.createPatient(f, l, mrn, dob, g) }
        )
    }
}