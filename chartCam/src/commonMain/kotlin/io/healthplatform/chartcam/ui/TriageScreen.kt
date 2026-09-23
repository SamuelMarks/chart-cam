/**
 * @file TriageScreen.kt
 * Contains declarations for TriageScreen.kt.
 *
 * Triage Screen definition.
 * Allows users to search for or create patients to attach captured photos to.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.action_clear_selection
import chartcam.chartcam.generated.resources.action_select_all
import chartcam.chartcam.generated.resources.captured_photos_count_format
import chartcam.chartcam.generated.resources.cd_action_select_patient
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_create_patient
import chartcam.chartcam.generated.resources.cd_delete_selected_photos
import chartcam.chartcam.generated.resources.cd_proceed
import chartcam.chartcam.generated.resources.cd_search_icon
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.mrn_dob_format
import chartcam.chartcam.generated.resources.no_patients_found
import chartcam.chartcam.generated.resources.search_placeholder
import chartcam.chartcam.generated.resources.selected_photos_ready
import chartcam.chartcam.generated.resources.triage_select_patient
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.getFullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.ui.theme.AppSpacing
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
 * @param viewModel ViewModel managing triage state, search, and patient selection.
 * @param onProceedToEncounter Callback invoked with the selected patient ID and the `capturedPhotoPaths`
 *        map to initiate or append to a clinical encounter.
 * @param onBack Callback invoked when navigation back is requested.
 * @param fileStorage File storage used to delete rejected photos from disk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    viewModel: TriageViewModel,
    onProceedToEncounter: (String, Map<String, String>) -> Unit,
    onBack: () -> Unit,
    fileStorage: io.healthplatform.chartcam.files.FileStorage,
) {
    val state by viewModel.uiState.collectAsState()

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
                if (state.capturedPhotoPaths.isNotEmpty()) {
                    TriagePhotoBatchBar(
                        photoPaths = state.capturedPhotoPaths,
                        selectedKeys = state.selectedPhotoKeys,
                        actions = buildTriagePhotoBatchActions(viewModel, fileStorage),
                    )
                }

                state.selectedPatient?.let { patient ->
                    val pathsToAssign =
                        if (state.selectedPhotoKeys.isNotEmpty()) {
                            state.capturedPhotoPaths.filterKeys { it in state.selectedPhotoKeys }
                        } else {
                            state.capturedPhotoPaths
                        }
                    TriagePatientSelectionHeader(
                        patient = patient,
                        photoCount = pathsToAssign.size,
                        onProceed = { onProceedToEncounter(patient.id ?: "", pathsToAssign) },
                    )
                    HorizontalDivider()
                }

                TriageSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onCreatePatientClick = { viewModel.showCreatePatient(true) },
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                ) {
                    state.searchResults.forEach { patient ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    patient.getFullName(currentLang),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() },
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
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.xl)
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

        if (state.isCreatingPatient) {
            CreatePatientDialog(
                onDismissRequest = handleDismissCreatePatient(viewModel),
                onConfirm = handleConfirmCreatePatient(viewModel),
            )
        }
    }
}

/**
 * Callbacks for [TriagePhotoBatchBar] actions.
 *
 * @property onToggleSelect Callback when a photo chip is toggled.
 * @property onSelectAll Callback to select all photos.
 * @property onClearSelection Callback to clear selection.
 * @property onDeleteSelected Callback to delete selected photos.
 */
@androidx.compose.runtime.Immutable
data class TriagePhotoBatchActions(
    val onToggleSelect: (String) -> Unit,
    val onSelectAll: () -> Unit,
    val onClearSelection: () -> Unit,
    val onDeleteSelected: () -> Unit,
)

/**
 * Builds action callbacks for [TriagePhotoBatchBar].
 *
 * @param viewModel ViewModel handling triage operations.
 * @param fileStorage File storage used to delete rejected photos from disk.
 * @return Configured [TriagePhotoBatchActions].
 */
internal fun buildTriagePhotoBatchActions(
    viewModel: TriageViewModel,
    fileStorage: FileStorage,
): TriagePhotoBatchActions =
    TriagePhotoBatchActions(
        onToggleSelect = viewModel::togglePhotoSelection,
        onSelectAll = viewModel::selectAllPhotos,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelected = { viewModel.deleteSelectedPhotos(fileStorage) },
    )

/**
 * Produces dismissal callback for patient creation dialog.
 *
 * @param viewModel The triage ViewModel.
 * @return Callback to dismiss the create patient dialog.
 */
internal fun handleDismissCreatePatient(viewModel: TriageViewModel): () -> Unit =
    { viewModel.showCreatePatient(false) }

/**
 * Produces confirmation callback for patient creation dialog.
 *
 * @param viewModel The triage ViewModel.
 * @return Callback accepting patient registration details.
 */
internal fun handleConfirmCreatePatient(
    viewModel: TriageViewModel,
): (String, String, String, kotlinx.datetime.LocalDate, String?) -> Unit =
    { f, l, mrn, dob, g -> viewModel.createPatient(f, l, mrn, dob, g ?: "unknown") }

/**
 * Header displaying the currently selected patient.
 *
 * @param patient The selected patient.
 * @param photoCount Number of captured photos.
 * @param onProceed Callback when proceed is clicked.
 */
@Composable
internal fun TriagePatientSelectionHeader(
    patient: dev.ohs.fhir.model.r4.Patient,
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
                modifier = Modifier.semantics { heading() },
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
                .minimumInteractiveComponentSize()
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                }.clickable(role = Role.Button, onClickLabel = proceedLabel) {
                    onProceed()
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
internal fun TriageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCreatePatientClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val searchLabel = stringResource(Res.string.search_placeholder)
        DockedSearchBar(
            inputField = {
                androidx.compose.material3.SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                    modifier = Modifier.semantics { contentDescription = searchLabel },
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

        IconButton(
            onClick = onCreatePatientClick,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_create_patient))
        }
    }
}

/**
 * Visual photo batch management bar for multi-selection, select all, and batch deletion.
 *
 * @param photoPaths Map of photo identifiers to file paths.
 * @param selectedKeys Set of currently selected photo keys.
 * @param actions The actions supported by the batch bar.
 */
@Composable
internal fun TriagePhotoBatchBar(
    photoPaths: Map<String, String>,
    selectedKeys: Set<String>,
    actions: TriagePhotoBatchActions,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                .testTag("TriagePhotoBatchBar"),
        colors =
            CardDefaults
                .cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.captured_photos_count_format, photoPaths.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Row {
                    TextButton(onClick = actions.onSelectAll) {
                        Text(stringResource(Res.string.action_select_all))
                    }
                    if (selectedKeys.isNotEmpty()) {
                        TextButton(onClick = actions.onClearSelection) {
                            Text(stringResource(Res.string.action_clear_selection))
                        }
                        IconButton(onClick = actions.onDeleteSelected) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.cd_delete_selected_photos),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                modifier =
                    Modifier
                        .padding(top = AppSpacing.xs)
                        .horizontalScroll(rememberScrollState()),
            ) {
                photoPaths.keys.forEach { key ->
                    val isSelected = selectedKeys.contains(key)
                    FilterChip(
                        selected = isSelected,
                        onClick = { actions.onToggleSelect(key) },
                        label = { Text(key) },
                        leadingIcon =
                            if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}
