package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.terminologies.PublicationStatus

/**
 * Repository to manage Questionnaire forms available for encounters.
 */
class QuestionnaireRepository {

    private val inMemoryForms = mutableMapOf<String, Questionnaire>()

    init {
        inMemoryForms["std-form"] = createFhirQuestionnaire(
            id = "std-form",
            title = "Standard Clinical Photo",
            items = listOf(
                createItem("notes", "Clinical Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                createItem("front", "Front", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("front_ruler", "Front + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("right", "Right Side", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("right_ruler", "Right Side + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("back", "Back", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("back_ruler", "Back + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("left", "Left Side", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("left_ruler", "Left Side + Ruler", Questionnaire.QuestionnaireItemType.Attachment, required = true)
            )
        )

        val choiceItem = createItem("followup_type", "Type of Follow-up", Questionnaire.QuestionnaireItemType.Choice, required = true).apply {
            answerOption.add(Questionnaire.Item.AnswerOption.Builder(
                Questionnaire.Item.AnswerOption.Value.String(com.google.fhir.model.r4.String.Builder().apply { value = "Routine" }.build())
            ))
            answerOption.add(Questionnaire.Item.AnswerOption.Builder(
                Questionnaire.Item.AnswerOption.Value.String(com.google.fhir.model.r4.String.Builder().apply { value = "Urgent" }.build())
            ))
        }

        val conditionItem = createItem("urgent_reason", "Reason for Urgency", Questionnaire.QuestionnaireItemType.String, required = true).apply {
            enableWhen.add(
                Questionnaire.Item.EnableWhen.Builder(
                    com.google.fhir.model.r4.String.Builder().apply { value = "followup_type" },
                    Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                    Questionnaire.Item.EnableWhen.Answer.String(com.google.fhir.model.r4.String.Builder().apply { value = "Urgent" }.build())
                )
            )
        }

        val booleanItem = createItem("patient_consent", "Patient consented to photos", Questionnaire.QuestionnaireItemType.Boolean, required = true)

        inMemoryForms["basic-followup"] = createFhirQuestionnaire(
            id = "basic-followup",
            title = "Basic Follow-up",
            items = listOf(
                createItem("notes", "Follow-up Notes", Questionnaire.QuestionnaireItemType.String, required = false),
                choiceItem,
                conditionItem,
                booleanItem,
                createItem("front", "Front View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("left", "Left View", Questionnaire.QuestionnaireItemType.Attachment, required = true),
                createItem("right", "Right View", Questionnaire.QuestionnaireItemType.Attachment, required = true)
            )
        )
    }

    private fun createItem(
        linkId: kotlin.String,
        text: kotlin.String,
        type: Questionnaire.QuestionnaireItemType,
        required: kotlin.Boolean
    ): Questionnaire.Item.Builder {
        return Questionnaire.Item.Builder(
            com.google.fhir.model.r4.String.Builder().apply { value = linkId },
            Enumeration(value = type)
        ).apply {
            this.text = com.google.fhir.model.r4.String.Builder().apply { value = text }
            this.required = com.google.fhir.model.r4.Boolean.Builder().apply { value = required }
        }
    }

    private fun createFhirQuestionnaire(
        id: kotlin.String,
        title: kotlin.String,
        items: List<Questionnaire.Item.Builder>
    ): Questionnaire {
        return Questionnaire.Builder(Enumeration(value = PublicationStatus.Active)).apply {
            this.id = id
            this.title = com.google.fhir.model.r4.String.Builder().apply { value = title }
            this.item.addAll(items)
        }.build()
    }

    /**
     * Gets all predefined and custom questionnaires.
     * @return List of Questionnaire
     */
    fun getAvailableQuestionnaires(): List<Questionnaire> {
        return inMemoryForms.values.toList()
    }

    /**
     * Gets a specific Questionnaire by its ID.
     * @param id The Questionnaire ID.
     * @return The Questionnaire or null if not found.
     */
    fun getQuestionnaire(id: kotlin.String): Questionnaire? {
        return inMemoryForms[id]
    }

    /**
     * Creates a new custom Questionnaire with a specified number of photos.
     * @param title The form title.
     * @param photos Number of photo attachments to require.
     * @return The created Questionnaire.
     */
    fun createQuestionnaire(title: kotlin.String, photos: Int): Questionnaire {
        val id = "custom-${title.lowercase().replace(" ", "-")}"
        val items = mutableListOf(createItem("notes", "Clinical Notes", Questionnaire.QuestionnaireItemType.String, required = false))
        for (i in 1..photos) {
            items.add(createItem("photo_$i", "Photo $i", Questionnaire.QuestionnaireItemType.Attachment, required = true))
        }
        val q = createFhirQuestionnaire(id, title, items)
        inMemoryForms[id] = q
        return q
    }
}
