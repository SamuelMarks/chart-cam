package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse

import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Enumeration
import io.healthplatform.chartcam.models.createFhirProvenance
import io.healthplatform.chartcam.models.createFhirDevice
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirDocumentReference
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

/**
 * State for the Encounter Detail Screen.
 * Contains patient details, captured photos, and the state of the active questionnaire form.
 */
data class EncounterUiState(
    /** Indicates if initial data is being loaded. */
    val isLoading: Boolean = true,
    /** The patient context for this encounter. */
    val patient: Patient? = null,
    /** The currently authenticated practitioner. */
    val practitioner: Practitioner? = null,
    /** The FHIR Encounter resource being modified. */
    val encounter: Encounter? = null,
    /** The captured clinical photos. */
    val photos: List<DocumentReference> = emptyList(),
    /** Deprecated but kept for compatibility. */
    val notes: String = "", 
    /** A map of questionnaire linkId to dynamic answer (String, Boolean, etc.). */
    val answers: Map<String, Any> = emptyMap(),
    /** The list of available questionnaires. */
    val availableQuestionnaires: List<Questionnaire> = emptyList(),
    /** The currently selected questionnaire. */
    val selectedQuestionnaire: Questionnaire? = null,
    /** Indicates if data is currently syncing. */
    val isSyncing: Boolean = false,
    /** Flag to signal the UI that finalization and syncing is complete. */
    val isFinalized: Boolean = false
)

/**
 * ViewModel for viewing and finalizing an Encounter.
 * Handles loading existing encounters, recording form answers dynamically,
 * taking clinical photos, and persisting responses to FHIR JSON and server.
 */
