package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
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
    val exportedData: String? = null
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
            val newPatient = Patient(
                id = UUID.randomUUID(),
                name = listOf(HumanName(family = lastName, given = listOf(firstName))),
                birthDate = dob,
                mrn = mrn,
                gender = gender,
                managingOrganization = "Local"
            )
            repository.savePatient(newPatient)
            setCreateDialogVisible(false)
            loadPatients()
            onSuccess(newPatient.id)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                val data = exportImportService.exportData()
                _uiState.update { it.copy(exportedData = data) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearExportData() {
        _uiState.update { it.copy(exportedData = null) }
    }

    fun importData(data: String) {
        viewModelScope.launch {
            try {
                exportImportService.importData(data)
                loadPatients()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}