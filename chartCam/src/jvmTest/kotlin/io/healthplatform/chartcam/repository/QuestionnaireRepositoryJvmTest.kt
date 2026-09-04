/**
 * @file QuestionnaireRepositoryJvmTest.kt
 * Contains declarations for QuestionnaireRepositoryJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for QuestionnaireRepository on JVM.
 */
class QuestionnaireRepositoryJvmTest {
    /**
     * Fake FHIR repository for testing.
     */
    class FakeFhirRepo :
        FhirRepository(
            io.healthplatform.chartcam.database.ChartCamDatabase(
                app.cash.sqldelight.driver.jdbc.sqlite
                    .JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY),
            ),
        ) {
        /** Map of saved resources. */
        val savedResources = mutableMapOf<String, com.google.fhir.model.r4.Resource>()

        /** List of deleted resources. */
        val deletedResources = mutableListOf<String>()

        /**
         * Save resource.
         * @param resourceType The resource type.
         * @param resourceId The resource id.
         * @param resource The resource itself.
         * @param isLocalChange If it's a local change.
         */
        override suspend fun saveResource(
            resourceType: String,
            resourceId: String,
            resource: com.google.fhir.model.r4.Resource,
            isLocalChange: Boolean,
        ) {
            savedResources[resourceId] = resource
        }

        /**
         * Delete resource.
         * @param resourceType The resource type.
         * @param resourceId The resource id.
         * @param isLocalChange If it's a local change.
         */
        override suspend fun deleteResource(
            resourceType: String,
            resourceId: String,
            isLocalChange: Boolean,
        ) {
            deletedResources.add(resourceId)
        }
    }

    /**
     * Tests loading default forms.
     */
    @Test
    fun testLoadDefaultForms() =
        runTest {
            val fhirRepo = FakeFhirRepo()
            val qrRepo = QuestionnaireRepository(fhirRepo)

            qrRepo.loadDefaultForms()

            val q1 = qrRepo.getQuestionnaire("std-form")
            val q2 = qrRepo.getQuestionnaire("basic-followup")

            // Default forms should be loaded
            assertNotNull(q1)
            assertNotNull(q2)
        }

    /**
     * Tests loading default forms exceptions.
     */
    @Test
    fun testLoadDefaultFormsExceptions() =
        runTest {
            val nullRepo = QuestionnaireRepository(null)
            nullRepo.loadDefaultForms() // Shouldn't crash
            assertTrue(true)
        }

    /**
     * Tests creating a questionnaire.
     */
    @Test
    fun testCreateQuestionnaire() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("Test Title", 2, "Label A, Label B")

        assertEquals("custom-test-title", q.id)
        assertEquals("Test Title", q.title?.value)
        assertEquals(3, q.item.size)
        assertEquals("notes", q.item[0].linkId.value)
        assertEquals("photo_1", q.item[1].linkId.value)
        assertEquals("Label A", q.item[1].text?.value)
        assertEquals("photo_2", q.item[2].linkId.value)
        assertEquals("Label B", q.item[2].text?.value)
    }

    /**
     * Tests creating a questionnaire with fewer labels.
     */
    @Test
    fun testCreateQuestionnaireWithFewerLabels() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("Test 2", 2, "Label A")

        assertEquals(3, q.item.size)
        assertEquals("Label A", q.item[1].text?.value)
        assertEquals("1", q.item[2].text?.value)
    }

    /**
     * Tests getting available questionnaires.
     */
    @Test
    fun testGetAvailableQuestionnaires() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("Test Title", 1)
        val available = qrRepo.getAvailableQuestionnaires()

        assertTrue(available.any { it.id == q.id })
    }

    /**
     * Tests saving and getting a questionnaire.
     */
    @Test
    fun testSaveAndGetQuestionnaire() =
        kotlinx.coroutines.runBlocking {
            val fhirRepo = FakeFhirRepo()
            val qrRepo = QuestionnaireRepository(fhirRepo)
            val q =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "q-save"
                    }.build()

            qrRepo.saveQuestionnaire(q)
            val retrieved = qrRepo.getQuestionnaire("q-save")
            assertNotNull(retrieved)

            kotlinx.coroutines.delay(100)
            assertEquals(q, fhirRepo.savedResources["q-save"])
        }

    /**
     * Tests deleting a questionnaire.
     */
    @Test
    fun testDeleteQuestionnaire() =
        kotlinx.coroutines.runBlocking {
            val fhirRepo = FakeFhirRepo()
            val qrRepo = QuestionnaireRepository(fhirRepo)
            val q =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "q-delete"
                    }.build()
            qrRepo.saveQuestionnaire(q)
            assertNotNull(qrRepo.getQuestionnaire("q-delete"))

            qrRepo.deleteQuestionnaire("q-delete")
            assertNull(qrRepo.getQuestionnaire("q-delete"))

            kotlinx.coroutines.delay(100)
            assertTrue(fhirRepo.deletedResources.contains("q-delete"))
        }
}
