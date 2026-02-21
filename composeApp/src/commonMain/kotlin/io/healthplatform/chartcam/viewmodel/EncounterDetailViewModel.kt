package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.models.Attachment
import io.healthplatform.chartcam.models.DocumentReference
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Period
import io.healthplatform.chartcam.models.Practitioner
import io.healthplatform.chartcam.models.Questionnaire
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * State for the Encounter Detail Screen.
 */
data class EncounterUiState(
    val isLoading: Boolean = true,
    val patient: Patient? = null,
    val practitioner: Practitioner? = null,
    val encounter: Encounter? = null,
    val photos: List<DocumentReference> = emptyList(),
    val notes: String = "",
    val availableQuestionnaires: List<Questionnaire> = emptyList(),
    val selectedQuestionnaire: Questionnaire? = null,
    val isSyncing: Boolean = false,
    val isFinalized: Boolean = false
)

/**
 * ViewModel for viewing and finalizing an Encounter.
 */
class EncounterDetailViewModel(
    private val fhirRepository: FhirRepository,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val questionnaireRepository: QuestionnaireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncounterUiState())
    val uiState: StateFlow<EncounterUiState> = _uiState.asStateFlow()

    /**
     * Initializes the screen.
     * If visitId == "new", creates a draft Encounter. Otherwise, loads existing.
     *
     * @param patientId The selected patient.
     * @param visitId The encounter ID or "new".
     * @param photosMap Map of StepName -> FilePath.
     */
    fun initialize(patientId: String, visitId: String, photosMap: Map<String, String>) {
        if (_uiState.value.patient != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val patient = fhirRepository.getPatient(patientId)
            val practitioner = authRepository.currentUser.value
            val questionnaires = questionnaireRepository.getAvailableQuestionnaires()
            
            if (patient != null && practitioner != null) {
                if (visitId == "new") {
                    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val encounterId = UUID.randomUUID()
                    
                    val newEncounter = Encounter(
                        id = encounterId,
                        status = "in-progress",
                        subjectReference = patient.id,
                        participantReference = practitioner.id,
                        period = Period(start = now),
                        text = ""
                    )
                    
                    fhirRepository.saveEncounter(newEncounter)
                    
                    val docs = photosMap.map { (stepName, path) ->
                        DocumentReference(
                            id = UUID.randomUUID(),
                            subjectReference = patient.id,
                            contextReference = encounterId,
                            date = now,
                            description = stepName,
                            content = Attachment("image/jpeg", path)
                        ).also {
                            fhirRepository.saveDocumentReference(it)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            patient = patient,
                            practitioner = practitioner,
                            encounter = newEncounter,
                            photos = docs,
                            notes = "",
                            availableQuestionnaires = questionnaires,
                            selectedQuestionnaire = questionnaires.firstOrNull()
                        )
                    }
                } else {
                    val existingEncounter = fhirRepository.getEncounter(visitId)
                    val existingDocs = fhirRepository.getPhotosForEncounter(visitId).toMutableList()
                    
                    if (existingEncounter != null) {
                        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val newDocs = photosMap.map { (stepName, path) ->
                            DocumentReference(
                                id = UUID.randomUUID(),
                                subjectReference = patient.id,
                                contextReference = existingEncounter.id,
                                date = now,
                                description = stepName,
                                content = Attachment("image/jpeg", path)
                            ).also {
                                fhirRepository.saveDocumentReference(it)
                            }
                        }
                        existingDocs.addAll(newDocs)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                patient = patient,
                                practitioner = practitioner,
                                encounter = existingEncounter,
                                photos = existingDocs,
                                notes = existingEncounter.text ?: "",
                                availableQuestionnaires = questionnaires,
                                selectedQuestionnaire = questionnaires.firstOrNull()
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Updates local notes state and DB.
     * @param text The new notes text.
     */
    fun onNotesChanged(text: String) {
        _uiState.update { it.copy(notes = text) }
        val enc = _uiState.value.encounter
        if (enc != null) {
            viewModelScope.launch {
                fhirRepository.updateEncounterNotes(enc.id, text)
            }
        }
    }

    /**
     * Sets the active questionnaire form.
     * @param q The selected Questionnaire.
     */
    fun selectQuestionnaire(q: Questionnaire) {
        _uiState.update { it.copy(selectedQuestionnaire = q) }
    }

    /**
     * Creates a custom questionnaire and sets it as active.
     * @param title The title of the form.
     * @param photosCount Number of photos.
     */
    fun createAndSelectQuestionnaire(title: String, photosCount: Int) {
        val q = questionnaireRepository.createQuestionnaire(title, photosCount)
        _uiState.update { 
            it.copy(
                availableQuestionnaires = questionnaireRepository.getAvailableQuestionnaires(),
                selectedQuestionnaire = q
            )
        }
    }

    /**
     * Finalizes the encounter and triggers Sync.
     */
    fun finalizeEncounter() {
        val enc = _uiState.value.encounter ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            fhirRepository.updateEncounterStatus(enc.id, "finished")
            
            val success = syncManager.syncEncounter(enc.id)
            
            _uiState.update { 
                it.copy(
                    isSyncing = false, 
                    isFinalized = success
                ) 
            }
        }
    }
    
    /**
     * Resets finalized state after navigation.
     */
    fun resetFinalized() {
        _uiState.update { it.copy(isFinalized = false) }
    }
}
