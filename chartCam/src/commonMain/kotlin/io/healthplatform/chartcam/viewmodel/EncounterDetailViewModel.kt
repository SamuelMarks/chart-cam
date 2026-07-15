/**
 * ViewModel and UI state definitions for the Encounter Detail screen.
 * This file handles logic for creating, editing, and finalizing encounters,
 * including dynamic questionnaires and photo attachments.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.no
import chartcam.chartcam.generated.resources.no_notes
import chartcam.chartcam.generated.resources.recovered_form
import chartcam.chartcam.generated.resources.yes
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
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirProvenance
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncWorker
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
 * @property isLoading Indicates if initial data is being loaded.
 * @property patient The patient context for this encounter.
 * @property practitioner The currently authenticated practitioner.
 * @property encounter The FHIR Encounter resource being modified.
 * @property photos The captured clinical photos.
 * @property answers A map of questionnaire linkId to dynamic answer (String, Boolean, etc.).
 * @property availableQuestionnaires The list of available questionnaires.
 * @property selectedQuestionnaire The currently selected questionnaire.
 * @property isSyncing Indicates if data is currently syncing.
 * @property isFinalized Flag to signal the UI that finalization and syncing is complete.
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
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models (e.g., `Patient`, `Encounter`, `DocumentReference`) without relying on intermediary DTOs.
 *
 * @property fhirRepository The repository providing FHIR data access.
 * @property authRepository The repository providing authentication state.
 * @property syncWorker The worker handling synchronization of data.
 * @property questionnaireRepository The repository for managing and retrieving questionnaires.
 */
