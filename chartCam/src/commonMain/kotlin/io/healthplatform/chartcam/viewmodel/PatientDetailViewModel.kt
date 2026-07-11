/**
 * ViewModel and UI state definitions for the Patient Detail screen.
 * This file handles logic for loading and managing patient data and encounters.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State definition for the Patient Detail Screen.
 *
 * @property patient The FHIR Patient object being displayed.
 * @property encounters The list of FHIR Encounters associated with the patient.
 * @property isLoading Flag indicating whether the patient details are currently being loaded.
 */
data class PatientDetailUiState(
    val patient: Patient? = null,
    val encounters: List<Encounter> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * ViewModel handling the business logic for the Patient Detail Screen.
 * Bridges the UI events to the [FhirRepository].
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models (e.g., `Patient`, `Encounter`) without relying on intermediary DTOs.
 *
 * @property fhirRepository The repository providing FHIR data access.
 */
class PatientDetailViewModel(
    private val fhirRepository: FhirRepository,
) : ViewModel() {
    /**
     * Internal mutable state flow for the patient detail UI state.
     */
    private val _uiState = MutableStateFlow(PatientDetailUiState(isLoading = true))

    /**
     * Public immutable state flow for the patient detail UI state.
     */
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads patient data and associated encounters by patient ID.
     * Updates the UI state with the fetched data.
     *
     * @param patientId The unique identifier of the patient to load.
     */
    fun loadPatientData(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val patient = fhirRepository.getPatient(patientId)
            val encounters = fhirRepository.getEncountersForPatient(patientId)
            _uiState.update { it.copy(patient = patient, encounters = encounters, isLoading = false) }
        }
    }

    /**
     * Deletes the currently loaded patient from the repository.
     *
     * @param onSuccess Callback triggered when the patient is successfully deleted.
     */
    fun deletePatient(onSuccess: () -> Unit) {
        val patientId = _uiState.value.patient?.id ?: return
        viewModelScope.launch {
            fhirRepository.deletePatient(patientId)
            onSuccess()
        }
    }
}
