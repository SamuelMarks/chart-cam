/**
 * @file EncounterDetailViewModel.kt
 * Contains declarations for EncounterDetailViewModel.kt.
 *
 * ViewModel and UI state definitions for the Encounter Detail screen.
 * This file handles logic for creating, editing, and finalizing encounters,
 * including dynamic questionnaires and photo attachments.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Reference
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirProvenance
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Encounter Detail Screen.
 * Contains patient details, captured photos, and the state of the active questionnaire form.
 *
 * @param isLoading Indicates if initial data is being loaded.
 * @param patient The patient context for this encounter.
 * @param practitioner The currently authenticated practitioner.
 * @param encounter The FHIR Encounter resource being modified.
 * @param photos The captured clinical photos.
 * @param answers A map of questionnaire linkId to dynamic answer (String, Boolean, etc.).
 * @param availableQuestionnaires The list of available questionnaires.
 * @param selectedQuestionnaire The currently selected questionnaire.
 * @param isSyncing Indicates if data is currently syncing.
 * @param isFinalized Flag to signal the UI that finalization and syncing is complete.
 */
data class EncounterUiState(
    val isLoading: Boolean = true,
    val patient: Patient? = null,
    val practitioner: Practitioner? = null,
    val encounter: Encounter? = null,
    val photos: List<DocumentReference> = emptyList(),
    val answers: Map<String, Any> = emptyMap(),
    val availableQuestionnaires: List<Questionnaire> = emptyList(),
    val selectedQuestionnaire: Questionnaire? = null,
    val isSyncing: Boolean = false,
    val isFinalized: Boolean = false,
)

/**
 * ViewModel for viewing and finalizing an Encounter.
 * Handles loading existing encounters, recording form answers dynamically,
 * taking clinical photos, and persisting responses to FHIR JSON and server.
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models
 * (e.g., `Patient`, `Encounter`, `DocumentReference`) without relying on intermediary DTOs.
 *
 * @param fhirRepository The repository providing FHIR data access.
 * @param authRepository The repository providing authentication state.
 * @param questionnaireRepository The repository for managing and retrieving questionnaires.
 */
