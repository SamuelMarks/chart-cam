package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientDetailUiState(
    val patient: Patient? = null,
    val encounters: List<Encounter> = emptyList(),
    val isLoading: Boolean = false
)

class PatientDetailViewModel(
    private val fhirRepository: FhirRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDetailUiState(isLoading = true))
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    fun loadPatientData(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val patient = fhirRepository.getPatient(patientId)
            // To get encounters, we need a method in the repository. Wait, `ChartCam.sq` has `getEncountersForPatient`
            // Let's add it to `FhirRepository` if it's missing, or we can just call it
            val encounters = fhirRepository.getEncountersForPatient(patientId)
            _uiState.update { it.copy(patient = patient, encounters = encounters, isLoading = false) }
        }
    }
}