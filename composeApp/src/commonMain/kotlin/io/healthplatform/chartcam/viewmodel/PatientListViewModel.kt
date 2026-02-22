package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class PatientListUiState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val isCreatingPatient: Boolean = false,
    val isLoading: Boolean = false,
    val exportedData: String? = null,
    val exportPassword: String? = null,
    val error: String? = null
)

class PatientListViewModel(
    private val repository: FhirRepository,
    private val exportImportService: ExportImportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientListUiState(isLoading = true))
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    init {
        loadPatients()
    }

    fun loadPatients() {
        val query = _uiState.value.searchQuery
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val results = if (query.isBlank()) {
                repository.getAllPatients()
            } else {
                repository.searchPatients(query)
            }
            _uiState.update { it.copy(patients = results, isLoading = false) }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        loadPatients()
    }

    fun setCreateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreatingPatient = visible) }
    }

    fun createPatient(
        firstName: String, 
        lastName: String, 
        mrn: String, 
        dob: LocalDate, 
        gender: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newPatient = createFhirPatient(
                id = UUID.randomUUID(),
                firstName = firstName,
                lastName = lastName,
                dob = dob,
                mrnValue = mrn,
                genderStr = gender
            )
            repository.savePatient(newPatient)
            setCreateDialogVisible(false)
            loadPatients()
            onSuccess(newPatient.id ?: "")
        }
    }

    fun exportData(password: String) {
        viewModelScope.launch {
            try {
                val data = exportImportService.exportData(password)
                _uiState.update { it.copy(exportedData = data, exportPassword = password) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearExportData() {
        _uiState.update { it.copy(exportedData = null, exportPassword = null) }
    }

    fun importData(data: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                exportImportService.importData(data, password)
                loadPatients()
                _uiState.update { it.copy(error = null) }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = "Failed to import. Wrong password or bad data.") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
