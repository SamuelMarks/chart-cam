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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_action_select_patient
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_create_patient
import chartcam.chartcam.generated.resources.cd_proceed
import chartcam.chartcam.generated.resources.cd_search_icon
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.mrn_dob_format
import chartcam.chartcam.generated.resources.no_patients_found
import chartcam.chartcam.generated.resources.search_placeholder
import chartcam.chartcam.generated.resources.selected_photos_ready
import chartcam.chartcam.generated.resources.triage_select_patient
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.getFullName
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
 * @param onBack Callback invoked when navigation back is requested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    capturedPhotoPaths: Map<String, String>,
    fhirRepository: FhirRepository,
    onProceedToEncounter: (String, Map<String, String>) -> Unit,
    onBack: () -> Unit = {},
) {
    val viewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel { TriageViewModel(fhirRepository) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(capturedPhotoPaths) {
        viewModel.setPaths(capturedPhotoPaths)
    }

    val currentLang by currentLanguageState.collectAsState()

    key(currentLang) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(Res.string.triage_select_patient),
                            modifier = Modifier.semantics { heading() },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                state.selectedPatient?.let { patient ->
                    TriagePatientSelectionHeader(
                        patient = patient,
                        photoCount = state.capturedPhotoPaths.size,
                        onProceed = { onProceedToEncounter(patient.id ?: "", state.capturedPhotoPaths) },
                    )
                    HorizontalDivider()
                }

                TriageSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onCreatePatientClick = { viewModel.showCreatePatient(true) },
                )

                LazyColumn {
                    items(state.searchResults) { patient ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    patient.getFullName(currentLang),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        Res.string.mrn_dob_format,
                                        patient.mrn,
                                        io.healthplatform.chartcam.utils
                                            .formatLocalizedDate(patient.customBirthDate, currentLang),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .semantics(mergeDescendants = true) {}
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(Res.string.cd_action_select_patient),
                                    ) { viewModel.selectPatient(patient) },
                        )
                        HorizontalDivider()
                    }

                    if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                        .semantics { liveRegion = LiveRegionMode.Polite },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(Res.string.no_patients_found),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
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
}

/**
 * Header displaying the currently selected patient.
 *
 * @param patient The selected patient.
 * @param photoCount Number of captured photos.
 * @param onProceed Callback when proceed is clicked.
 */
@Composable
private fun TriagePatientSelectionHeader(
    patient: com.google.fhir.model.r4.Patient,
    photoCount: Int,
    onProceed: () -> Unit,
) {
    val currentLang by currentLanguageState.collectAsState()
    val proceedLabel = stringResource(Res.string.cd_proceed)
    ListItem(
        headlineContent = {
            Text(
                patient.getFullName(currentLang),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                pluralStringResource(
                    Res.plurals.selected_photos_ready,
                    photoCount,
                    photoCount,
                ),
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
            )
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = proceedLabel) {
                    onProceed()
                }.padding(8.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                },
    )
}

/**
 * Search bar component for the triage screen.
 *
 * @param query The current search query.
 * @param onQueryChange Callback for query changes.
 * @param onCreatePatientClick Callback to create a new patient.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCreatePatientClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchBar(
            inputField = {
                androidx.compose.material3.SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(Res.string.cd_search_icon),
                        )
                    },
                    trailingIcon =
                        if (query.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(Res.string.clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            },
            expanded = false,
            onExpandedChange = { },
            modifier = Modifier.weight(1f),
        ) {}

        IconButton(onClick = onCreatePatientClick) {
            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_create_patient))
        }
    }
}
