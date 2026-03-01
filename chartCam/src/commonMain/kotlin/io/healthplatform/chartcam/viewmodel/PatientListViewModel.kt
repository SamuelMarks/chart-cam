package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.AuthRepository
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
    val error: String? = null,
    val showAllPatients: Boolean = false
)

class PatientListViewModel(
    private val repository: FhirRepository,
    private val exportImportService: ExportImportService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientListUiState(isLoading = true))
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    init {
        loadPatients()
    }

    fun loadPatients() {
        val query = _uiState.value.searchQuery
        val showAll = _uiState.value.showAllPatients
        val practitionerId = authRepository.currentUser.value?.id
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val results = if (query.isBlank()) {
                repository.getAllPatients(showAll = showAll, practitionerId = practitionerId)
            } else {
                repository.searchPatients(query, showAll = showAll, practitionerId = practitionerId)
            }
            _uiState.update { it.copy(patients = results, isLoading = false) }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        loadPatients()
    }

    fun setShowAllPatients(showAll: Boolean) {
        _uiState.update { it.copy(showAllPatients = showAll) }
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

    fun exportData(password: String, exportAll: Boolean) {
        val practitionerId = authRepository.currentUser.value?.id
        viewModelScope.launch {
            try {
                val data = exportImportService.exportData(password, exportAll, practitionerId)
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

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val practitioner = authRepository.currentUser.value
            if (practitioner != null) {
                val username = practitioner.name.firstOrNull()?.family?.value ?: ""
                val id = practitioner.id ?: ""
                
                // Delete all patients associated with this practitioner
                // which will cascade delete their visits, photos, and notes
                val allPatients = repository.getAllPatients(showAll = false, practitionerId = id)
                allPatients.forEach { patient -> 
                    patient.id?.let { repository.deletePatient(it) }
                }
                
                repository.deletePractitioner(id)
                authRepository.deleteAccount(username)
                onSuccess()
            }
        }
    }
}

