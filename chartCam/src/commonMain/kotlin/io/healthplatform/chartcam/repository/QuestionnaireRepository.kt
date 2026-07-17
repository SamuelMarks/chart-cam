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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val basicBytes =
                    chartcam.chartcam.generated.resources.Res
                        .readBytes("files/default_templates/basic-followup.json")
                val basicQ = fhirJson.decodeFromString(basicBytes.decodeToString()) as Questionnaire
                inMemoryForms["basic-followup"] = basicQ
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            fhirRepository?.let { repo ->
                val entities =
                    repo.database.chartCamQueries
                        .getAllResourcesByType("Questionnaire")
                        .awaitAsList()
                for (entity in entities) {
                    try {
                        val q = fhirJson.decodeFromString(entity.serializedResource) as Questionnaire
                        q.id?.let { inMemoryForms[it] = q }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
     * Retrieves all predefined and currently stored custom questionnaires.
     *
     * @return A List containing all available [Questionnaire] resources.
     */
    fun getAvailableQuestionnaires(): List<Questionnaire> = inMemoryForms.values.toList()

    /**
     * Retrieves a specific Questionnaire by its unique ID.
     *
     * @param id The unique identifier of the desired Questionnaire.
     * @return The [Questionnaire] if found, or null if it does not exist.
     */
    fun getQuestionnaire(id: kotlin.String): Questionnaire? = inMemoryForms[id]

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
        val items = mutableListOf(createItem("notes", "Clinical Notes", Questionnaire.QuestionnaireItemType.String, required = false))
        val parsedLabels = labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (i in 1..photos) {
            val labelStr = parsedLabels.getOrNull(i - 1) ?: (i - 1).toString()
            items.add(createItem("photo_$i", labelStr, Questionnaire.QuestionnaireItemType.Attachment, required = true))
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
