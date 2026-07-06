/**
 * ViewModel and UI state definitions for the Patient List screen.
 * This file handles searching, creating, and exporting patients.
 */
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

/**
 * UI State definition for the Patient List Screen.
 *
 * @property patients The list of patients currently being displayed.
 * @property searchQuery The current query used to filter patients.
 * @property isCreatingPatient Flag indicating if the create patient dialog is visible.
 * @property isLoading Flag indicating whether patients are currently being loaded.
 * @property exportedData The JSON string containing exported patient data, if an export occurred.
 * @property exportPassword The password used for the exported data.
 * @property error An error message to display if an operation fails.
 * @property showAllPatients Flag indicating whether to show all patients or just the current practitioner's patients.
 */
data class PatientListUiState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val isCreatingPatient: Boolean = false,
    val isLoading: Boolean = false,
    val exportedData: String? = null,
    val exportPassword: String? = null,
    val error: String? = null,
    val showAllPatients: Boolean = false,
)

/**
 * ViewModel handling the business logic for the Patient List Screen.
 * Bridges the UI events to the [FhirRepository] and [ExportImportService].
 *
 * @property repository The source of FHIR patient data.
 * @property exportImportService Service to handle exporting and importing of data.
 * @property authRepository The source of authentication truth, used to get the current practitioner.
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
            _uiState.update { it.copy(isLoading = true) }
            val results =
                if (query.isBlank()) {
                    repository.getAllPatients(showAll = showAll, practitionerId = practitionerId)
                } else {
                    repository.searchPatients(query, showAll = showAll, practitionerId = practitionerId)
                }
            _uiState.update { it.copy(patients = results, isLoading = false) }
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
        gender: String,
        onSuccess: (String) -> Unit,
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
            repository.savePatient(newPatient)
            setCreateDialogVisible(false)
            loadPatients()
            onSuccess(newPatient.id ?: "")
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
            try {
                val data = exportImportService.exportData(password, exportAll, practitionerId)
                _uiState.update { it.copy(exportedData = data, exportPassword = password) }
            } catch (e: Exception) {
                e.printStackTrace()
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

    /**
     * Clears the current error message from the state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Deletes the currently logged-in practitioner's account and all associated patients.
     *
     * @param onSuccess Callback triggered after successful deletion.
     */
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val practitioner = authRepository.currentUser.value
            if (practitioner != null) {
                val username =
                    practitioner.name
                        .firstOrNull()
                        ?.family
                        ?.value ?: ""
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
