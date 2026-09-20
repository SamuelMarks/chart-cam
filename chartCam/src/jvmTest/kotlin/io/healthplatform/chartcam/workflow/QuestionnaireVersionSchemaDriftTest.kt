/**
 * @file QuestionnaireVersionSchemaDriftTest.kt
 * End-to-end workflow test verifying backward compatibility of completed questionnaires under schema updates.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Reference
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.FhirMocks
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Validates resilience of historical QuestionnaireResponse records when form schemas are updated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestionnaireVersionSchemaDriftTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: FhirRepository
    private lateinit var questionnaireRepo: QuestionnaireRepository

    /**
     * Initializes test environment.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        repo = FhirRepository(db)
        questionnaireRepo = QuestionnaireRepository()
    }

    /**
     * Cleans up driver.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Verifies that updating a questionnaire schema does not corrupt historical questionnaire responses.
     */
    @Test
    fun testHistoricalResponsesRetainedAfterFormSchemaUpdate() =
        runTest(testDispatcher) {
            questionnaireRepo.loadDefaultForms()

            val patient = createFhirPatient("p-drift", "Jane", "Doe", LocalDate(1992, 6, 12), "MRN-DRIFT")
            repo.savePatient(patient)

            val enc = createFhirEncounter("enc-drift", "p-drift", "dr-who", "2026-09-14T10:00:00Z")
            repo.saveEncounter(enc)

            // Form v1
            val formV1 = questionnaireRepo.createQuestionnaire("PreOp Assessment", 1, "V1 Label")
            repo.saveQuestionnaire(formV1)

            // Historical response against v1
            val baseQr =
                FhirMocks.createMockQuestionnaireResponse(
                    idStr = "qr-drift-1",
                    questionnaireUrl = formV1.id ?: "",
                )
            val qr =
                baseQr
                    .toBuilder()
                    .apply {
                        encounter =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Encounter/enc-drift" }
                            }
                        subject =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Patient/p-drift" }
                            }
                    }.build()
            repo.saveQuestionnaireResponse(qr)

            // Now update form schema to v2 with extra fields
            val formV2 = questionnaireRepo.createQuestionnaire("PreOp Assessment Updated", 2, "V2 Label")
            val rekeyedV2 =
                formV2
                    .toBuilder()
                    .apply {
                        id = formV1.id
                    }.build()
            repo.saveQuestionnaire(rekeyedV2)

            // Query historical response
            val responses = repo.getQuestionnaireResponsesForEncounter("enc-drift")
            assertEquals(1, responses.size)
            val historicalQr = responses[0]
            assertNotNull(historicalQr)
            assertEquals("qr-drift-1", historicalQr.id)
            assertEquals(
                "Mock Question",
                historicalQr.item
                    .firstOrNull()
                    ?.text
                    ?.value,
            )
        }
}
