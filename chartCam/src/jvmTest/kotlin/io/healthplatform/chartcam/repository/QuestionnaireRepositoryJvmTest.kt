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

class QuestionnaireRepositoryJvmTest {
    class FakeFhirRepo :
        FhirRepository(
            io.healthplatform.chartcam.database.ChartCamDatabase(
                app.cash.sqldelight.driver.jdbc.sqlite
                    .JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY),
            ),
        ) {
        val savedResources = mutableMapOf<String, com.google.fhir.model.r4.Resource>()
        val deletedResources = mutableListOf<String>()

        override suspend fun saveResource(
            type: String,
            id: String,
            resource: com.google.fhir.model.r4.Resource,
            isLocalChange: Boolean,
        ) {
            savedResources[id] = resource
        }

        override suspend fun deleteResource(
            type: String,
            id: String,
            isLocalChange: Boolean,
        ) {
            deletedResources.add(id)
        }
    }

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

    @Test
    fun testLoadDefaultFormsExceptions() =
        runTest {
            val nullRepo = QuestionnaireRepository(null)
            nullRepo.loadDefaultForms() // Shouldn't crash
            assertTrue(true)
        }

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

    @Test
    fun testCreateQuestionnaireWithFewerLabels() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("Test 2", 2, "Label A")

        assertEquals(3, q.item.size)
        assertEquals("Label A", q.item[1].text?.value)
        assertEquals("1", q.item[2].text?.value)
    }

    @Test
    fun testGetAvailableQuestionnaires() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("Test Title", 1)
        val available = qrRepo.getAvailableQuestionnaires()

        assertTrue(available.any { it.id == q.id })
    }

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
