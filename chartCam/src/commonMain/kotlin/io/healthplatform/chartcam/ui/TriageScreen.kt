/**
 * @file TriageScreen.kt
 * Contains declarations for TriageScreen.kt.
 *
 * Triage Screen definition.
 * Allows users to search for or create patients to attach captured photos to.
 */
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_create_patient
import chartcam.chartcam.generated.resources.cd_proceed
import chartcam.chartcam.generated.resources.cd_search_icon
import chartcam.chartcam.generated.resources.mrn_dob_format
import chartcam.chartcam.generated.resources.no_patients_found
import chartcam.chartcam.generated.resources.search_placeholder
import chartcam.chartcam.generated.resources.selected_photos_ready
import chartcam.chartcam.generated.resources.triage_select_patient
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.fullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.viewmodel.TriageViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Screen designed to associate recently taken photos with a patient.
 * The user can search existing patients or create a new one. State is hoisted from the
 * [TriageViewModel] which manages the active search query, search results, and patient selection.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param capturedPhotoPaths Map of step name to photo file path. These paths are carried forward
 *        to the encounter creation phase.
 * @param fhirRepository Repository used to search or create patients.
 * @param onProceedToEncounter Callback invoked with the selected patient ID and the `capturedPhotoPaths`
 *        map to initiate or append to a clinical encounter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    capturedPhotoPaths: Map<String, String>,
    fhirRepository: FhirRepository,
    onProceedToEncounter: (String, Map<String, String>) -> Unit,
) {
    val viewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel { TriageViewModel(fhirRepository) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(capturedPhotoPaths) {
        viewModel.setPaths(capturedPhotoPaths)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.triage_select_patient), modifier = Modifier.semantics { heading() }) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            state.selectedPatient?.let { patient ->
                ListItem(
                    headlineContent = { Text(patient.fullName, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        Text(
                            pluralStringResource(
                                Res.plurals.selected_photos_ready,
                                state.capturedPhotoPaths.size,
                                state.capturedPhotoPaths.size,
                            ),
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onProceedToEncounter(patient.id ?: "", state.capturedPhotoPaths) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.cd_proceed))
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                )
                HorizontalDivider()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    inputField = {
                        androidx.compose.material3.SearchBarDefaults.InputField(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.cd_search_icon)) },
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.weight(1f),
                ) {}

                IconButton(onClick = { viewModel.showCreatePatient(true) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_create_patient))
                }
            }

            LazyColumn {
                items(state.searchResults) { patient ->
                    ListItem(
                        headlineContent = { Text(patient.fullName) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    Res.string.mrn_dob_format,
                                    patient.mrn,
                                    io.healthplatform.chartcam.utils
                                        .formatLocalizedDate(patient.customBirthDate),
                                ),
                            )
                        },
                        modifier =
                            Modifier.minimumInteractiveComponentSize().clickable(
                                role = Role.Button,
                            ) { viewModel.selectPatient(patient) },
                    )
                    HorizontalDivider()
                }

                if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(Res.string.no_patients_found))
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
            },
        )
    }
}
