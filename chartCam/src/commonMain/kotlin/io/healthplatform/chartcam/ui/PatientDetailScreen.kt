/**
 * @file PatientDetailScreen.kt
 * Contains declarations for PatientDetailScreen.kt.
 *
 * Provides the PatientDetailScreen component for displaying detailed information about a patient.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_action_view_encounter
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_more
import chartcam.chartcam.generated.resources.cd_new_visit
import chartcam.chartcam.generated.resources.delete
import chartcam.chartcam.generated.resources.delete_patient
import chartcam.chartcam.generated.resources.delete_patient_message
import chartcam.chartcam.generated.resources.mrn_dob_format
import chartcam.chartcam.generated.resources.no_notes
import chartcam.chartcam.generated.resources.no_visits_found
import chartcam.chartcam.generated.resources.patient_detail
import chartcam.chartcam.generated.resources.visit_history
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.encounterDate
import io.healthplatform.chartcam.models.getFullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.ui.theme.AppSpacing
import io.healthplatform.chartcam.viewmodel.PatientDetailViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Screen displaying the details of a selected patient and their encounter history.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param viewModel ViewModel handling patient details and encounter history.
 * @param onBack Callback invoked when the user requests to navigate back.
 * @param onNewVisit Callback invoked when the user requests to create a new visit (encounter) for the patient.
 * @param onVisitSelected Callback invoked when the user selects a specific past visit. Provides the visit ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    viewModel: PatientDetailViewModel,
    onBack: () -> Unit,
    onNewVisit: () -> Unit,
    onVisitSelected: (String) -> Unit,
) {
    /** State representing the current UI data for the patient details. */
    val state by viewModel.uiState.collectAsState()

    val currentLang by currentLanguageState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    key(currentLang) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                PatientDetailTopBar(
                    onBack = onBack,
                    onDeletePatient = { viewModel.deletePatient { onBack() } },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNewVisit) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_new_visit))
                }
            },
        ) { padding ->
            PatientDetailContent(padding, state, onVisitSelected)
        }
    }
}

/**
 * Internal helper for patient detail content.
 *
 * @param padding Padding values provided by Scaffold.
 * @param state Current UI state for patient details.
 * @param onVisitSelected Callback when a visit is selected.
 */
@Composable
internal fun PatientDetailContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: io.healthplatform.chartcam.viewmodel.PatientDetailUiState,
    onVisitSelected: (String) -> Unit,
) {
    val currentLang by currentLanguageState.collectAsState()
    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
        state.patient?.let { PatientInfo(it) }

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.visit_history),
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier
                    .padding(AppSpacing.md)
                    .semantics { heading() },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            state.encounters.forEach { encounter ->
                val visitId = encounter.id ?: ""
                ListItem(
                    headlineContent = {
                        Text(
                            io.healthplatform.chartcam.utils
                                .formatLocalizedDate(encounter.encounterDate, currentLang),
                        )
                    },
                    supportingContent = {
                        val notes =
                            encounter.text?.let { narrative ->
                                io.healthplatform.chartcam.utils.QuestionnaireUtils.stripNarrativeDiv(
                                    narrative.div.value,
                                )
                            } ?: stringResource(Res.string.no_notes)
                        Text(notes)
                    },
                    modifier =
                        Modifier
                            .minimumInteractiveComponentSize()
                            .semantics(mergeDescendants = true) {}
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(Res.string.cd_action_view_encounter),
                            ) { onVisitSelected(visitId) },
                )
                HorizontalDivider()
            }

            if (state.encounters.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.xl)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(Res.string.no_visits_found),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

/**
 * Internal helper for patient detail top app bar.
 *
 * @param onBack Callback when back navigation is requested.
 * @param onDeletePatient Callback when patient deletion is confirmed.
 * @param scrollBehavior TopAppBar scroll behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatientDetailTopBar(
    onBack: () -> Unit,
    onDeletePatient: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                stringResource(Res.string.patient_detail),
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
        actions = {
            var showMenu by remember { mutableStateOf(false) }
            var showDeleteConfirm by remember { mutableStateOf(false) }

            IconButton(
                onClick = { showMenu = !showMenu },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(Res.string.delete_patient))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                    onClick = {
                        showMenu = false
                        showDeleteConfirm = true
                    },
                )
            }

            if (showDeleteConfirm) {
                PatientDeleteConfirmDialog(
                    onDismiss = { showDeleteConfirm = false },
                    onDeletePatient = onDeletePatient,
                )
            }
        },
    )
}

/**
 * Internal helper for patient information header.
 *
 * @param patient The FHIR Patient resource.
 */
@Composable
internal fun PatientInfo(patient: dev.ohs.fhir.model.r4.Patient) {
    val currentLang by currentLanguageState.collectAsState()
    Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.md)) {
        Text(
            text = patient.getFullName(currentLang),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text =
                stringResource(
                    Res.string.mrn_dob_format,
                    patient.mrn,
                    io.healthplatform.chartcam.utils
                        .formatLocalizedDate(patient.customBirthDate, currentLang),
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = AppSpacing.xs),
        )
    }
}

/**
 * Confirmation dialog for patient deletion.
 *
 * @param onDismiss Callback when dismissal is requested.
 * @param onDeletePatient Callback when deletion is confirmed.
 */
@Composable
internal fun PatientDeleteConfirmDialog(
    onDismiss: () -> Unit,
    onDeletePatient: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.delete_patient),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(Res.string.delete_patient_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onDeletePatient()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
