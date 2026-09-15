/**
 * @file TriageViewModel.kt
 * Contains declarations for TriageViewModel.kt.
 *
 * ViewModel and UI state definitions for the Triage screen.
 * This file handles logic for associating photos with patients in a triage workflow.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * UI State definition for the Triage Screen.
 *
 * @param capturedPhotoPaths A map holding paths of captured photos, mapping keys to URIs/paths.
 * @param searchQuery The current query used to search for a patient.
 * @param searchResults The list of patients matching the current search query.
 * @param isCreatingPatient Flag indicating if the create patient dialog is visible.
 * @param selectedPatient The currently selected patient to associate with the photos.
 * @param selectedPhotoKeys Set of photo keys currently selected for association or deletion.
 */
data class TriageUiState(
    val capturedPhotoPaths: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Patient> = emptyList(),
    val isCreatingPatient: Boolean = false,
    val selectedPatient: Patient? = null,
    val selectedPhotoKeys: Set<String> = emptySet(),
)

/**
 * ViewModel handling the business logic for the Triage Screen.
 * Bridges UI events to the [FhirRepository]. This ViewModel directly consumes
 * and emits native FHIR R4 `Resource` models (e.g., Patient, Encounter)
 * without relying on intermediary DTOs.
 *
 * @param fhirRepository The repository providing FHIR data access.
 */
class TriageViewModel(
    private val fhirRepository: FhirRepository,
) : ViewModel() {
    /**
     * Internal mutable state flow for the triage UI state.
     */
    private val _uiState = MutableStateFlow(TriageUiState())

    /**
     * Public immutable state flow for the triage UI state.
     */
    val uiState: StateFlow<TriageUiState> = _uiState.asStateFlow()

    /**
     * Sets the captured photo paths to be associated with a patient.
     *
     * @param map A map containing the photo keys and their corresponding paths.
     */
    fun setPaths(map: Map<String, String>) {
        _uiState.update { it.copy(capturedPhotoPaths = map) }
    }

    /**
     * Toggles selection of a specific photo key in the triage batch.
     *
     * @param key The photo key to toggle.
     */
    fun togglePhotoSelection(key: String) {
        _uiState.update { current ->
            val updated = current.selectedPhotoKeys.toMutableSet()
            if (updated.contains(key)) {
                updated.remove(key)
            } else {
                updated.add(key)
            }
            current.copy(selectedPhotoKeys = updated)
        }
    }

    /**
     * Selects all photos currently in the batch.
     */
    fun selectAllPhotos() {
        _uiState.update { current ->
            current.copy(selectedPhotoKeys = current.capturedPhotoPaths.keys)
        }
    }

    /**
     * Clears all selected photo keys.
     */
    fun clearSelection() {
        _uiState.update { current ->
            current.copy(selectedPhotoKeys = emptySet())
        }
    }

    /**
     * Deletes the currently selected photos from the active batch and disk storage.
     *
     * @param fileStorage Optional file storage to delete the physical image files.
     * @return A [Result] indicating success of the deletion operation.
     */
    fun deleteSelectedPhotos(fileStorage: FileStorage? = null): Result<Unit> =
        runCatching {
            val toDelete = _uiState.value.selectedPhotoKeys
            if (toDelete.isEmpty()) return@runCatching

            val updatedPaths = _uiState.value.capturedPhotoPaths.toMutableMap()
            toDelete.forEach { key ->
                val path = updatedPaths.remove(key)
                if (path != null && fileStorage != null) {
                    fileStorage.deleteImage(path)
                }
            }
            _uiState.update {
                it.copy(
                    capturedPhotoPaths = updatedPaths,
                    selectedPhotoKeys = emptySet(),
                )
            }
        }

    /**
     * Confirms association of selected photos (or all photos if none selected) with the target patient.
     *
     * @param onComplete Callback invoked with the remaining unassociated photo paths.
     * @return A [Result] enclosing the associated photos map.
     */
    fun confirmAssociation(onComplete: (Map<String, String>) -> Unit): Result<Map<String, String>> =
        runCatching {
            val selected = _uiState.value.selectedPhotoKeys
            val all = _uiState.value.capturedPhotoPaths
            val toAssign = if (selected.isNotEmpty()) all.filterKeys { selected.contains(it) } else all
            val remaining = all.filterKeys { !toAssign.containsKey(it) }
            _uiState.update {
                it.copy(
                    capturedPhotoPaths = remaining,
                    selectedPhotoKeys = emptySet(),
                )
            }
            onComplete(toAssign)
            toAssign
        }

    /**
     * Updates the search query and loads search results.
     * Search triggers if query is not blank.
     *
     * @param query The new search query string.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isNotEmpty()) {
                val results = fhirRepository.searchPatients(trimmed)
                _uiState.update { it.copy(searchResults = results) }
            } else {
                _uiState.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    /**
     * Selects a patient from the search results to associate with the photos.
     *
     * @param patient The [Patient] that was selected.
     */
    fun selectPatient(patient: Patient) {
        _uiState.update { it.copy(selectedPatient = patient) }
    }

    /**
     * Toggles the visibility of the create patient dialog.
     *
     * @param show True to display the dialog, false to hide it.
     */
    fun showCreatePatient(show: Boolean) {
        _uiState.update { it.copy(isCreatingPatient = show) }
    }

    /**
     * Creates a new FHIR Patient and saves it to the repository.
     * Upon creation, sets the new patient as the selected patient.
     *
     * @param firstName The patient's first name.
     * @param lastName The patient's last name.
     * @param mrn The patient's medical record number.
     * @param dob The patient's date of birth.
     * @param gender The patient's gender.
     */
    fun createPatient(
        firstName: String,
        lastName: String,
        mrn: String,
        dob: LocalDate,
        gender: String,
    ) {
        viewModelScope.launch {
            val newPatient =
                createFhirPatient(
                    id = UUID.randomUUID(),
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    mrnValue = mrn,
                    gender = gender,
                )
            fhirRepository.savePatient(newPatient)
            selectPatient(newPatient)
            showCreatePatient(false)
        }
    }
}