class EncounterDetailViewModel(
    private val fhirRepository: FhirRepository,
    private val authRepository: AuthRepository,
    private val questionnaireRepository: QuestionnaireRepository,
) : ViewModel() {
    /** Internal mutable state flow holding the Encounter UI state. */
    private val _uiState = MutableStateFlow(EncounterUiState())

    /** Exposes the immutable UI state. */
    val uiState: StateFlow<EncounterUiState> = _uiState.asStateFlow()

    /**
     * Initializes the view model by creating a new encounter or loading an existing one.
     *
     * @param patientId ID of the patient.
     * @param visitId ID of the visit (or "new" to create a new one).
     * @param photosMap Incoming captured photos, mapping step description to file path.
     * @return Unit
     */
    fun initialize(
        patientId: String,
        visitId: String,
        photosMap: Map<kotlin.String, kotlin.String>,
    ) {
        if (_uiState.value.patient != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val patient = fhirRepository.getPatient(patientId)
            val practitioner = authRepository.currentUser.value
            val questionnaires = questionnaireRepository.getAvailableQuestionnaires()

            if (patient != null && practitioner != null) {
                if (visitId == "new") {
                    handleNewEncounter(patient, practitioner, questionnaires, photosMap)
                } else {
                    handleExistingEncounter(visitId, patient, practitioner, questionnaires, photosMap)
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Handles new encounter.
     * @param patient The patient.
     * @param practitioner The practitioner.
     * @param questionnaires The questionnaires.
     * @param photosMap The photosMap.
     */
    private suspend fun handleNewEncounter(
        patient: Patient,
        practitioner: Practitioner,
        questionnaires: List<Questionnaire>,
        photosMap: Map<String, String>,
    ) {
        val now =
            kotlin.time.Clock.System
                .now()
        val encounterId = UUID.randomUUID()

        val newEncounter =
            createFhirEncounter(
                id = encounterId,
                patientId = patient.id ?: "",
                practitionerId = practitioner.id ?: "",
                dateStr = now.toString(),
            )

        fhirRepository.saveEncounter(newEncounter)

        val docs =
            photosMap.map { (stepName, path) ->
                val label =
                    questionnaires.firstOrNull()?.item?.let { items ->
                        io.healthplatform.chartcam.utils.QuestionnaireUtils
                            .findItemRecursively(items, stepName)
                            ?.text
                            ?.value
                    } ?: stepName

                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = UUID.randomUUID(),
                        patientId = patient.id ?: "",
                        encounterId = encounterId,
                        dateStr = now.toString(),
                        desc = label,
                        mime = "image/jpeg",
                        urlPath = path,
                        answerCode = stepName,
                    ),
                ).also { fhirRepository.saveDocumentReference(it) }
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
                selectedQuestionnaire = questionnaires.firstOrNull(),
            )
        }
    }

    /**
     * Handles existing encounter.
     * @param existingResponses The existingResponses.
     * @param questionnaires The questionnaires.
     * @param existingAnswers The existingAnswers.
     * @return The result.
     */
    private suspend fun extractExistingAnswers(
        existingResponses: List<QuestionnaireResponse>,
        questionnaires: List<Questionnaire>,
        existingAnswers: MutableMap<String, Any>,
    ): Questionnaire? {
        if (existingResponses.isEmpty()) return null
        val latestQr = existingResponses.first()
        val rawCanonical = latestQr.questionnaire?.value ?: ""
        val resolvedQId = rawCanonical.substringAfterLast("/")
        var existingSelectedQ = questionnaires.find { it.id == resolvedQId }
        if (existingSelectedQ == null) {
            existingSelectedQ = createRecoveredQuestionnaire(latestQr)
        }
        io.healthplatform.chartcam.utils.QuestionnaireUtils
            .extractAnswersRecursively(latestQr.item, existingAnswers)
        return existingSelectedQ
    }

    /**
     * Handles existing encounter.
     * @param visitId The visitId.
     * @param patient The patient.
     * @param practitioner The practitioner.
     * @param questionnaires The questionnaires.
     * @param photosMap The photosMap.
     */
    private suspend fun handleExistingEncounter(
        visitId: String,
        patient: Patient,
        practitioner: Practitioner,
        questionnaires: List<Questionnaire>,
        photosMap: Map<String, String>,
    ) {
        val existingEncounter = fhirRepository.getEncounter(visitId)
        val existingDocs = fhirRepository.getPhotosForEncounter(visitId).toMutableList()
        val existingResponses = fhirRepository.getQuestionnaireResponsesForEncounter(visitId)

        val existingAnswers = mutableMapOf<String, Any>()
        var existingSelectedQ: Questionnaire? =
            extractExistingAnswers(existingResponses, questionnaires, existingAnswers)

        if (existingEncounter != null) {
            val now =
                kotlin.time.Clock.System
                    .now()
            val newDocs =
                photosMap.map { (stepName, path) ->
                    var label = stepName
                    existingSelectedQ?.item?.let { items ->
                        label = io.healthplatform.chartcam.utils.QuestionnaireUtils
                            .findItemRecursively(items, stepName)
                            ?.text
                            ?.value ?: label
                    }
                    if (label == stepName) {
                        questionnaires.firstOrNull()?.item?.let { items ->
                            label = io.healthplatform.chartcam.utils.QuestionnaireUtils
                                .findItemRecursively(items, stepName)
                                ?.text
                                ?.value ?: label
                        }
                    }

                    createFhirDocumentReference(
                        DocumentReferenceCreationParams(
                            id = UUID.randomUUID(),
                            patientId = patient.id ?: "",
                            encounterId = existingEncounter.id ?: "",
                            dateStr = now.toString(),
                            desc = label,
                            mime = "image/jpeg",
                            urlPath = path,
                            answerCode = stepName,
                        ),
                    ).also { fhirRepository.saveDocumentReference(it) }
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
                    selectedQuestionnaire = existingSelectedQ ?: questionnaires.firstOrNull(),
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Creates recovered questionnaire.
     * @param latestQr The latestQr.
     * @param recoveredFormStr The string to use for the recovered form title.
     * @return The result.
     */
    private suspend fun createRecoveredQuestionnaire(
        latestQr: QuestionnaireResponse,
        recoveredFormStr: String = "Recovered Form",
    ): Questionnaire {
        val dummyItems =
            io.healthplatform.chartcam.utils.QuestionnaireUtils
                .buildDummyItemsRecursively(latestQr.item)
        return Questionnaire
            .Builder(
                Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active),
            ).apply {
                this.id = latestQr.questionnaire?.value ?: "unknown"
                this.title =
                    com.google.fhir.model.r4.String
                        .Builder()
                        .apply { value = recoveredFormStr }
                this.item.addAll(dummyItems)
            }.build()
    }

    /**
     * Updates an answer for a specific questionnaire item.
     *
     * @param linkId The linkId of the question.
     * @param answer The provided answer value, or null to remove the answer.
     */
    fun onAnswerChanged(
        linkId: String,
        answer: Any?,
    ) {
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
     * Updates the form answers and the generated QuestionnaireResponse.
     *
     * @param newAnswers The evaluated answers map.
     */
    fun onFormUpdated(newAnswers: Map<String, Any>) {
        _uiState.update {
            it.copy(answers = newAnswers)
        }
        // Store newResponse somewhere if you want to use it during finalizeEncounter
        // instead of generating it manually, but for now we just update answers to
        // keep the rest of the flow working and fulfill the requirement.
    }

    /**
     * Handles changes to the notes field specifically.
     *
     * @param text The new notes text.
     */
    fun onNotesChanged(text: kotlin.String) {
        onAnswerChanged("notes", text)
    }

    /**
     * Changes the selected Questionnaire form.
     *
     * @param q The newly selected Questionnaire.
     */
    fun selectQuestionnaire(q: Questionnaire) {
        _uiState.update { it.copy(selectedQuestionnaire = q) }
    }

    /**
     * Selects a questionnaire by its ID from the available ones.
     *
     * @param id The ID of the questionnaire to select.
     */
    fun selectQuestionnaireById(id: String) {
        val q = _uiState.value.availableQuestionnaires.find { it.id == id }
        if (q != null) {
            _uiState.update { it.copy(selectedQuestionnaire = q) }
        } else {
            // Might not be in the current state's list yet if it was just created
            val freshList = questionnaireRepository.getAvailableQuestionnaires()
            val freshQ = freshList.find { it.id == id }
            if (freshQ != null) {
                _uiState.update {
                    it.copy(
                        availableQuestionnaires = freshList,
                        selectedQuestionnaire = freshQ,
                    )
                }
            }
        }
    }

    /**
     * Creates a new dynamic Questionnaire and selects it.
     *
     * @param title The title of the form.
     * @param photosCount The amount of required photos.
     * @param labels Optional labels/tags for the questionnaire.
     */
    fun createAndSelectQuestionnaire(
        title: kotlin.String,
        photosCount: Int,
        labels: kotlin.String = "",
    ) {
        val q = questionnaireRepository.createQuestionnaire(title, photosCount, labels)
        _uiState.update {
            it.copy(
                availableQuestionnaires = questionnaireRepository.getAvailableQuestionnaires(),
                selectedQuestionnaire = q,
            )
        }
    }

    /**
     * Saves all dynamic answers and photos into a QuestionnaireResponse,
     * updates the Encounter status to Finished, and attempts a cloud sync.
     *
     * @param yesStr The string to use for a positive boolean answer.
     * @param noStr The string to use for a negative boolean answer.
     * @param noNotesStr The string to use if there are no notes.
     */
    fun finalizeEncounter(
        yesStr: String = "Yes",
        noStr: String = "No",
        noNotesStr: String = "No notes",
    ) {
        val enc = _uiState.value.encounter ?: return
        val id = enc.id ?: return
        val q = _uiState.value.selectedQuestionnaire

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            // Build and save QuestionnaireResponse
            buildAndSaveQuestionnaireResponse(q, enc)
            updateEncounterWithNotes(q, id, yesStr, noStr, noNotesStr)

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isFinalized = true,
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
     * Reopens a finalized encounter by updating its FHIR status back to "in-progress"
     * and clearing the `isFinalized` flag, allowing further edits to forms and photos.
     */
    fun reopenEncounter() {
        val enc = _uiState.value.encounter ?: return
        val id = enc.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            // Revert the encounter status back to in-progress
            fhirRepository.updateEncounterStatus(
                id,
                "in-progress",
                "",
            )

            // Reload the encounter to ensure state is completely in sync with local DB
            val updatedEnc = fhirRepository.getEncounter(id)

            _uiState.update {
                it.copy(
                    encounter = updatedEnc,
                    isSyncing = false,
                    isFinalized = false,
                )
            }
        }
    }

    /**
     * Adds newly captured photos to the current encounter.
     *
     * @param photosMap Map of step names to photo paths.
     */
    fun addPhotos(photosMap: Map<String, String>) {
        val enc = _uiState.value.encounter ?: return
        val patient = _uiState.value.patient ?: return

        viewModelScope.launch {
            val now =
                kotlin.time.Clock.System
                    .now()
            val newDocs =
                photosMap.map { (stepName, path) ->
                    val label =
                        _uiState.value.selectedQuestionnaire?.item?.let { items ->
                            io.healthplatform.chartcam.utils.QuestionnaireUtils
                                .findItemRecursively(items, stepName)
                                ?.text
                                ?.value
                        } ?: stepName

                    createFhirDocumentReference(
                        DocumentReferenceCreationParams(
                            id =
                                io.healthplatform.chartcam.utils.UUID
                                    .randomUUID(),
                            patientId = patient.id ?: "",
                            encounterId = enc.id ?: "",
                            dateStr = now.toString(),
                            desc = label,
                            mime = "image/jpeg",
                            urlPath = path,
                            answerCode = stepName,
                        ),
                    ).also {
                        fhirRepository.saveDocumentReference(it)
                    }
                }

            _uiState.update {
                it.copy(photos = it.photos + newDocs)
            }
        }
    }

    /**
     * Deletes the currently active encounter.
     *
     * @param onSuccess Callback executed when deletion completes successfully.
     */
    fun deleteEncounter(onSuccess: () -> Unit) {
        val encId = _uiState.value.encounter?.id ?: return
        viewModelScope.launch {
            fhirRepository.deleteEncounter(encId)
            onSuccess()
        }
    }

    /**
     * Builds and saves a QuestionnaireResponse.
     * @param q The q.
     * @param enc The enc.
     */
    private suspend fun buildAndSaveQuestionnaireResponse(
        q: Questionnaire?,
        enc: Encounter,
    ) {
        val id = enc.id ?: return
        if (q == null) return

        val qrId = UUID.randomUUID()
        val answers = _uiState.value.answers

        val qr =
            QuestionnaireResponse
                .Builder(
                    Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                ).apply {
                    this.id = qrId
                    this.subject = buildSubjectReference(enc)
                    this.encounter = buildEncounterReference(id)
                    this.questionnaire = Canonical.Builder().apply { value = q.id ?: "" }

                    try {
                        this.authored =
                            DateTime.Builder().apply {
                                value =
                                    FhirDateTime.fromString(
                                        kotlin.time.Clock.System
                                            .now()
                                            .toString(),
                                    )
                            }
                    } catch (ignored: RuntimeException) {
                        // Ignored
                    }

                    if (q.item.isNotEmpty()) {
                        this.item.addAll(
                            io.healthplatform.chartcam.utils.QuestionnaireUtils
                                .buildResponseItemsRecursively(q.item, answers),
                        )
                    }

                    appendPhotosToResponseItems(this.item)
                }.build()

        fhirRepository.saveQuestionnaireResponse(qr)

        val prov =
            createFhirProvenance(
                id = UUID.randomUUID(),
                targetResourceId = qr.id!!,
                practitionerId = "Practitioner/${_uiState.value.practitioner?.id}",
                dateStr =
                    kotlin.time.Clock.System
                        .now()
                        .toString(),
            )
        fhirRepository.saveProvenance(prov, _uiState.value.encounter?.id)
    }

    /**
     * Builds subject reference.
     * @param enc The enc.
     * @return The result.
     */
    private fun buildSubjectReference(enc: Encounter): Reference.Builder {
        val rawSubjectValue = enc.subject?.reference?.value ?: ""
        val subjectReference =
            if (rawSubjectValue.startsWith("Patient/")) rawSubjectValue else "Patient/$rawSubjectValue"
        return Reference.Builder().apply {
            reference =
                com.google.fhir.model.r4.String
                    .Builder()
                    .apply { value = subjectReference }
        }
    }

    /**
     * Builds encounter reference.
     * @param id The id.
     * @return The result.
     */
    private fun buildEncounterReference(id: String): Reference.Builder {
        val encounterReference = if (id.startsWith("Encounter/")) id else "Encounter/$id"
        return Reference.Builder().apply {
            reference =
                com.google.fhir.model.r4.String
                    .Builder()
                    .apply { value = encounterReference }
        }
    }

    /**
     * Appends photos.
     * @param items The items.
     */
    private fun appendPhotosToResponseItems(items: MutableList<QuestionnaireResponse.Item.Builder>) {
        _uiState.value.photos.forEach { photo ->
            val stepName =
                photo.context
                    ?.related
                    ?.firstOrNull()
                    ?.identifier
                    ?.value
                    ?.value ?: photo.description?.value
            if (stepName != null) {
                val urlPath =
                    photo.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value
                if (urlPath != null) {
                    items.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = stepName },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment(
                                                com.google.fhir.model.r4.Attachment
                                                    .Builder()
                                                    .apply {
                                                        url =
                                                            com.google.fhir.model.r4.Url
                                                                .Builder()
                                                                .apply { value = urlPath }
                                                    }.build(),
                                            )
                                    },
                                )
                            },
                    )
                }
            }
        }
    }

    /**
     * Updates the encounter with notes from the form.
     * @param q The q.
     * @param encounterId The encounterId.
     * @param yesStr The string to use for a positive boolean answer.
     * @param noStr The string to use for a negative boolean answer.
     * @param noNotesStr The string to use if there are no notes.
     */
    private suspend fun updateEncounterWithNotes(
        q: Questionnaire?,
        encounterId: String,
        yesStr: String,
        noStr: String,
        noNotesStr: String,
    ) {
        val allAnswers = _uiState.value.answers
        val notesBuilder = StringBuilder()
        allAnswers.forEach { (linkId, answer) ->
            val questionItem =
                q?.item?.let {
                    io.healthplatform.chartcam.utils.QuestionnaireUtils
                        .findItemRecursively(it, linkId)
                }
            val questionTitle = questionItem?.text?.value ?: linkId
            when (answer) {
                is String -> if (answer.isNotBlank()) notesBuilder.append("$questionTitle: $answer. ")
                is Boolean ->
                    notesBuilder.append(
                        "$questionTitle: ${if (answer) yesStr else noStr}. ",
                    )
                is List<*> -> {
                    val strList = answer.filterIsInstance<String>()
                    if (strList.isNotEmpty()) {
                        notesBuilder.append("$questionTitle: ${strList.joinToString(", ")}. ")
                    }
                }
                is Float -> notesBuilder.append("$questionTitle: $answer. ")
            }
        }
        val notesStr = notesBuilder.toString().trim()

        fhirRepository.updateEncounterStatus(
            encounterId,
            "finished",
            notesStr.ifBlank { noNotesStr },
        )

        // Ignore result to support offline persistence
    }
}
