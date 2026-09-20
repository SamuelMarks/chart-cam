/**
 * @file PatientListViewModel.kt
 * Contains declarations for PatientListViewModel.kt.
 *
 * ViewModel and UI state definitions for the Patient List screen.
 * This file handles searching, creating, and exporting patients.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.failed_to_import
import chartcam.chartcam.generated.resources.failed_to_load_patients
import chartcam.chartcam.generated.resources.unknown_error
import dev.ohs.fhir.model.r4.Patient
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.ImportPreviewSummary
import io.healthplatform.chartcam.models.createFhirPatientCatching
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import io.healthplatform.chartcam.utils.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/**
 * Stages in the interactive import and conflict resolution pipeline.
 */
enum class ImportStage {
    /** No import currently in progress. */
    IDLE,

    /** Archive decrypted and staged for review. */
    PREVIEW_STAGED,

    /** Ingestion in progress. */
    IMPORTING,

    /** Ingestion completed successfully. */
    SUCCESS,

    /** Ingestion failed. */
    ERROR,
}

/**
 * Staged import payload bundling the encrypted archive data and password.
 *
 * @param data The raw encrypted data string.
 * @param password The decryption password.
 */
data class StagedImportPayload(
    val data: String,
    val password: String,
)

/**
 * UI State definition for the Patient List Screen.
 *
 * @param patients The list of patients currently being displayed.
 * @param searchQuery The current query used to filter patients.
 * @param isCreatingPatient Flag indicating if the create patient dialog is visible.
 * @param isLoading Flag indicating whether patients are currently being loaded.
 * @param exportedData The JSON string containing exported patient data, if an export occurred.
 * @param exportPassword The password used for the exported data.
 * @param error An error message to display if an operation fails.
 * @param showAllPatients Flag indicating whether to show all patients or just the current practitioner's patients.
 * @param importStage Current phase of the import and conflict resolution pipeline.
 * @param importPreview Staged archive preview metadata, or null if no archive is staged.
 * @param stagedPayload The currently staged import payload bundle, or null if no archive is staged.
 * @param importFilterOptions Category filter configuration for selective import.
 * @param selectedPatientIds The set of staged incoming patient IDs selected for import.
 * @param conflictResolutions Chosen conflict resolution strategies keyed by incoming patient ID.
 */
data class PatientListUiState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val isCreatingPatient: Boolean = false,
    val isLoading: Boolean = false,
    val exportedData: String? = null,
    val exportPassword: String? = null,
    val error: StringResource? = null,
    val showAllPatients: Boolean = false,
    val importStage: ImportStage = ImportStage.IDLE,
    val importPreview: ImportPreviewSummary? = null,
    val stagedPayload: StagedImportPayload? = null,
    val importFilterOptions: ImportFilterOptions = ImportFilterOptions.all(),
    val selectedPatientIds: Set<String> = emptySet(),
    val conflictResolutions: Map<String, ConflictResolutionStrategy> = emptyMap(),
)

/**
 * ViewModel handling the business logic for the Patient List Screen.
 * Bridges the UI events to the [FhirRepository] and [ExportImportService].
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models
 * (e.g., Patient) without relying on intermediary DTOs.
 *
 * @param repository The source of FHIR patient data.
 * @param exportImportService Service to handle exporting and importing of data.
 * @param authRepository The source of authentication truth, used to get the current practitioner.
 */