class EncounterDetailViewModel(
    private val fhirRepository: FhirRepository,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val questionnaireRepository: QuestionnaireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncounterUiState())
    /** Exposes the immutable UI state. */
    val uiState: StateFlow<EncounterUiState> = _uiState.asStateFlow()

    /**
     * Initializes the view model by creating a new encounter or loading an existing one.
     * @param patientId ID of the patient.
     * @param visitId ID of the visit (or "new").
     * @param photosMap Incoming captured photos.
     */
    fun initialize(patientId: String, visitId: String, photosMap: Map<kotlin.String, kotlin.String>) {
        if (_uiState.value.patient != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val patient = fhirRepository.getPatient(patientId)
            val practitioner = authRepository.currentUser.value
            val questionnaires = questionnaireRepository.getAvailableQuestionnaires()
            
            if (patient != null && practitioner != null) {
                if (visitId == "new") {
                    val now = kotlin.time.Clock.System.now()
                    val encounterId = UUID.randomUUID()
                    
                    val newEncounter = createFhirEncounter(
                        id = encounterId,
                        patientId = patient.id ?: "",
                        practitionerId = practitioner.id ?: "",
                        dateStr = now.toString(),
                        statusStr = "in-progress"
                    )
                    
                    fhirRepository.saveEncounter(newEncounter)
                    
                    val docs = photosMap.map { (stepName, path) ->
                        createFhirDocumentReference(
                            id = UUID.randomUUID(),
                            patientId = patient.id ?: "",
                            encounterId = encounterId,
                            dateStr = now.toString(),
                            desc = stepName,
                            mime = "image/jpeg",
                            urlPath = path
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
                            answers = emptyMap(),
                            availableQuestionnaires = questionnaires,
                            selectedQuestionnaire = questionnaires.firstOrNull()
                        )
                    }
                } else {
                    val existingEncounter = fhirRepository.getEncounter(visitId)
                    val existingDocs = fhirRepository.getPhotosForEncounter(visitId).toMutableList()
                    val existingResponses = fhirRepository.getQuestionnaireResponsesForEncounter(visitId)
                    
                    var existingAnswers = mutableMapOf<String, Any>()
                    var existingSelectedQ: Questionnaire? = null
                    
                    if (existingResponses.isNotEmpty()) {
                        val latestQr = existingResponses.first()
                        existingSelectedQ = questionnaires.find { it.id == latestQr.questionnaire?.value }
                        
                        latestQr.item.forEach { item ->
                            val linkId = item.linkId.value ?: return@forEach
                            val answer = item.answer.firstOrNull()?.value
                            if (answer != null) {
                                when (answer) {
                                    is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String -> {
                                        val v = answer.value.value
                                        if (v != null) existingAnswers[linkId] = v
                                    }
                                    is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Boolean -> {
                                        val v = answer.value.value
                                        if (v != null) existingAnswers[linkId] = v
                                    }
                                    is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment -> {
                                        // handled in photos
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    
                    if (existingEncounter != null) {
                        val now = kotlin.time.Clock.System.now()
                        val newDocs = photosMap.map { (stepName, path) ->
                            createFhirDocumentReference(
                                id = UUID.randomUUID(),
                                patientId = patient.id ?: "",
                                encounterId = existingEncounter.id ?: "",
                                dateStr = now.toString(),
                                desc = stepName,
                                mime = "image/jpeg",
                                urlPath = path
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
                                answers = existingAnswers,
                                availableQuestionnaires = questionnaires,
                                selectedQuestionnaire = existingSelectedQ ?: questionnaires.firstOrNull()
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
     * Updates an answer for a specific questionnaire item.
     * @param linkId The linkId of the question.
     * @param answer The provided answer value.
     */
    fun onAnswerChanged(linkId: String, answer: Any?) {
        _uiState.update { 
            val newAnswers = it.answers.toMutableMap()
            if (answer == null) {
                newAnswers.remove(linkId)
            } else {
                newAnswers[linkId] = answer
            }
            it.copy(answers = newAnswers)
        }
    }

    /**
     * Handles changes to the notes field specifically.
     * @param text The new notes text.
     */
    fun onNotesChanged(text: kotlin.String) {
        onAnswerChanged("notes", text)
        _uiState.update { it.copy(notes = text) }
    }

    /**
     * Changes the selected Questionnaire form.
     * @param q The newly selected Questionnaire.
     */
    fun selectQuestionnaire(q: Questionnaire) {
        _uiState.update { it.copy(selectedQuestionnaire = q) }
    }

    /**
     * Creates a new dynamic Questionnaire and selects it.
     * @param title The title of the form.
     * @param photosCount The amount of required photos.
     */
    fun createAndSelectQuestionnaire(title: kotlin.String, photosCount: Int) {
        val q = questionnaireRepository.createQuestionnaire(title, photosCount)
        _uiState.update { 
            it.copy(
                availableQuestionnaires = questionnaireRepository.getAvailableQuestionnaires(),
                selectedQuestionnaire = q
            )
        }
    }

    /**
     * Saves all dynamic answers and photos into a QuestionnaireResponse,
     * updates the Encounter status to Finished, and attempts a cloud sync.
     */
    fun finalizeEncounter() {
        val enc = _uiState.value.encounter ?: return
        val id = enc.id ?: return
        val q = _uiState.value.selectedQuestionnaire
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            // Build and save QuestionnaireResponse
            if (q != null) {
                val qrId = UUID.randomUUID()
                val answers = _uiState.value.answers
                val qr = QuestionnaireResponse.Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed)).apply {
                    this.id = qrId
                    
                    val rawSubjectValue = enc.subject?.reference?.value ?: ""
                    val subjectReference = if (rawSubjectValue.startsWith("Patient/")) rawSubjectValue else "Patient/$rawSubjectValue"
                    subject = Reference.Builder().apply { reference = com.google.fhir.model.r4.String.Builder().apply { value = subjectReference } }
                    
                    val encounterReference = if (id.startsWith("Encounter/")) id else "Encounter/$id"
                    encounter = Reference.Builder().apply { reference = com.google.fhir.model.r4.String.Builder().apply { value = encounterReference } }

                    questionnaire = Canonical.Builder().apply { value = q.id ?: "" }
                    try {
                        authored = DateTime.Builder().apply { value = FhirDateTime.fromString(kotlin.time.Clock.System.now().toString()) }
                    } catch (e: Exception) {}

                    answers.forEach { (linkId, answer) ->
                        val itemBuilder = QuestionnaireResponse.Item.Builder(com.google.fhir.model.r4.String.Builder().apply { value = linkId })
                        
                        when (answer) {
                            is String -> {
                                if (answer.isNotBlank()) {
                                    itemBuilder.answer.add(QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value = com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String(com.google.fhir.model.r4.String.Builder().apply { value = answer }.build())
                                    })
                                    item.add(itemBuilder)
                                }
                            }
                            is Boolean -> {
                                itemBuilder.answer.add(QuestionnaireResponse.Item.Answer.Builder().apply {
                                    value = com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Boolean(com.google.fhir.model.r4.Boolean.Builder().apply { value = answer }.build())
                                })
                                item.add(itemBuilder)
                            }
                        }
                    }

                    _uiState.value.photos.forEach { photo ->
                        val stepName = photo.description?.value ?: return@forEach
                        val urlPath = photo.content.firstOrNull()?.attachment?.url?.value ?: return@forEach

                        item.add(QuestionnaireResponse.Item.Builder(com.google.fhir.model.r4.String.Builder().apply { value = stepName }).apply {
                            answer.add(QuestionnaireResponse.Item.Answer.Builder().apply {
                                value = com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment(
                                    com.google.fhir.model.r4.Attachment.Builder().apply {
                                        url = com.google.fhir.model.r4.Url.Builder().apply { value = urlPath }
                                    }.build()
                                )
                            })
                        })
                    }
                }.build()
                fhirRepository.saveQuestionnaireResponse(qr)
                val prov = createFhirProvenance(
                    id = io.healthplatform.chartcam.utils.UUID.randomUUID(),
                    targetResourceId = qr.id!!,
                    practitionerId = "Practitioner/${_uiState.value.practitioner?.id}",
                    dateStr = kotlin.time.Clock.System.now().toString()
                )
                fhirRepository.saveProvenance(prov, _uiState.value.encounter?.id)
            }
            
            val allAnswers = _uiState.value.answers
            val notesBuilder = StringBuilder()
            allAnswers.forEach { (linkId, answer) ->
                val questionTitle = q?.item?.find { it.linkId.value == linkId }?.text?.value ?: linkId
                when (answer) {
                    is String -> if (answer.isNotBlank()) notesBuilder.append("$questionTitle: $answer. ")
                    is Boolean -> notesBuilder.append("$questionTitle: ${if (answer) "Yes" else "No"}. ")
                }
            }
            val notesStr = notesBuilder.toString().trim()
            
            fhirRepository.updateEncounterStatus(id, "finished", notesStr.ifBlank { "No notes" })
            
            // Ignore result to support offline persistence
            syncManager.syncEncounter(id)
            
            _uiState.update { 
                it.copy(
                    isSyncing = false, 
                    isFinalized = true
                ) 
            }
        }
    }
    
    /**
     * Resets the finalized flag back to false.
     */
    fun resetFinalized() {
        _uiState.update { it.copy(isFinalized = false) }
    }

    /**
     * Adds newly captured photos to the current encounter.
     */
    fun addPhotos(photosMap: Map<String, String>) {
        val enc = _uiState.value.encounter ?: return
        val patient = _uiState.value.patient ?: return
        
        viewModelScope.launch {
            val now = kotlin.time.Clock.System.now()
            val newDocs = photosMap.map { (stepName, path) ->
                createFhirDocumentReference(
                    id = io.healthplatform.chartcam.utils.UUID.randomUUID(),
                    patientId = patient.id ?: "",
                    encounterId = enc.id ?: "",
                    dateStr = now.toString(),
                    desc = stepName,
                    mime = "image/jpeg",
                    urlPath = path
                ).also {
                    fhirRepository.saveDocumentReference(it)
                }
            }
            
            _uiState.update { 
                it.copy(photos = it.photos + newDocs)
            }
        }
    }
}
