/**
 * @file QuestionnaireRepository.kt
 * Contains declarations for QuestionnaireRepository.kt.
 *
 * Repository for providing and managing FHIR Questionnaire resources.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.ui.currentLanguageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
     * Loads the default questionnaire templates from bundled JSON resources.
     */
    suspend fun loadDefaultForms() {
        val fhirJson =
            com.google.fhir.model.r4
                .FhirR4Json()

        if (!inMemoryForms.containsKey("std-form")) {
            try {
                val stdBytes =
                    chartcam.chartcam.generated.resources.Res
                        .readBytes("files/default_templates/std-form.json")
                val stdQ = fhirJson.decodeFromString(stdBytes.decodeToString()) as Questionnaire
                inMemoryForms["std-form"] = stdQ
            } catch (e: IllegalArgumentException) {
                println("Error: ${e.message}")
            } catch (e: IllegalStateException) {
                println("Error: ${e.message}")
            }

            try {
                val basicBytes =
                    chartcam.chartcam.generated.resources.Res
                        .readBytes("files/default_templates/basic-followup.json")
                val basicQ = fhirJson.decodeFromString(basicBytes.decodeToString()) as Questionnaire
                inMemoryForms["basic-followup"] = basicQ
            } catch (e: IllegalArgumentException) {
                println("Error: ${e.message}")
            } catch (e: IllegalStateException) {
                println("Error: ${e.message}")
            }
        }

        kotlin
            .runCatching {
                fhirRepository?.let { repo ->
                    val entities =
                        repo.database.chartCamQueries
                            .getAllResourcesByType("Questionnaire")
                            .awaitAsList()
                    for (entity in entities) {
                        kotlin
                            .runCatching {
                                val q = fhirJson.decodeFromString(entity.serializedResource) as Questionnaire
                                q.id?.let { inMemoryForms[it] = q }
                            }.onFailure { println("Error: ${it.message}") }
                    }
                }
            }.onFailure { println("Error: ${it.message}") }
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
        linkId: kotlin.String,
        text: kotlin.String,
        type: Questionnaire.QuestionnaireItemType,
        required: kotlin.Boolean,
    ): Questionnaire.Item.Builder =
        Questionnaire.Item
            .Builder(
                com.google.fhir.model.r4.String
                    .Builder()
                    .apply { value = linkId },
                Enumeration(value = type),
            ).apply {
                this.text =
                    com.google.fhir.model.r4.String
                        .Builder()
                        .apply { value = text }
                this.required =
                    com.google.fhir.model.r4.Boolean
                        .Builder()
                        .apply { value = required }
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
        id: kotlin.String,
        title: kotlin.String,
        items: List<Questionnaire.Item.Builder>,
    ): Questionnaire =
        Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                this.id = id
                this.title =
                    com.google.fhir.model.r4.String
                        .Builder()
                        .apply { value = title }
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
                    this.title =
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = localizedTitle }
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
            builder.text =
                com.google.fhir.model.r4.String
                    .Builder()
                    .apply { value = localizedText }
        }
        if (item.item.isNotEmpty()) {
            val nested = item.item.map { localizeItem(it, qId, lang) }
            builder.item.clear()
            builder.item.addAll(nested)
        }
        return builder
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
            val labelStr = parsedLabels.getOrNull(i - 1) ?: (i - 1).toString()
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
     */
    fun saveQuestionnaire(questionnaire: Questionnaire) {
        val qId = questionnaire.id
        if (qId != null) {
            inMemoryForms[qId] = questionnaire
            fhirRepository?.let { repo ->
                CoroutineScope(Dispatchers.Default).launch {
                    repo.saveResource("Questionnaire", qId, questionnaire)
                }
            }
        }
    }

    /**
     * Deletes a Questionnaire from the repository.
     *
     * @param id The ID of the Questionnaire to delete.
     */
    fun deleteQuestionnaire(id: kotlin.String) {
        inMemoryForms.remove(id)
        fhirRepository?.let { repo ->
            CoroutineScope(Dispatchers.Default).launch {
                repo.deleteResource("Questionnaire", id)
            }
        }
    }
}
