package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.models.Questionnaire
import io.healthplatform.chartcam.models.QuestionnaireItem

/**
 * Repository to manage Questionnaire forms available for encounters.
 */
class QuestionnaireRepository {

    private val inMemoryForms = mutableMapOf(
        "std-form" to Questionnaire(
            id = "std-form",
            title = "Standard Clinical Photo",
            status = "active",
            item = listOf(
                QuestionnaireItem("notes", "Clinical Notes", "string"),
                QuestionnaireItem("front", "Front", "attachment", required = true),
                QuestionnaireItem("front_ruler", "Front + Ruler", "attachment", required = true),
                QuestionnaireItem("right", "Right Side", "attachment", required = true),
                QuestionnaireItem("right_ruler", "Right Side + Ruler", "attachment", required = true),
                QuestionnaireItem("back", "Back", "attachment", required = true),
                QuestionnaireItem("back_ruler", "Back + Ruler", "attachment", required = true),
                QuestionnaireItem("left", "Left Side", "attachment", required = true),
                QuestionnaireItem("left_ruler", "Left Side + Ruler", "attachment", required = true)
            )
        ),
        "basic-followup" to Questionnaire(
            id = "basic-followup",
            title = "Basic Follow-up",
            status = "active",
            item = listOf(
                QuestionnaireItem("notes", "Follow-up Notes", "string"),
                QuestionnaireItem("front", "Front View", "attachment", required = true),
                QuestionnaireItem("left", "Left View", "attachment", required = true),
                QuestionnaireItem("right", "Right View", "attachment", required = true)
            )
        )
    )

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
    fun getQuestionnaire(id: String): Questionnaire? {
        return inMemoryForms[id]
    }

    /**
     * Creates a new custom Questionnaire with a specified number of photos.
     * @param title The form title.
     * @param photos Number of photo attachments to require.
     * @return The created Questionnaire.
     */
    fun createQuestionnaire(title: String, photos: Int): Questionnaire {
        val id = "custom-${title.lowercase().replace(" ", "-")}"
        val items = mutableListOf(QuestionnaireItem("notes", "Clinical Notes", "string"))
        for (i in 1..photos) {
            items.add(QuestionnaireItem("photo_$i", "Photo $i", "attachment", required = true))
        }
        val q = Questionnaire(id, title, "active", items)
        inMemoryForms[id] = q
        return q
    }
}
