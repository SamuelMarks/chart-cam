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

data class TriageUiState(
    val capturedPhotoPaths: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Patient> = emptyList(),
    val isCreatingPatient: Boolean = false,
    val selectedPatient: Patient? = null
)

class TriageViewModel(
    private val fhirRepository: FhirRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageUiState())
    val uiState: StateFlow<TriageUiState> = _uiState.asStateFlow()

    fun setPaths(map: Map<String, String>) {
        _uiState.update { it.copy(capturedPhotoPaths = map) }
    }

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

    fun selectPatient(patient: Patient) {
        _uiState.update { it.copy(selectedPatient = patient) }
    }

    fun showCreatePatient(show: Boolean) {
        _uiState.update { it.copy(isCreatingPatient = show) }
    }

    fun createPatient(firstName: String, lastName: String, mrn: String, dob: LocalDate, gender: String) {
        viewModelScope.launch {
            val newPatient = createFhirPatient(
                id = UUID.randomUUID(),
                firstName = firstName,
                lastName = lastName,
                dob = dob,
                mrnValue = mrn,
                genderStr = gender
            )
            fhirRepository.savePatient(newPatient)
            selectPatient(newPatient)
            showCreatePatient(false)
        }
    }
}
