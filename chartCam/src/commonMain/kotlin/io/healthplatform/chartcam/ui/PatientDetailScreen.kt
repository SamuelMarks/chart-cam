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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("Patient Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewVisit) {
                Icon(Icons.Default.Add, contentDescription = "New Visit")
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
                        text = "MRN: ${patient.mrn} | DOB: ${patient.customBirthDate}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "Visit History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.encounters) { encounter ->
                    ListItem(
                        headlineContent = { Text(encounter.encounterDate) },
                        supportingContent = { Text(encounter.text?.div?.value?.removePrefix("<div>")?.removeSuffix("</div>") ?: "No notes") },
                        modifier = Modifier.clickable { onVisitSelected(encounter.id ?: "") }
                    )
                    HorizontalDivider()
                }
                
                if (state.encounters.isEmpty() ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No visits found", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}