/**
 * @file QuestionnaireRepositoryJvmTest.kt
 * Contains declarations for QuestionnaireRepositoryJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
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
    class FakeFhirRepo(
        db: ChartCamDatabase = createTestDb(),
    ) : FhirRepository(db) {
        companion object {
            /**
             * Creates in-memory test database with schema initialized.
             * @return Initialized [ChartCamDatabase].
             */
            fun createTestDb(): ChartCamDatabase {
                val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
                ChartCamDatabase.Schema.synchronous().create(driver)
                return ChartCamDatabase(driver)
            }
        }

        /** Map of saved resources. */
        val savedResources = mutableMapOf<String, dev.ohs.fhir.model.r4.Resource>()

        /** List of deleted resources. */
        val deletedResources = mutableListOf<String>()

        /**
         * Save resource.
         * @param resourceType The resource type.
         * @param resourceId The resource id.
         * @param resource The resource itself.
         * @param isLocalChange If it's a local change.
         * @return Result enclosing success.
         */
        override suspend fun saveResource(
            resourceType: String,
            resourceId: String,
            resource: dev.ohs.fhir.model.r4.Resource,
            isLocalChange: Boolean,
        ): Result<Unit> {
            savedResources[resourceId] = resource
            return Result.success(Unit)
        }

        /**
         * Delete resource.
         * @param resourceType The resource type.
         * @param resourceId The resource id.
         * @param isLocalChange If it's a local change.
         * @return Result enclosing success.
         */
        override suspend fun deleteResource(
            resourceType: String,
            resourceId: String,
            isLocalChange: Boolean,
        ): Result<Unit> {
            deletedResources.add(resourceId)
            return Result.success(Unit)
        }
    }

    /**
     * Tests loading default forms.
     */
    @Test
    fun testLoadDefaultForms() =
        runTest {
            val fhirRepo = FakeFhirRepo()
            val db = fhirRepo.database
            val validQ =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "custom-from-db"
                    }.build()
            val validJson =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(
                        Questionnaire.serializer(),
                        validQ,
                    ).getOrThrow()
            db.chartCamQueries.insertResource("custom-from-db", "Questionnaire", validJson, "2026-09-18T10:00:00Z")
            db.chartCamQueries.insertResource("corrupt-json", "Questionnaire", "{corrupt", "2026-09-18T10:00:00Z")
            val nullIdQ = Questionnaire.Builder(Enumeration(value = PublicationStatus.Active)).build()
            val nullIdJson =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(
                        Questionnaire.serializer(),
                        nullIdQ,
                    ).getOrThrow()
            db.chartCamQueries.insertResource("no-id-q", "Questionnaire", nullIdJson, "2026-09-18T10:00:00Z")

            val qrRepo = QuestionnaireRepository(fhirRepo)

            qrRepo.loadDefaultForms()

            val q1 = qrRepo.getQuestionnaire("std-form")
            val q2 = qrRepo.getQuestionnaire("basic-followup")
            val qDb = qrRepo.getQuestionnaire("custom-from-db")

            // Default forms should be loaded
            assertNotNull(q1)
            assertNotNull(q2)
            assertNotNull(qDb)
        }

    /**
     * Tests creating a questionnaire with zero photos.
     */
    @Test
    fun testCreateQuestionnaireWithZeroPhotos() {
        val qrRepo = QuestionnaireRepository(null)
        val q = qrRepo.createQuestionnaire("No Photos Form", 0, "")
        assertEquals(1, q.item.size)
        assertEquals("notes", q.item[0].linkId.value)
    }

    /**
     * Tests loading default forms exceptions.
     */
    @Test
    fun testLoadDefaultFormsExceptions() =
        runTest {
            val nullRepo = QuestionnaireRepository(null)
            val initialForms = nullRepo.getAvailableQuestionnaires()
            nullRepo.loadDefaultForms()
            val forms = nullRepo.getAvailableQuestionnaires()
            assertNotNull(forms)
            assertTrue(forms.size >= initialForms.size)
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

    /**
     * Tests localization of standard forms in Spanish, Japanese, Hebrew, and Traditional Chinese.
     */
    @Test
    fun testLocalizeDefaultForms() =
        runTest {
            val qrRepo = QuestionnaireRepository(null)
            qrRepo.loadDefaultForms()

            // English (Default)
            val stdEn = qrRepo.getQuestionnaire("std-form", "en")
            assertNotNull(stdEn)
            assertEquals("Standard Clinical Photo", stdEn.title?.value)

            // Spanish
            val stdEs = qrRepo.getQuestionnaire("std-form", "es")
            assertNotNull(stdEs)
            assertEquals("Formulario Clínico Estándar", stdEs.title?.value)
            val frontEs = stdEs.item.find { it.linkId.value == "front" }
            assertEquals("Frente", frontEs?.text?.value)

            // Japanese
            val stdJa = qrRepo.getQuestionnaire("std-form", "ja")
            assertNotNull(stdJa)
            assertEquals("標準臨床問診票", stdJa.title?.value)
            val frontJa = stdJa.item.find { it.linkId.value == "front" }
            assertEquals("正面", frontJa?.text?.value)

            // Hebrew
            val stdHe = qrRepo.getQuestionnaire("std-form", "he")
            assertNotNull(stdHe)
            assertEquals("טופס קליני סטנדרטי", stdHe.title?.value)
            val frontHe = stdHe.item.find { it.linkId.value == "front" }
            assertEquals("חזית", frontHe?.text?.value)

            // Traditional Chinese
            val stdZh = qrRepo.getQuestionnaire("std-form", "zh")
            assertNotNull(stdZh)
            assertEquals("標準臨床問診表", stdZh.title?.value)

            // Basic followup localization
            val followEs = qrRepo.getQuestionnaire("basic-followup", "es")
            assertNotNull(followEs)
            assertEquals("Seguimiento Básico", followEs.title?.value)
            val consentEs = followEs.item.find { it.linkId.value == "patient_consent" }
            assertEquals("El paciente consintió las fotos", consentEs?.text?.value)

            // Available questionnaires with localization
            val availableEs = qrRepo.getAvailableQuestionnaires("es")
            assertTrue(availableEs.any { it.title?.value == "Formulario Clínico Estándar" })
        }

    /**
     * Tests localization edge cases: nested items, string options, unknown values, and null IDs.
     */
    @Test
    fun testLocalizationEdgeCases() =
        runTest {
            val qrRepo = QuestionnaireRepository(null)

            // Nested item with String answer option and Coding answer option
            val childItem =
                Questionnaire.Item
                    .Builder(
                        linkId =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "child-item" },
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                    ).apply {
                        text =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Child Question" }
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.String(
                                        dev.ohs.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = "Routine" }
                                            .build(),
                                    ),
                            ),
                        )
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.String(
                                        dev.ohs.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = "unknown-option" }
                                            .build(),
                                    ),
                            ),
                        )
                    }

            val parentItem =
                Questionnaire.Item
                    .Builder(
                        linkId =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "group-item" },
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                    ).apply {
                        item.add(childItem)
                    }

            val customQ =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "custom-nested-q"
                        item.add(parentItem)
                    }.build()

            qrRepo.saveQuestionnaire(customQ)
            val localizedCustom = qrRepo.getQuestionnaire("custom-nested-q", "es")
            assertNotNull(localizedCustom)
            assertEquals(1, localizedCustom.item.size)
            val group = localizedCustom.item[0]
            assertEquals(1, group.item.size)
            val localizedChild = group.item[0]
            val opt1 =
                localizedChild.answerOption[0]
                    .value
                    .asString()
                    ?.value
                    ?.value
            assertEquals("Rutina", opt1)
            val opt2 =
                localizedChild.answerOption[1]
                    .value
                    .asString()
                    ?.value
                    ?.value
            assertEquals("unknown-option", opt2)

            // Saving questionnaire with null ID
            val noIdQ = Questionnaire.Builder(Enumeration(value = PublicationStatus.Active)).build()
            val saveResult = qrRepo.saveQuestionnaire(noIdQ)
            assertTrue(saveResult.isFailure)

            // Questionnaire with no title, item with no text, coding options with no display
            val codingNoDisplayItem =
                Questionnaire.Item
                    .Builder(
                        linkId =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "unknown-link" },
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                    ).apply {
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.Coding(
                                        Coding
                                            .Builder()
                                            .apply {
                                                code = Code.Builder().apply { value = "routine" }
                                            }.build(),
                                    ),
                            ),
                        )
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.Coding(
                                        Coding.Builder().build(),
                                    ),
                            ),
                        )
                    }

            val qNoTitle =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "unknown-custom-form"
                        item.add(codingNoDisplayItem)
                    }.build()

            val localizedNoTitle = qrRepo.localizeQuestionnaire(qNoTitle, "es")
            assertNotNull(localizedNoTitle)
            assertNull(localizedNoTitle.title)

            // Form with ID std-form but unknown linkId to trigger line 405
            qrRepo.loadDefaultForms()
            val stdQ = qrRepo.getQuestionnaire("std-form")!!
            val stdWithUnknownLink = stdQ.toBuilder().apply { item.add(codingNoDisplayItem) }.build()
            val localizedStd = qrRepo.localizeQuestionnaire(stdWithUnknownLink, "es")
            assertNotNull(localizedStd)

            // Questionnaire with null ID and option with null string and integer value
            val nullStringOptionItem =
                Questionnaire.Item
                    .Builder(
                        linkId =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = null },
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                    ).apply {
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.String(
                                        dev.ohs.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = null }
                                            .build(),
                                    ),
                            ),
                        )
                        answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                value =
                                    Questionnaire.Item.AnswerOption.Value.Integer(
                                        dev.ohs.fhir.model.r4.Integer
                                            .Builder()
                                            .apply { value = 123 }
                                            .build(),
                                    ),
                            ),
                        )
                    }

            val qNullId =
                Questionnaire
                    .Builder(Enumeration(value = PublicationStatus.Active))
                    .apply {
                        title =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Form With Null ID" }
                        item.add(nullStringOptionItem)
                    }.build()

            val localizedNullId = qrRepo.localizeQuestionnaire(qNullId, "es")
            assertNotNull(localizedNullId)
            assertEquals("Form With Null ID", localizedNullId.title?.value)

            // Repeated loadDefaultForms to test already-loaded branch
            qrRepo.loadDefaultForms()
        }
}
