/**
 * @file QuestionnaireRepositoryTest.kt
 * Contains tests for Questionnaire repository operations.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fake implementation of a Questionnaire Repository for testing CRUD operations
 * without relying on a platform-specific SQLDelight driver.
 */
class FakeQuestionnaireRepository {
    private val memoryDb = mutableMapOf<kotlin.String, Questionnaire>()

    /**
     * Saves a Questionnaire.
     * @param questionnaire The questionnaire.
     */
    fun saveQuestionnaire(questionnaire: Questionnaire) {
        val id = questionnaire.id ?: throw IllegalArgumentException("ID is required")
        memoryDb[id] = questionnaire
    }

    /**
     * Gets a Questionnaire by ID.
     * @param id The ID.
     * @return The questionnaire or null.
     */
    fun getQuestionnaire(id: kotlin.String): Questionnaire? = memoryDb[id]

    /**
     * Deletes a Questionnaire.
     * @param id The ID.
     */
    fun deleteQuestionnaire(id: kotlin.String) {
        memoryDb.remove(id)
    }

    /**
     * Gets all Questionnaires.
     * @return List of all Questionnaires.
     */
    fun getAllQuestionnaires(): List<Questionnaire> = memoryDb.values.toList()
}

/**
 * Common test logic for Questionnaire repository operations.
 */
class QuestionnaireRepositoryTest {
    /**
     * Validates CRUD operations for Questionnaire schemas.
     */
    @Test
    fun testQuestionnaireCrudOperations() =
        runTest {
            val repo = FakeQuestionnaireRepository()

            val questionnaireId = "q-123"
            val questionnaire =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = questionnaireId
                        title = String.Builder().apply { value = "Patient Intake Form" }
                    }.build()

            // 1. Create (Insert)
            repo.saveQuestionnaire(questionnaire)

            // 2. Read (Select)
            val retrieved = repo.getQuestionnaire(questionnaireId)
            assertNotNull(retrieved, "Questionnaire should be retrieved successfully")
            assertEquals("Patient Intake Form", retrieved.title?.value)

            // 3. Update (Replace)
            val updatedQuestionnaire =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = questionnaireId
                        title = String.Builder().apply { value = "Updated Intake Form" }
                    }.build()
            repo.saveQuestionnaire(updatedQuestionnaire)

            val updatedRetrieved = repo.getQuestionnaire(questionnaireId)
            assertNotNull(updatedRetrieved, "Updated questionnaire should be retrieved")
            assertEquals("Updated Intake Form", updatedRetrieved.title?.value)

            // 4. Delete
            repo.deleteQuestionnaire(questionnaireId)
            assertNull(repo.getQuestionnaire(questionnaireId), "Questionnaire should be deleted")
        }
}
