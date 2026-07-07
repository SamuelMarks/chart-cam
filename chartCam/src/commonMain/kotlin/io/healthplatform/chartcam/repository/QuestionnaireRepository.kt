/**
 * Repository for providing and managing FHIR Questionnaire resources.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus

/**
 * Repository to manage Questionnaire forms available for clinical encounters.
 * Currently stores forms in memory and provides both predefined templates and the ability to generate custom forms.
 */
class QuestionnaireRepository {
    /**
     * In-memory storage mapping questionnaire IDs to their respective FHIR Questionnaire resources.
     */
    private val inMemoryForms = mutableMapOf<String, Questionnaire>()

    init {
        inMemoryForms["std-form"] =
            createFhirQuestionnaire(
                id = "std-form",
                title = "Standard Clinical Photo",
                items =
                    listOf(
                        createItem("notes", "Clinical Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                        createItem("front", "Front", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("front_ruler", "Front + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("right", "Right Side", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("right_ruler", "Right Side + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("back", "Back", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("back_ruler", "Back + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("left", "Left Side", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("left_ruler", "Left Side + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                    ),
            )

        val choiceItem =
            createItem("followup_type", "Type of Follow-up", Questionnaire.QuestionnaireItemType.Choice, required = true).apply {
                answerOption.add(
                    Questionnaire.Item.AnswerOption.Builder(
                        Questionnaire.Item.AnswerOption.Value
                            .String(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "Routine" }
                                    .build(),
                            ),
                    ),
                )
                answerOption.add(
                    Questionnaire.Item.AnswerOption.Builder(
                        Questionnaire.Item.AnswerOption.Value
                            .String(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "Urgent" }
                                    .build(),
                            ),
                    ),
                )
            }

        val conditionItem =
            createItem("urgent_reason", "Reason for Urgency", Questionnaire.QuestionnaireItemType.String, required = true).apply {
                enableWhen.add(
                    Questionnaire.Item.EnableWhen.Builder(
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = "followup_type" },
                        Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                        Questionnaire.Item.EnableWhen.Answer.String(
                            com.google.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Urgent" }
                                .build(),
                        ),
                    ),
                )
            }

        val booleanItem =
            createItem("patient_consent", "Patient consented to photos", Questionnaire.QuestionnaireItemType.Boolean, required = true)

        inMemoryForms["basic-followup"] =
            createFhirQuestionnaire(
                id = "basic-followup",
                title = "Basic Follow-up",
                items =
                    listOf(
                        createItem("notes", "Follow-up Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                        choiceItem,
                        conditionItem,
                        booleanItem,
                        createItem("front", "Front View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("left", "Left View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                        createItem("right", "Right View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                    ),
            )
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
        inMemoryForms[id] = q
        inMemoryForms[q.id ?: ""] = q
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
        }
    }
}