class PatientListViewModel(
    private val repository: FhirRepository,
    private val exportImportService: ExportImportService,
    private val authRepository: AuthRepository,
) : ViewModel() {
    /**
     * Internal mutable state flow for the patient list UI state.
     */
    private val _uiState = MutableStateFlow(PatientListUiState(isLoading = true))

    /**
     * Public immutable state flow for the patient list UI state.
     */
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    init {
        loadPatients()
    }

    /**
     * Loads patients from the repository based on the current search query and showAll toggle.
     */
    fun loadPatients() {
        val query = _uiState.value.searchQuery
        val showAll = _uiState.value.showAllPatients
        val practitionerId = authRepository.currentUser.value?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result =
                if (query.isBlank()) {
                    repository.getAllPatientsCatching(showAll = showAll, practitionerId = practitionerId)
                } else {
                    repository.searchPatientsCatching(query, showAll = showAll, practitionerId = practitionerId)
                }
            result
                .onSuccess { results ->
                    _uiState.update { it.copy(patients = results.orEmpty(), isLoading = false) }
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(isLoading = false, error = Res.string.failed_to_load_patients) }
                }
        }
    }

    /**
     * Updates the search query and reloads the patients.
     *
     * @param newQuery The new search query string.
     */
    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        loadPatients()
    }

    /**
     * Updates the toggle for showing all patients vs only the current practitioner's.
     *
     * @param showAll Boolean indicating whether to show all patients.
     */
    fun setShowAllPatients(showAll: Boolean) {
        _uiState.update { it.copy(showAllPatients = showAll) }
        loadPatients()
    }

    /**
     * Toggles the visibility of the create patient dialog.
     *
     * @param visible True to show the dialog, false to hide it.
     */
    fun setCreateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreatingPatient = visible) }
    }

    /**
     * Creates a new FHIR Patient and saves it to the repository.
     *
     * @param firstName The patient's first name.
     * @param lastName The patient's last name.
     * @param mrn The patient's medical record number.
     * @param dob The patient's date of birth.
     * @param gender The patient's gender.
     * @param onSuccess Callback triggered when the patient is successfully created, providing the new patient ID.
     */
    fun createPatient(
        firstName: String,
        lastName: String,
        mrn: String,
        dob: LocalDate,
        gender: String = "unknown",
        onSuccess: (String) -> Unit,
    ) {
        val practitionerId = authRepository.currentUser.value?.id
        val newId = UUID.randomUUID()
        viewModelScope.launch {
            val patientResult =
                createFhirPatientCatching(
                    id = newId,
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    mrnValue = mrn,
                    organizationId = practitionerId,
                    gender = gender,
                )
            val newPatient =
                patientResult.getOrElse {
                    _uiState.update { it.copy(error = Res.string.failed_to_load_patients) }
                    return@launch
                }
            repository
                .savePatient(newPatient)
                .onSuccess {
                    setCreateDialogVisible(false)
                    loadPatients()
                    onSuccess(newId)
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(error = Res.string.failed_to_load_patients) }
                }
        }
    }

    /**
     * Exports the data for the current practitioner or all practitioners.
     *
     * @param password The password used to encrypt the exported data.
     * @param exportAll Boolean indicating if all data should be exported.
     */
    fun exportData(
        password: String,
        exportAll: Boolean,
    ) {
        val practitionerId = authRepository.currentUser.value?.id
        viewModelScope.launch {
            exportImportService
                .exportData(password, exportAll, practitionerId)
                .onSuccess { data ->
                    _uiState.update { it.copy(exportedData = data, exportPassword = password, error = null) }
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(error = Res.string.unknown_error) }
                }
        }
    }

    /**
     * Clears the currently held exported data and password from the state.
     */
    fun clearExportData() {
        _uiState.update { it.copy(exportedData = null, exportPassword = null) }
    }

    /**
     * Imports data into the application.
     *
     * @param data The encrypted string of data to import.
     * @param password The password used to decrypt the data.
     * @param onSuccess Callback triggered on a successful import.
     */
    fun importData(
        data: String,
        password: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            exportImportService
                .importData(data, password)
                .onSuccess {
                    loadPatients()
                    _uiState.update { it.copy(error = null) }
                    onSuccess()
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(error = Res.string.failed_to_import) }
                }
        }
    }

    /**
     * Stages an import archive for preview, conflict detection, and batch selection.
     *
     * @param data The encrypted bundle string.
     * @param password The decryption password.
     */
    fun stageImport(
        data: String,
        password: String,
    ) {
        viewModelScope.launch {
            exportImportService
                .inspectArchive(data, password)
                .onSuccess { preview ->
                    val allPatientIds = preview.stagedPatients.mapNotNull { it.incomingPatient.id }.toSet()
                    val initialResolutions =
                        preview.stagedPatients.associate {
                            val id = it.incomingPatient.id ?: ""
                            id to it.resolutionStrategy
                        }
                    _uiState.update {
                        it.copy(
                            importStage = ImportStage.PREVIEW_STAGED,
                            importPreview = preview,
                            stagedPayload = StagedImportPayload(data, password),
                            selectedPatientIds = allPatientIds,
                            conflictResolutions = initialResolutions,
                            error = null,
                        )
                    }
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(importStage = ImportStage.ERROR, error = Res.string.failed_to_import) }
                }
        }
    }

    /**
     * Toggles inclusion of an individual patient in the staged import batch.
     *
     * @param patientId The ID of the patient to toggle.
     * @param selected True if selected, false otherwise.
     */
    fun togglePatientSelection(
        patientId: String,
        selected: Boolean,
    ) {
        _uiState.update { current ->
            val updated = current.selectedPatientIds.toMutableSet()
            if (selected) updated.add(patientId) else updated.remove(patientId)
            current.copy(selectedPatientIds = updated)
        }
    }

    /**
     * Toggles inclusion of all staged patients.
     *
     * @param selected True to select all, false to deselect all.
     */
    fun toggleSelectAllPatients(selected: Boolean) {
        _uiState.update { current ->
            val preview = current.importPreview
            val allIds =
                if (selected && preview != null) {
                    preview.stagedPatients.mapNotNull { it.incomingPatient.id }.toSet()
                } else {
                    emptySet()
                }
            current.copy(selectedPatientIds = allIds)
        }
    }

    /**
     * Toggles whether a specific data category should be imported.
     *
     * @param category The category to toggle.
     * @param enabled True to include, false to exclude.
     */
    fun toggleCategory(
        category: ImportCategory,
        enabled: Boolean,
    ) {
        _uiState.update { current ->
            val cats = current.importFilterOptions.enabledCategories.toMutableSet()
            if (enabled) cats.add(category) else cats.remove(category)
            current.copy(importFilterOptions = current.importFilterOptions.copy(enabledCategories = cats))
        }
    }

    /**
     * Sets the resolution strategy for a conflicting patient.
     *
     * @param resourceId The conflicting patient ID.
     * @param strategy The chosen resolution strategy.
     */
    fun setConflictResolution(
        resourceId: String,
        strategy: ConflictResolutionStrategy,
    ) {
        _uiState.update { current ->
            val resolutions = current.conflictResolutions.toMutableMap()
            resolutions[resourceId] = strategy
            current.copy(conflictResolutions = resolutions)
        }
    }

    /**
     * Confirms and executes the staged import with chosen selections and resolutions.
     *
     * @param onSuccess Callback triggered upon successful completion.
     */
    fun confirmAndExecuteImport(onSuccess: () -> Unit) {
        val payload = _uiState.value.stagedPayload ?: return
        val filter = _uiState.value.importFilterOptions
        val selectedIds = _uiState.value.selectedPatientIds
        val resolutions = _uiState.value.conflictResolutions

        viewModelScope.launch {
            _uiState.update { it.copy(importStage = ImportStage.IMPORTING) }
            exportImportService
                .importDataSelective(
                    encryptedData = payload.data,
                    password = payload.password,
                    filterOptions = filter,
                    selectedPatientIds = selectedIds,
                    resolutionMap = resolutions,
                ).onSuccess {
                    loadPatients()
                    _uiState.update {
                        it.copy(
                            importStage = ImportStage.SUCCESS,
                            importPreview = null,
                            stagedPayload = null,
                            error = null,
                        )
                    }
                    onSuccess()
                }.onFailure { e ->
                    println(e.message)
                    _uiState.update { it.copy(importStage = ImportStage.ERROR, error = Res.string.failed_to_import) }
                }
        }
    }

    /**
     * Cancels the staged import and resets import state to IDLE.
     */
    fun cancelImport() {
        _uiState.update {
            it.copy(
                importStage = ImportStage.IDLE,
                importPreview = null,
                stagedPayload = null,
            )
        }
    }

    /**
     * Clears the current error message from the state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Extracts reference string value from a FHIR Reference element.
     *
     * @param ref The reference element.
     * @return The raw reference value, or null.
     */
    private fun extractReferenceValue(ref: dev.ohs.fhir.model.r4.Reference?): String? {
        val r = if (ref != null) ref.reference else null
        return if (r != null) r.value else null
    }

    /**
     * Deletes the clinician account, cascading deletions to practitioner resources.
     *
     * @param onSuccess Callback triggered upon successful deletion.
     */
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val practitioner = authRepository.currentUser.value
            if (practitioner != null) {
                val names = practitioner.name
                val username =
                    if (names.isNotEmpty()) {
                        val fam = names[0].family
                        if (fam != null && fam.value != null) fam.value!! else ""
                    } else {
                        ""
                    }
                val rawId = practitioner.id
                val id = if (rawId != null) rawId else ""

                // Delete all encounters associated with this practitioner,
                // and delete patients solely if no other practitioner holds encounters on that patient
                val allPatients = repository.getAllPatients(showAll = false, practitionerId = id)
                allPatients.forEach { patient ->
                    val pid = patient.id ?: return@forEach
                    val encounters =
                        runSuspendCatching {
                            repository.getEncountersForPatient(pid)
                        }.getOrDefault(emptyList())
                    val otherPractitionerEncounters =
                        encounters.filter { enc ->
                            enc.participant.any { p ->
                                val ref = extractReferenceValue(p.individual)
                                ref != null && !ref.contains(id)
                            }
                        }
                    if (otherPractitionerEncounters.isNotEmpty()) {
                        val myEncounters =
                            encounters.filter { enc ->
                                enc.participant.any { p ->
                                    val ref = extractReferenceValue(p.individual)
                                    ref != null && ref.contains(id)
                                }
                            }
                        myEncounters.forEach { enc ->
                            val encId = enc.id
                            if (encId != null) {
                                repository.deleteEncounter(encId)
                            }
                        }
                    } else {
                        repository.deletePatient(pid)
                    }
                }

                repository.deletePractitioner(id)
                authRepository.deleteAccount(username)
                onSuccess()
            }
        }
    }
}
