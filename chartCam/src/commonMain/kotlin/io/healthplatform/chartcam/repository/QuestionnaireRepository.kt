/**
 * @file QuestionnaireRepository.kt
 * Contains declarations for QuestionnaireRepository.kt.
 *
 * Repository for providing and managing FHIR Questionnaire resources.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import dev.ohs.fhir.model.r4.terminologies.QuestionnaireItemType
import io.healthplatform.chartcam.utils.ChartCamError
import io.healthplatform.chartcam.utils.DefaultDispatcherProvider
import io.healthplatform.chartcam.utils.DefaultLocaleProvider
import io.healthplatform.chartcam.utils.DispatcherProvider
import io.healthplatform.chartcam.utils.LocaleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Repository to manage Questionnaire forms available for clinical encounters.
 * Stores forms in memory and coordinates persistence with [FhirRepository].
 *
 * @param fhirRepository Optional persistent FHIR repository for saving/loading custom questionnaires.
 * @param dispatcherProvider Provider for coroutine dispatchers.
 * @param localeProvider Provider for the active application language tag.
 * @param externalScope Optional external [CoroutineScope] for background database persistence.
 */
open class QuestionnaireRepository(
    private val fhirRepository: FhirRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    private val localeProvider: LocaleProvider = DefaultLocaleProvider,
    externalScope: CoroutineScope? = null,
) {
    /**
     * Supervised coroutine scope used for asynchronous repository synchronization.
     */
    private val repositoryScope: CoroutineScope =
        externalScope ?: CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    /**
     * In-memory storage mapping questionnaire IDs to their respective FHIR Questionnaire resources.
     */
    private val inMemoryForms = mutableMapOf<String, Questionnaire>()

    /**
     * Loads a single questionnaire template from bundled JSON resources, falling back to an in-code
     * template if the bundled resource is unavailable.
     *
     * @param path The resource path.
     * @param id The template ID.
     * @return A [Result] enclosing the loaded [Questionnaire].
     */
    internal suspend fun loadTemplate(
        path: String,
        id: String,
    ): Result<Questionnaire> {
        val loaded =
            runCatching {
                val bytes =
                    chartcam.chartcam.generated.resources.Res
                        .readBytes(path)
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .decodeTypedResource(Questionnaire.serializer(), bytes.decodeToString())
                    .getOrNull()
            }.getOrNull() ?: createDefaultFallbackQuestionnaire(id)
                ?: Questionnaire(status = Enumeration(value = PublicationStatus.Active))

        inMemoryForms[id] = loaded
        return Result.success(loaded)
    }

    /**
     * Creates a fallback Questionnaire when a bundled JSON file cannot be read from resources.
     *
     * @param id The template ID.
     * @return A constructed fallback [Questionnaire], or null if unrecognized ID.
     */
    private fun createDefaultFallbackQuestionnaire(id: String): Questionnaire? =
        when (id) {
            "facial-cornea-profile" -> createFacialCorneaProfileFallback(id)
            "std-form" -> createStdFormFallback(id)
            "basic-followup" -> createBasicFollowupFallback(id)
            else -> null
        }

    /**
     * Constructs the fallback questionnaire for facial profile and cornea examination.
     *
     * @param id The questionnaire identifier.
     * @return Fully constructed fallback [Questionnaire].
     */
    private fun createFacialCorneaProfileFallback(id: String): Questionnaire {
        val notes =
            createItem(
                "clinical_notes",
                "Clinical Observations & Notes",
                QuestionnaireItemType.Text,
                required = false,
            )
        val left =
            createGuidedPhotoItem(
                "profile_left",
                "Left Profile (Cornea & Nose)",
                "profile-cornea-left",
                "272480006",
                "Left lateral",
            )
        val front =
            createGuidedPhotoItem(
                "front_view",
                "Front View",
                "frontal-face",
                "272483008",
                "Anterior",
            )
        val right =
            createGuidedPhotoItem(
                "profile_right",
                "Right Profile (Cornea & Nose)",
                "profile-cornea-right",
                "272481005",
                "Right lateral",
            )
        return createFhirQuestionnaire(
            id = id,
            title = "Facial Profile & Cornea Examination",
            items = listOf(notes, left, front, right),
        )
    }

    /**
     * Helper to create a single guided photo item with silhouette extension and orientation coding.
     *
     * @param linkId Item linkId.
     * @param text Item prompt text.
     * @param silhouetteCode Preset silhouette code.
     * @param snomedCode Orientation SNOMED code.
     * @param snomedDisplay Orientation display label.
     * @return Populated [Questionnaire.Item.Builder].
     */
    private fun createGuidedPhotoItem(
        linkId: String,
        text: String,
        silhouetteCode: String,
        snomedCode: String,
        snomedDisplay: String,
    ): Questionnaire.Item.Builder =
        createItem(linkId, text, QuestionnaireItemType.Attachment, required = true).apply {
            extension.add(
                dev.ohs.fhir.model.r4.Extension
                    .Builder(
                        url = "http://healthplatform.io/fhir/StructureDefinition/camera-silhouette",
                    ).apply {
                        value =
                            dev.ohs.fhir.model.r4.Extension.Value.Code(
                                dev.ohs.fhir.model.r4
                                    .Code(value = silhouetteCode),
                            )
                    },
            )
            code.add(
                dev.ohs.fhir.model.r4
                    .Coding(
                        system =
                            dev.ohs.fhir.model.r4
                                .Uri(value = "http://loinc.org"),
                        code =
                            dev.ohs.fhir.model.r4
                                .Code(value = "72170-4"),
                        display = FhirString(value = "Photographic image"),
                    ).toBuilder(),
            )
            code.add(
                dev.ohs.fhir.model.r4
                    .Coding(
                        system =
                            dev.ohs.fhir.model.r4
                                .Uri(value = "http://snomed.info/sct"),
                        code =
                            dev.ohs.fhir.model.r4
                                .Code(value = snomedCode),
                        display = FhirString(value = snomedDisplay),
                    ).toBuilder(),
            )
        }

    /**
     * Constructs fallback questionnaire for std-form.
     *
     * @param id The questionnaire ID.
     * @return Constructed fallback [Questionnaire].
     */
    private fun createStdFormFallback(id: String): Questionnaire {
        val items =
            listOf(
                createItem("notes", "Clinical Notes", QuestionnaireItemType.String, required = false),
                createItem("front", "Front", QuestionnaireItemType.Attachment, required = true),
                createItem("front_ruler", "Front + Ruler", QuestionnaireItemType.Attachment, true),
                createItem("right", "Right Side", QuestionnaireItemType.Attachment, required = true),
                createItem("right_ruler", "Right Side + Ruler", QuestionnaireItemType.Attachment, true),
            )
        return createFhirQuestionnaire(id, "Standard Clinical Photo", items)
    }

    /**
     * Constructs fallback questionnaire for basic-followup.
     *
     * @param id The questionnaire ID.
     * @return Constructed fallback [Questionnaire].
     */
    private fun createBasicFollowupFallback(id: String): Questionnaire {
        val items =
            listOf(
                createItem("notes", "Follow-up Notes", QuestionnaireItemType.String, required = false),
                createItem("front", "Front View", QuestionnaireItemType.Attachment, required = true),
                createItem("right", "Right View", QuestionnaireItemType.Attachment, required = true),
            )
        return createFhirQuestionnaire(id, "Basic Follow-up", items)
    }

    /**
     * Loads custom questionnaires persisted in the database into memory.
     *
     * @param repo The FHIR repository instance.
     */
    private suspend fun loadDatabaseForms(repo: FhirRepository) {
        val entities =
            repo.database.chartCamQueries
                .getAllResourcesByType("Questionnaire")
                .awaitAsList()

        for (entity in entities) {
            val q =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .decodeTypedResource(Questionnaire.serializer(), entity.serializedResource)
                    .getOrNull()
            val qId = q?.id
            if (q != null && qId != null) {
                inMemoryForms[qId] = q
            }
        }
    }

    /**
     * Loads the default questionnaire templates from bundled JSON resources and any persisted database forms.
     *
     * @return A [Result] indicating success or failure of loading default forms.
     */
    suspend fun loadDefaultForms(): Result<Unit> {
        if (!inMemoryForms.containsKey("std-form")) {
            loadTemplate("files/default_templates/std-form.json", "std-form")
            loadTemplate("files/default_templates/basic-followup.json", "basic-followup")
            loadTemplate("files/default_templates/facial-cornea-profile.json", "facial-cornea-profile")
        }

        val repo = fhirRepository
        if (repo != null) {
            loadDatabaseForms(repo)
        }
        return Result.success(Unit)
    }

    /**
     * Helper method to construct a single FHIR Questionnaire Item builder.
     *
     * @param linkId The unique identifier within the questionnaire for this item.
     * @param text The descriptive text or question to display for this item.
     * @param type The data type of the expected answer.
     * @param required True if this item must be answered to complete the questionnaire.
     * @return A builder for a [Questionnaire.Item].
     */
    private fun createItem(
        linkId: String,
        text: String,
        type: QuestionnaireItemType,
        required: Boolean,
    ): Questionnaire.Item.Builder =
        Questionnaire.Item
            .Builder(
                FhirString(value = linkId).toBuilder(),
                Enumeration(value = type),
            ).apply {
                this.text = FhirString(value = text).toBuilder()
                this.required = FhirBoolean(value = required).toBuilder()
            }

    /**
     * Helper method to construct a complete FHIR Questionnaire resource.
     *
     * @param id The global unique identifier for the new questionnaire.
     * @param title The display title of the questionnaire.
     * @param items The list of item builders making up the content of the questionnaire.
     * @return A fully constructed [Questionnaire] resource.
     */
    private fun createFhirQuestionnaire(
        id: String,
        title: String,
        items: List<Questionnaire.Item.Builder>,
    ): Questionnaire =
        Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                this.id = id
                this.title = FhirString(value = title).toBuilder()
                this.item.addAll(items)
            }.build()

    /**
     * Retrieves all predefined and currently stored custom questionnaires, localized to the target language.
     *
     * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "zh").
     * @return A List containing all available localized [Questionnaire] resources.
     */
    open fun getAvailableQuestionnaires(
        language: String = io.healthplatform.chartcam.utils.appLocaleState.value,
    ): List<Questionnaire> =
        inMemoryForms.values.map { localizeQuestionnaire(it, language) }

    /**
     * Retrieves a specific Questionnaire by its unique ID, localized to the target language.
     *
     * @param id The unique identifier of the desired Questionnaire.
     * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "zh").
     * @return The [Questionnaire] if found, or null if it does not exist.
     */
    open fun getQuestionnaire(
        id: kotlin.String,
        language: String = io.healthplatform.chartcam.utils.appLocaleState.value,
    ): Questionnaire? =
        inMemoryForms[id]?.let { localizeQuestionnaire(it, language) }

    /**
     * Localizes standard questionnaire titles and item questions according to language tag.
     *
     * @param questionnaire The original questionnaire.
     * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "zh").
     * @return A localized [Questionnaire] copy.
     */
    fun localizeQuestionnaire(
        questionnaire: Questionnaire,
        language: String,
    ): Questionnaire {
        val lang = language.lowercase().split("-", "_").first()
        if (lang == "en") return questionnaire

        val titleMap = StandardQuestionnaireTranslations.getTitleTranslations(questionnaire.id ?: "")
        val localizedTitle = titleMap[lang] ?: questionnaire.title?.value

        val newItems =
            questionnaire.item.map { item ->
                localizeItem(item, questionnaire.id ?: "", lang)
            }

        return questionnaire
            .toBuilder()
            .apply {
                if (localizedTitle != null) {
                    this.title = FhirString(value = localizedTitle).toBuilder()
                }
                this.item.clear()
                this.item.addAll(newItems)
            }.build()
    }

    /**
     * Localizes a Questionnaire.Item and its nested children.
     *
     * @param item The item to localize.
     * @param qId The questionnaire ID.
     * @param lang The language code.
     * @return The localized item builder.
     */
    private fun localizeItem(
        item: Questionnaire.Item,
        qId: String,
        lang: String,
    ): Questionnaire.Item.Builder {
        val linkId = item.linkId.value ?: ""
        val localizedText = StandardQuestionnaireTranslations.getItemTranslations(qId, linkId)[lang] ?: item.text?.value
        val builder = item.toBuilder()
        if (localizedText != null) {
            builder.text = FhirString(value = localizedText).toBuilder()
        }
        if (item.answerOption.isNotEmpty()) {
            val nestedOptions = item.answerOption.map { localizeAnswerOption(it, lang) }
            builder.answerOption.clear()
            builder.answerOption.addAll(nestedOptions)
        }
        if (item.item.isNotEmpty()) {
            val nested = item.item.map { localizeItem(it, qId, lang) }
            builder.item.clear()
            builder.item.addAll(nested)
        }
        return builder
    }

    /**
     * Localizes an answer option's display text.
     *
     * @param option The original answer option.
     * @param lang The target language code.
     * @return The localized answer option builder.
     */
    private fun localizeAnswerOption(
        option: Questionnaire.Item.AnswerOption,
        lang: String,
    ): Questionnaire.Item.AnswerOption.Builder {
        val builder = option.toBuilder()
        val optVal = option.value
        if (optVal is Questionnaire.Item.AnswerOption.Value.Coding) {
            val coding = optVal.value
            val display = coding.display?.value
            val code = coding.code?.value
            val raw = display ?: code ?: ""
            val translated = StandardQuestionnaireTranslations.getOptionTranslations(raw)[lang] ?: raw
            val codingBuilder = coding.toBuilder()
            codingBuilder.display = FhirString(value = translated).toBuilder()
            builder.value =
                Questionnaire.Item.AnswerOption.Value
                    .Coding(codingBuilder.build())
        } else if (optVal is Questionnaire.Item.AnswerOption.Value.String) {
            val raw = optVal.value.value ?: ""
            val translated = StandardQuestionnaireTranslations.getOptionTranslations(raw)[lang] ?: raw
            builder.value =
                Questionnaire.Item.AnswerOption.Value.String(
                    FhirString(value = translated),
                )
        }
        return builder
    }

    /**
     * Dynamically creates and stores a new custom Questionnaire with a specified number of photo attachment items.
     * The new questionnaire is immediately available via [getAvailableQuestionnaires] and [getQuestionnaire].
     *
     * @param title The display title for the new form.
     * @param photos The exact number of photo attachments required.
     * @param labels A comma-separated string of custom labels for each required photo item.
     * @return The newly created [Questionnaire] resource.
     */
    fun createQuestionnaire(
        title: kotlin.String,
        photos: Int,
        labels: kotlin.String = "",
    ): Questionnaire {
        val id = "custom-${title.lowercase().replace(" ", "-")}"
        val items =
            mutableListOf(
                createItem(
                    "notes",
                    "Clinical Notes",
                    QuestionnaireItemType.String,
                    required = false,
                ),
            )
        val parsedLabels = labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (i in 1..photos) {
            val labelStr =
                if (i - 1 < parsedLabels.size) {
                    parsedLabels[i - 1]
                } else {
                    (i - 1).toString()
                }
            items.add(
                createItem(
                    "photo_$i",
                    labelStr,
                    QuestionnaireItemType.Attachment,
                    required = true,
                ),
            )
        }
        val q = createFhirQuestionnaire(id, title, items)
        saveQuestionnaire(q)
        return q
    }

    /**
     * Saves an externally created Questionnaire to the repository synchronously in-memory
     * and dispatches persistence to the supervised [repositoryScope].
     *
     * @param questionnaire The Questionnaire to save.
     * @return A [Result] indicating success or a [ChartCamError.ValidationFailure] if ID is missing.
     */
    fun saveQuestionnaire(questionnaire: Questionnaire): Result<Unit> {
        val qId = questionnaire.id
        return if (qId != null) {
            inMemoryForms[qId] = questionnaire
            fhirRepository?.let { repo ->
                repositoryScope.launch {
                    repo.saveResource("Questionnaire", qId, questionnaire)
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Questionnaire ID must not be null"))
        }
    }

    /**
     * Saves an externally created Questionnaire and suspends until database persistence completes.
     *
     * @param questionnaire The Questionnaire to save.
     * @return A [Result] indicating success or failure of both in-memory and database persistence.
     */
    suspend fun saveQuestionnaireSuspending(questionnaire: Questionnaire): Result<Unit> {
        val qId = questionnaire.id
        return when {
            qId == null -> Result.failure(ChartCamError.ValidationFailure("Questionnaire ID must not be null"))
            else -> {
                inMemoryForms[qId] = questionnaire
                fhirRepository?.saveResource("Questionnaire", qId, questionnaire) ?: Result.success(Unit)
            }
        }
    }

    /**
     * Deletes a Questionnaire from the repository in-memory and dispatches deletion to [repositoryScope].
     *
     * @param id The ID of the Questionnaire to delete.
     * @return A [Result] indicating success or failure.
     */
    fun deleteQuestionnaire(id: kotlin.String): Result<Unit> {
        inMemoryForms.remove(id)
        fhirRepository?.let { repo ->
            repositoryScope.launch {
                repo.deleteResource("Questionnaire", id)
            }
        }
        return Result.success(Unit)
    }

    /**
     * Deletes a Questionnaire from the repository and suspends until database deletion completes.
     *
     * @param id The ID of the Questionnaire to delete.
     * @return A [Result] indicating success or failure of the deletion.
     */
    suspend fun deleteQuestionnaireSuspending(id: kotlin.String): Result<Unit> {
        inMemoryForms.remove(id)
        val repo = fhirRepository ?: return Result.success(Unit)
        return repo.deleteResource("Questionnaire", id)
    }
}
