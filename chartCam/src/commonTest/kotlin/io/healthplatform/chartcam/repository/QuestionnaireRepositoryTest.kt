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

    /**
     * Validates that localizeQuestionnaire translates bundled questionnaire titles and item labels.
     */
    @Test
    fun testLocalizeQuestionnaire() {
        val repo = QuestionnaireRepository()
        val itemNotes =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "notes" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = String.Builder().apply { value = "Clinical Notes" }
                }.build()

        val stdQ =
            Questionnaire
                .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                .apply {
                    id = "std-form"
                    title = String.Builder().apply { value = "Standard Clinical Photo" }
                    item.add(itemNotes.toBuilder())
                }.build()

        val enQ = repo.localizeQuestionnaire(stdQ, "en")
        assertEquals("Standard Clinical Photo", enQ.title?.value)
        assertEquals(
            "Clinical Notes",
            enQ.item
                .first()
                .text
                ?.value,
        )

        val esQ = repo.localizeQuestionnaire(stdQ, "es")
        assertEquals("Formulario Clínico Estándar", esQ.title?.value)
        assertEquals(
            "Notas Clínicas",
            esQ.item
                .first()
                .text
                ?.value,
        )

        val jaQ = repo.localizeQuestionnaire(stdQ, "ja")
        assertEquals("標準臨床問診票", jaQ.title?.value)
        assertEquals(
            "臨床記録",
            jaQ.item
                .first()
                .text
                ?.value,
        )

        val heQ = repo.localizeQuestionnaire(stdQ, "he")
        assertEquals("טופס קליני סטנדרטי", heQ.title?.value)
        assertEquals(
            "הערות קליניות",
            heQ.item
                .first()
                .text
                ?.value,
        )

        val zhQ = repo.localizeQuestionnaire(stdQ, "zh")
        assertEquals("標準臨床問診表", zhQ.title?.value)
        assertEquals(
            "臨床筆記",
            zhQ.item
                .first()
                .text
                ?.value,
        )
    }
}
