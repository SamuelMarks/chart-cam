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
import io.healthplatform.chartcam.ui.currentLanguageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Repository to manage Questionnaire forms available for clinical encounters.
 * Currently stores forms in memory and provides both predefined templates and the ability to generate custom forms.
 */
class QuestionnaireRepository(
    private val fhirRepository: FhirRepository? = null,
) {
    /**
     * In-memory storage mapping questionnaire IDs to their respective FHIR Questionnaire resources.
     */
    private val inMemoryForms = mutableMapOf<String, Questionnaire>()

    /**
     * Loads a single questionnaire template from bundled JSON resources.
     *
     * @param path The resource path.
     * @param id The template ID.
     */
    internal suspend fun loadTemplate(path: String, id: String) {
        val q =
            runCatching {
                val bytes =
                    chartcam.chartcam.generated.resources.Res
                        .readBytes(path)
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .decodeTypedResource(Questionnaire.serializer(), bytes.decodeToString())
                    .getOrNull()
            }.getOrNull() ?: createDefaultFallbackQuestionnaire(id)
                ?: Questionnaire(status = Enumeration(value = PublicationStatus.Active))

        inMemoryForms[id] = q
    }

    /**
     * Creates a fallback Questionnaire when bundled JSON file cannot be read from resources.
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
                Questionnaire.QuestionnaireItemType.Text,
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
        createItem(linkId, text, Questionnaire.QuestionnaireItemType.Attachment, required = true).apply {
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
                createItem("notes", "Clinical Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                createItem("front", "Front", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("front_ruler", "Front + Ruler", Questionnaire.QuestionnaireItemType.Attachment, true),
                createItem("right", "Right Side", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("right_ruler", "Right Side + Ruler", Questionnaire.QuestionnaireItemType.Attachment, true),
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
                createItem("notes", "Follow-up Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                createItem("front", "Front View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("right", "Right View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
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
            if (q != null) {
                val qId = q.id
                if (qId != null) {
                    inMemoryForms[qId] = q
                }
            }
        }
    }

    /**
     * Loads the default questionnaire templates from bundled JSON resources.
     */
    suspend fun loadDefaultForms() {
        if (!inMemoryForms.containsKey("std-form")) {
            loadTemplate("files/default_templates/std-form.json", "std-form")
            loadTemplate("files/default_templates/basic-followup.json", "basic-followup")
            loadTemplate("files/default_templates/facial-cornea-profile.json", "facial-cornea-profile")
        }

        val repo = fhirRepository
        if (repo != null) {
            loadDatabaseForms(repo)
        }
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
        type: Questionnaire.QuestionnaireItemType,
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
     * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "zh"). Defaults to the current in-app language.
     * @return A List containing all available localized [Questionnaire] resources.
     */
    fun getAvailableQuestionnaires(language: String = currentLanguageState.value): List<Questionnaire> =
        inMemoryForms.values.map { localizeQuestionnaire(it, language) }

    /**
     * Retrieves a specific Questionnaire by its unique ID, localized to the target language.
     *
     * @param id The unique identifier of the desired Questionnaire.
     * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "zh"). Defaults to the current in-app language.
     * @return The [Questionnaire] if found, or null if it does not exist.
     */
    fun getQuestionnaire(
        id: kotlin.String,
        language: String = currentLanguageState.value,
    ): Questionnaire? = inMemoryForms[id]?.let { localizeQuestionnaire(it, language) }

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

        val titleMap = getStandardTitleTranslations(questionnaire.id ?: "")
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
        val localizedText = getStandardItemTranslations(qId, linkId)[lang] ?: item.text?.value
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
            val raw =
                if (display != null) {
                    display
                } else if (code != null) {
                    code
                } else {
                    ""
                }
            val translated = getStandardOptionTranslations(raw)[lang] ?: raw
            val codingBuilder = coding.toBuilder()
            codingBuilder.display = FhirString(value = translated).toBuilder()
            builder.value =
                Questionnaire.Item.AnswerOption.Value
                    .Coding(codingBuilder.build())
        } else if (optVal is Questionnaire.Item.AnswerOption.Value.String) {
            val raw = optVal.value.value ?: ""
            val translated = getStandardOptionTranslations(raw)[lang] ?: raw
            builder.value =
                Questionnaire.Item.AnswerOption.Value.String(
                    FhirString(value = translated),
                )
        }
        return builder
    }

    /**
     * Returns translations for standard answer options.
     *
     * @param optionValue The raw option value or display.
     * @return Map of language code to translated option text.
     */
    private fun getStandardOptionTranslations(optionValue: String): Map<String, String> =
        when (optionValue.lowercase()) {
            "routine" ->
                mapOf(
                    "es" to "Rutina",
                    "ja" to "定期",
                    "he" to "שגרתי",
                    "zh" to "常規",
                )
            "urgent" ->
                mapOf(
                    "es" to "Urgente",
                    "ja" to "緊急",
                    "he" to "דחוף",
                    "zh" to "緊急",
                )
            else -> emptyMap()
        }

    /**
     * Returns standard translated titles for bundled forms.
     *
     * @param qId The questionnaire template ID.
     * @return Map of language code to translated title.
     */
    private fun getStandardTitleTranslations(qId: String): Map<String, String> =
        when (qId) {
            "std-form" ->
                mapOf(
                    "es" to "Formulario Clínico Estándar",
                    "ja" to "標準臨床問診票",
                    "he" to "טופס קליני סטנדרטי",
                    "zh" to "標準臨床問診表",
                )
            "basic-followup" ->
                mapOf(
                    "es" to "Seguimiento Básico",
                    "ja" to "基本フォローアップ",
                    "he" to "מעקב בסיסי",
                    "zh" to "基本追蹤",
                )
            "facial-cornea-profile" ->
                mapOf(
                    "es" to "Examen de perfil facial y córnea",
                    "ja" to "顔貌側面および角膜検査",
                    "he" to "בדיקת פרופיל פנים וקרנית",
                    "zh" to "面部側臉與角膜檢查",
                )
            else -> emptyMap()
        }

    /**
     * Returns standard translated item text for bundled forms.
     *
     * @param qId The questionnaire template ID.
     * @param linkId The item linkId.
     * @return Map of language code to translated item question text.
     */
    private fun getStandardItemTranslations(
        qId: String,
        linkId: String,
    ): Map<String, String> =
        when (qId) {
            "std-form" -> getStdFormItemTranslations(linkId)
            "basic-followup" -> getBasicFollowupItemTranslations(linkId)
            "facial-cornea-profile" -> getFacialCorneaProfileItemTranslations(linkId)
            else -> emptyMap()
        }

    /**
     * Item translations for the facial profile and cornea examination template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getFacialCorneaProfileItemTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "clinical_notes" ->
                mapOf(
                    "es" to "Observaciones clínicas y notas",
                    "ja" to "臨床所見およびメモ",
                    "he" to "תצפיות קליניות והערות",
                    "zh" to "臨床觀察與備註",
                )
            "profile_left" ->
                mapOf(
                    "es" to "Perfil izquierdo (córnea y nariz)",
                    "ja" to "左側面プロファイル（角膜・鼻）",
                    "he" to "פרופיל שמאל (קרנית ואף)",
                    "zh" to "左側臉（角膜與鼻子）",
                )
            "front_view" ->
                mapOf(
                    "es" to "Vista frontal",
                    "ja" to "正面視",
                    "he" to "מבט חזיתי",
                    "zh" to "正臉視角",
                )
            "profile_right" ->
                mapOf(
                    "es" to "Perfil derecho (córnea y nariz)",
                    "ja" to "右側面プロファイル（角膜・鼻）",
                    "he" to "פרופיל ימין (קרנית ואף)",
                    "zh" to "右側臉（角膜與鼻子）",
                )
            else -> emptyMap()
        }

    /**
     * Item translations for the standard clinical photo template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormItemTranslations(linkId: String): Map<String, String> =
        getStdFormGeneralTranslations(linkId).ifEmpty { getStdFormPhotoTranslations(linkId) }

    /**
     * General translations for standard form.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormGeneralTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "notes" ->
                mapOf(
                    "es" to "Notas Clínicas",
                    "ja" to "臨床記録",
                    "he" to "הערות קליניות",
                    "zh" to "臨床筆記",
                )
            "front" ->
                mapOf(
                    "es" to "Frente",
                    "ja" to "正面",
                    "he" to "חזית",
                    "zh" to "正面",
                )
            "front_ruler" ->
                mapOf(
                    "es" to "Frente + Regla",
                    "ja" to "正面 + 定規",
                    "he" to "חזית + סרגל",
                    "zh" to "正面 + 尺",
                )
            else -> emptyMap()
        }

    /**
     * Photo orientation translations for standard form.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormPhotoTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "right" ->
                mapOf(
                    "es" to "Lado Derecho",
                    "ja" to "右側",
                    "he" to "צד ימין",
                    "zh" to "右側",
                )
            "right_ruler" ->
                mapOf(
                    "es" to "Lado Derecho + Regla",
                    "ja" to "右側 + 定規",
                    "he" to "צד ימין + סרגל",
                    "zh" to "右側 + 尺",
                )
            "back" ->
                mapOf(
                    "es" to "Espalda",
                    "ja" to "背面",
                    "he" to "גב",
                    "zh" to "背面",
                )
            "back_ruler" ->
                mapOf(
                    "es" to "Espalda + Regla",
                    "ja" to "背面 + 定規",
                    "he" to "גב + סרגל",
                    "zh" to "背面 + 尺",
                )
            "left" ->
                mapOf(
                    "es" to "Lado Izquierdo",
                    "ja" to "左側",
                    "he" to "צד שמאל",
                    "zh" to "左側",
                )
            "left_ruler" ->
                mapOf(
                    "es" to "Lado Izquierdo + Regla",
                    "ja" to "左側 + 定規",
                    "he" to "צד שמאל + סרגל",
                    "zh" to "左側 + 尺",
                )
            else -> emptyMap()
        }

    /**
     * Item translations for the basic follow-up template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getBasicFollowupItemTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "notes" ->
                mapOf(
                    "es" to "Notas de seguimiento",
                    "ja" to "フォローアップ記録",
                    "he" to "הערות מעקב",
                    "zh" to "追蹤記錄",
                )
            "followup_type" ->
                mapOf(
                    "es" to "Tipo de seguimiento",
                    "ja" to "フォローアップの種類",
                    "he" to "סוג המעקב",
                    "zh" to "追蹤類型",
                )
            "urgent_reason" ->
                mapOf(
                    "es" to "Motivo de urgencia",
                    "ja" to "緊急の理由",
                    "he" to "סיבת הדחיפות",
                    "zh" to "緊急原因",
                )
            "patient_consent" ->
                mapOf(
                    "es" to "El paciente consintió las fotos",
                    "ja" to "患者が写真撮影に同意しました",
                    "he" to "המטופל הסכים לצילום תמונות",
                    "zh" to "病患已同意拍攝相片",
                )
            "front" ->
                mapOf(
                    "es" to "Vista frontal",
                    "ja" to "正面写真",
                    "he" to "מבט חזיתי",
                    "zh" to "正面視圖",
                )
            else -> emptyMap()
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
                    Questionnaire.QuestionnaireItemType.String,
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
                    Questionnaire.QuestionnaireItemType.Attachment,
                    required = true,
                ),
            )
        }
        val q = createFhirQuestionnaire(id, title, items)
        saveQuestionnaire(q)
        return q
    }

    /**
     * Saves an externally created Questionnaire to the repository.
     *
     * @param questionnaire The Questionnaire to save.
     * @return A [Result] indicating success or failure.
     */
    fun saveQuestionnaire(questionnaire: Questionnaire): Result<Unit> {
        val qId = questionnaire.id
        return if (qId != null) {
            inMemoryForms[qId] = questionnaire
            fhirRepository?.let { repo ->
                CoroutineScope(Dispatchers.Default).launch {
                    repo.saveResource("Questionnaire", qId, questionnaire)
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Questionnaire ID must not be null"))
        }
    }

    /**
     * Deletes a Questionnaire from the repository.
     *
     * @param id The ID of the Questionnaire to delete.
     * @return A [Result] indicating success or failure.
     */
    fun deleteQuestionnaire(id: kotlin.String): Result<Unit> {
        inMemoryForms.remove(id)
        fhirRepository?.let { repo ->
            CoroutineScope(Dispatchers.Default).launch {
                repo.deleteResource("Questionnaire", id)
            }
        }
        return Result.success(Unit)
    }
}