class EncounterDetailViewModel(
    private val fhirRepository: FhirRepository,
    private val authRepository: AuthRepository,
    private val syncWorker: SyncWorker,
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
                            statusStr = "in-progress",
                        )

                    fhirRepository.saveEncounter(newEncounter)

                    val docs =
                        photosMap.map { (stepName, path) ->
                            val label =
                                questionnaires.firstOrNull()?.item?.let { items ->
                                    findItemRecursively(items, stepName)?.text?.value
                                } ?: stepName

                            createFhirDocumentReference(
                                id = UUID.randomUUID(),
                                patientId = patient.id ?: "",
                                encounterId = encounterId,
                                dateStr = now.toString(),
                                desc = label,
                                mime = "image/jpeg",
                                urlPath = path,
                                answerCode = stepName,
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
                            selectedQuestionnaire = questionnaires.firstOrNull(),
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

                        // Parse FHIR Canonical URI to match local Resource ID
                        // The Questionnaire reference may be an absolute URI, a relative path (e.g., 'Questionnaire/q-id'),
                        // or just the raw ID. We substring after the last slash to normalize it to the base ID.
                        val rawCanonical = latestQr.questionnaire?.value ?: ""
                        val resolvedQId = rawCanonical.substringAfterLast("/")

                        existingSelectedQ = questionnaires.find { it.id == resolvedQId }

                        if (existingSelectedQ == null) {
                            val dummyItems = buildDummyItemsRecursively(latestQr.item)
                            val recoveredFormStr =
                                org.jetbrains.compose.resources
                                    .getString(Res.string.recovered_form)
                            existingSelectedQ =
                                Questionnaire
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

                        extractAnswersRecursively(latestQr.item, existingAnswers)
                    }

                    if (existingEncounter != null) {
                        val now =
                            kotlin.time.Clock.System
                                .now()
                        val newDocs =
                            photosMap.map { (stepName, path) ->
                                val label =
                                    existingSelectedQ?.item?.let { items ->
                                        findItemRecursively(items, stepName)?.text?.value
                                    } ?: (
                                        questionnaires.firstOrNull()?.item?.let { items ->
                                            findItemRecursively(items, stepName)?.text?.value
                                        } ?: stepName
                                    )

                                createFhirDocumentReference(
                                    id = UUID.randomUUID(),
                                    patientId = patient.id ?: "",
                                    encounterId = existingEncounter.id ?: "",
                                    dateStr = now.toString(),
                                    desc = label,
                                    mime = "image/jpeg",
                                    urlPath = path,
                                    answerCode = stepName,
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
                                selectedQuestionnaire = existingSelectedQ ?: questionnaires.firstOrNull(),
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
     * @param newResponse The generated QuestionnaireResponse resource.
     */
    fun onFormUpdated(
        newAnswers: Map<String, Any>,
        newResponse: com.google.fhir.model.r4.QuestionnaireResponse,
    ) {
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
                val qr =
                    QuestionnaireResponse
                        .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                        .apply {
                            this.id = qrId

                            val rawSubjectValue = enc.subject?.reference?.value ?: ""
                            val subjectReference =
                                if (rawSubjectValue.startsWith(
                                        "Patient/",
                                    )
                                ) {
                                    rawSubjectValue
                                } else {
                                    "Patient/$rawSubjectValue"
                                }
                            subject =
                                Reference.Builder().apply {
                                    reference =
                                        com.google.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = subjectReference }
                                }

                            val encounterReference = if (id.startsWith("Encounter/")) id else "Encounter/$id"
                            encounter =
                                Reference.Builder().apply {
                                    reference =
                                        com.google.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = encounterReference }
                                }

                            questionnaire = Canonical.Builder().apply { value = q.id ?: "" }
                            try {
                                authored =
                                    DateTime.Builder().apply {
                                        value =
                                            FhirDateTime.fromString(
                                                kotlin.time.Clock.System
                                                    .now()
                                                    .toString(),
                                            )
                                    }
                            } catch (e: Exception) {
                            }

                            if (q.item.isNotEmpty()) {
                                item.addAll(buildResponseItemsRecursively(q.item, answers))
                            }

                            _uiState.value.photos.forEach { photo ->
                                val stepName =
                                    photo.context
                                        ?.related
                                        ?.firstOrNull()
                                        ?.identifier
                                        ?.value
                                        ?.value
                                        ?: photo.description?.value
                                        ?: return@forEach
                                val urlPath =
                                    photo.content
                                        .firstOrNull()
                                        ?.attachment
                                        ?.url
                                        ?.value ?: return@forEach

                                item.add(
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
                        }.build()
                fhirRepository.saveQuestionnaireResponse(qr)
                val prov =
                    createFhirProvenance(
                        id =
                            io.healthplatform.chartcam.utils.UUID
                                .randomUUID(),
                        targetResourceId = qr.id!!,
                        practitionerId = "Practitioner/${_uiState.value.practitioner?.id}",
                        dateStr =
                            kotlin.time.Clock.System
                                .now()
                                .toString(),
                    )
                fhirRepository.saveProvenance(prov, _uiState.value.encounter?.id)
            }

            val allAnswers = _uiState.value.answers
            val notesBuilder = StringBuilder()
            allAnswers.forEach { (linkId, answer) ->
                val questionItem = q?.item?.let { findItemRecursively(it, linkId) }
                val questionTitle = questionItem?.text?.value ?: linkId
                when (answer) {
                    is String -> if (answer.isNotBlank()) notesBuilder.append("$questionTitle: $answer. ")
                    is Boolean ->
                        notesBuilder.append(
                            "$questionTitle: ${if (answer) {
                                org.jetbrains.compose.resources.getString(
                                    Res.string.yes,
                                )
                            } else {
                                org.jetbrains.compose.resources
                                    .getString(Res.string.no)
                            }}. ",
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
                id,
                "finished",
                notesStr.ifBlank {
                    org.jetbrains.compose.resources
                        .getString(Res.string.no_notes)
                },
            )

            // Ignore result to support offline persistence
            syncWorker.sync()

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
                            findItemRecursively(items, stepName)?.text?.value
                        } ?: stepName

                    createFhirDocumentReference(
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
     * Recursively searches for a Questionnaire.Item by its linkId.
     *
     * @param items The list of items to search.
     * @param linkId The linkId to search for.
     * @return The found item or null.
     */
    private fun findItemRecursively(
        items: List<Questionnaire.Item>,
        linkId: String,
    ): Questionnaire.Item? {
        for (item in items) {
            if (item.linkId.value == linkId) return item
            val found = findItemRecursively(item.item, linkId)
            if (found != null) return found
        }
        return null
    }

    /**
     * Recursively builds dummy Questionnaire.Item.Builders from a QuestionnaireResponse.Item tree.
     *
     * @param qrItems The items from a QuestionnaireResponse.
     * @return A list of generated Questionnaire.Item.Builder instances.
     */
    private fun buildDummyItemsRecursively(qrItems: List<QuestionnaireResponse.Item>): List<Questionnaire.Item.Builder> {
        val dummyItems = mutableListOf<Questionnaire.Item.Builder>()
        qrItems.forEach { qrItem ->
            val linkId = qrItem.linkId.value ?: return@forEach
            val answer = qrItem.answer.firstOrNull()?.value
            val qItemType =
                if (answer != null) {
                    when (answer) {
                        is QuestionnaireResponse.Item.Answer.Value.String ->
                            Questionnaire.QuestionnaireItemType.String
                        is QuestionnaireResponse.Item.Answer.Value.Boolean ->
                            Questionnaire.QuestionnaireItemType.Boolean
                        is QuestionnaireResponse.Item.Answer.Value.Attachment ->
                            Questionnaire.QuestionnaireItemType.Attachment
                        is QuestionnaireResponse.Item.Answer.Value.Decimal ->
                            Questionnaire.QuestionnaireItemType.Decimal
                        is QuestionnaireResponse.Item.Answer.Value.Integer ->
                            Questionnaire.QuestionnaireItemType.Integer
                        is QuestionnaireResponse.Item.Answer.Value.Date ->
                            Questionnaire.QuestionnaireItemType.Date
                        is QuestionnaireResponse.Item.Answer.Value.DateTime ->
                            Questionnaire.QuestionnaireItemType.DateTime
                        else -> Questionnaire.QuestionnaireItemType.String
                    }
                } else if (qrItem.item.isNotEmpty()) {
                    Questionnaire.QuestionnaireItemType.Group
                } else {
                    Questionnaire.QuestionnaireItemType.String
                }

            val builder =
                Questionnaire.Item
                    .Builder(
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = linkId },
                        Enumeration(value = qItemType),
                    ).apply {
                        this.text =
                            com.google.fhir.model.r4.String
                                .Builder()
                                .apply { value = linkId.replaceFirstChar { it.uppercase() } }
                        if (qrItem.item.isNotEmpty()) {
                            this.item.addAll(buildDummyItemsRecursively(qrItem.item))
                        }
                    }
            dummyItems.add(builder)
        }
        return dummyItems
    }

    /**
     * Recursively builds QuestionnaireResponse.Item from Questionnaire.Item based on current answers.
     *
     * @param qItems The list of Questionnaire.Item to traverse.
     * @param answers The map of current answers.
     * @return A list of populated QuestionnaireResponse.Item.Builder instances.
     */
    private fun buildResponseItemsRecursively(
        qItems: List<Questionnaire.Item>,
        answers: Map<String, Any>,
    ): List<QuestionnaireResponse.Item.Builder> {
        val responseItems = mutableListOf<QuestionnaireResponse.Item.Builder>()
        for (qItem in qItems) {
            val linkId = qItem.linkId.value ?: continue
            val qType = qItem.type.value
            val answer = answers[linkId]

            val itemBuilder =
                QuestionnaireResponse.Item
                    .Builder(
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = linkId },
                    ).apply {
                        this.text = qItem.text?.toBuilder()
                    }

            var hasAnswer = false

            if (answer != null) {
                when (answer) {
                    is String -> {
                        if (answer.isNotBlank()) {
                            val fhirValue =
                                when (qType) {
                                    Questionnaire.QuestionnaireItemType.Date ->
                                        com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Date(
                                            com.google.fhir.model.r4.Date
                                                .Builder()
                                                .apply {
                                                    value =
                                                        com.google.fhir.model.r4.FhirDate
                                                            .fromString(answer)
                                                }.build(),
                                        )
                                    Questionnaire.QuestionnaireItemType.DateTime ->
                                        com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.DateTime(
                                            com.google.fhir.model.r4.DateTime
                                                .Builder()
                                                .apply {
                                                    value =
                                                        com.google.fhir.model.r4.FhirDateTime
                                                            .fromString(answer)
                                                }.build(),
                                        )
                                    Questionnaire.QuestionnaireItemType.Decimal ->
                                        com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal(
                                            com.google.fhir.model.r4.Decimal
                                                .Builder()
                                                .apply {
                                                    value =
                                                        com.ionspin.kotlin.bignum.decimal.BigDecimal
                                                            .parseString(answer)
                                                }.build(),
                                        )
                                    Questionnaire.QuestionnaireItemType.Integer ->
                                        com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer(
                                            com.google.fhir.model.r4.Integer
                                                .Builder()
                                                .apply {
                                                    value =
                                                        answer.toIntOrNull() ?: 0
                                                }.build(),
                                        )
                                    else ->
                                        com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String(
                                            com.google.fhir.model.r4.String
                                                .Builder()
                                                .apply {
                                                    value =
                                                        answer
                                                }.build(),
                                        )
                                }
                            itemBuilder.answer.add(
                                QuestionnaireResponse.Item.Answer
                                    .Builder()
                                    .apply { value = fhirValue },
                            )
                            hasAnswer = true
                        }
                    }
                    is List<*> -> {
                        val strList = answer.filterIsInstance<String>().filter { it.isNotBlank() }
                        if (strList.isNotEmpty()) {
                            strList.forEach { strVal ->
                                itemBuilder.answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String(
                                                com.google.fhir.model.r4.String
                                                    .Builder()
                                                    .apply { value = strVal }
                                                    .build(),
                                            )
                                    },
                                )
                            }
                            hasAnswer = true
                        }
                    }
                    is Boolean -> {
                        itemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer.Builder().apply {
                                value =
                                    com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Boolean(
                                        com.google.fhir.model.r4.Boolean
                                            .Builder()
                                            .apply { value = answer }
                                            .build(),
                                    )
                            },
                        )
                        hasAnswer = true
                    }
                    is Float -> {
                        val fhirValue =
                            when (qType) {
                                Questionnaire.QuestionnaireItemType.Integer ->
                                    com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer(
                                        com.google.fhir.model.r4.Integer
                                            .Builder()
                                            .apply {
                                                value =
                                                    answer.toInt()
                                            }.build(),
                                    )
                                else ->
                                    com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal(
                                        com.google.fhir.model.r4.Decimal
                                            .Builder()
                                            .apply {
                                                value =
                                                    com.ionspin.kotlin.bignum.decimal.BigDecimal
                                                        .parseString(answer.toString())
                                            }.build(),
                                    )
                            }
                        itemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer
                                .Builder()
                                .apply { value = fhirValue },
                        )
                        hasAnswer = true
                    }
                }
            }

            if (qItem.item.isNotEmpty()) {
                val nestedItems = buildResponseItemsRecursively(qItem.item, answers)
                if (nestedItems.isNotEmpty()) {
                    itemBuilder.item.addAll(nestedItems)
                    hasAnswer = true
                }
            }

            if (hasAnswer || qType == Questionnaire.QuestionnaireItemType.Group) {
                responseItems.add(itemBuilder)
            }
        }
        return responseItems
    }

    /**
     * Recursively extracts answers from QuestionnaireResponse items.
     *
     * @param items The list of QuestionnaireResponse.Item to traverse.
     * @param existingAnswers The map to populate with extracted answers.
     */
    private fun extractAnswersRecursively(
        items: List<QuestionnaireResponse.Item>,
        existingAnswers: MutableMap<String, Any>,
    ) {
        items.forEach { item ->
            val linkId = item.linkId.value ?: return@forEach
            val answers = item.answer
            if (answers.isNotEmpty()) {
                if (answers.size > 1) {
                    val list =
                        answers.mapNotNull {
                            val v = it.value
                            if (v is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String) v.value.value else null
                        }
                    existingAnswers[linkId] = list
                } else {
                    val answer = answers.first().value
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
                            is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal -> {
                                val v = answer.value.value
                                if (v != null) existingAnswers[linkId] = v.toStringExpanded().toFloatOrNull() ?: 0f
                            }
                            is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer -> {
                                val v = answer.value.value
                                existingAnswers[linkId] = v?.toFloat() ?: 0f
                            }
                            is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Date -> {
                                val v = answer.value.value
                                if (v != null) existingAnswers[linkId] = v.toString()
                            }
                            is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.DateTime -> {
                                val v = answer.value.value
                                if (v != null) existingAnswers[linkId] = v.toString()
                            }
                            is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment -> {
                                // handled in photos
                            }
                            else -> {}
                        }
                    }
                }
            }
            if (item.item.isNotEmpty()) {
                extractAnswersRecursively(item.item, existingAnswers)
            }
        }
    }
}
