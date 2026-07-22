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
 */
data class TriageUiState(
    val capturedPhotoPaths: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Patient> = emptyList(),
    val isCreatingPatient: Boolean = false,
    val selectedPatient: Patient? = null,
)

/**
 * ViewModel handling the business logic for the Triage Screen.
 * Bridges UI events to the [FhirRepository]. This ViewModel directly consumes and emits native FHIR R4 `Resource` models (e.g., `Patient`, `Encounter`) without relying on intermediary DTOs.
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
     * Updates the search query and loads search results.
     * Search triggers if query length is greater than 1.
     *
     * @param query The new search query string.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.length > 1) {
                val results = fhirRepository.searchPatients(query)
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
                    genderStr = gender,
                )
            fhirRepository.savePatient(newPatient)
            selectPatient(newPatient)
            showCreatePatient(false)
        }
    }
}
