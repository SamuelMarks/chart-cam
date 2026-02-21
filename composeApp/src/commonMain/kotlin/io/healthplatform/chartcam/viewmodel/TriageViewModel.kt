package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * State for the Triage Screen.
 *
 * @property capturedPhotoPaths Map of PhotoStep Name to File Path.
 * @property searchQuery Current input in search bar.
 * @property searchResults List of patients matching query.
 * @property isCreatingPatient Whether the create dialog is open.
 */
data class TriageUiState(
    val capturedPhotoPaths: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Patient> = emptyList(),
    val isCreatingPatient: Boolean = false,
    val selectedPatient: Patient? = null
)

/**
 * ViewModel for Triage Logic.
 *
 * @property fhirRepository Data access for Patients.
 */
class TriageViewModel(
    private val fhirRepository: FhirRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageUiState())
    val uiState: StateFlow<TriageUiState> = _uiState.asStateFlow()

    /**
     * Initializes the view with captured photos.
     *
     * @param photoPathsJson JSON string representation of the map (simplified for prompt as map).
     * In real app, we parse JSON. Here we assume we receive the Map or parse manually.
     * For this step, we will assume the paths are passed into [setPaths] manually by the UI for simplicity or parsed.
     */
    fun setPaths(map: Map<String, String>) {
        _uiState.update { it.copy(capturedPhotoPaths = map) }
    }

    /**
     * Updates search query and fetches results.
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
     * Selects a patient to proceed to Encounter.
     */
    fun selectPatient(patient: Patient) {
        _uiState.update { it.copy(selectedPatient = patient) }
    }

    /**
     * Toggles Create Patient dialog.
     */
    fun showCreatePatient(show: Boolean) {
        _uiState.update { it.copy(isCreatingPatient = show) }
    }

    /**
     * Creates a new patient and selects them.
     */
    fun createPatient(firstName: String, lastName: String, mrn: String, dob: LocalDate, gender: String) {
        viewModelScope.launch {
            val newPatient = Patient(
                id = UUID.randomUUID(),
                name = listOf(HumanName(family = lastName, given = listOf(firstName))),
                birthDate = dob,
                mrn = mrn,
                gender = gender,
                managingOrganization = "Local"
            )
            fhirRepository.savePatient(newPatient)
            selectPatient(newPatient)
            showCreatePatient(false)
        }
    }
}