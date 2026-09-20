/**
 * @file SdcCalculatedExpressionDivisionByZeroWorkflowTest.kt
 * Contains declarations for SdcCalculatedExpressionDivisionByZeroWorkflowTest.kt.
 *
 * Validates resilience of the SDC calculated expressions engine against division by zero,
 * unpopulated numeric denominators, malformed syntax strings, and BigDecimal conversion safety.
 */
package io.healthplatform.chartcam.sdc

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.fhir.QuestionnaireResponseGenerator
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Workflow tests validating division-by-zero resilience and safe decimal conversions in clinical forms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SdcCalculatedExpressionDivisionByZeroWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var questionnaireRepo: QuestionnaireRepository

    /**
     * Initializes test environment and database.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_sdc_div0_${java.util.UUID.randomUUID()}")
        authRepo = AuthRepository(storage)
        questionnaireRepo = QuestionnaireRepository()
    }

    /**
     * Tears down test environment.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Constructs a test questionnaire containing formula items.
     *
     * @return Fully structured [Questionnaire].
     */
    private fun createFormulaQuestionnaire(): Questionnaire {
        val bmiExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value =
                            Extension.Value.String(
                                FhirString.Builder().apply { value = "%weight / (%height * %height)" }.build(),
                            )
                    },
                )
            }

        val mapExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value =
                            Extension.Value.String(
                                FhirString.Builder().apply { value = "(%systolic + 2 * %diastolic) / 3" }.build(),
                            )
                    },
                )
            }

        val weightItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "weight" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Weight (kg)" }
                }

        val heightItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "height" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Height (m)" }
                }

        val bmiItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "bmi" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Body Mass Index" }
                    extension.add(bmiExt)
                }

        val mapItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "map" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Mean Arterial Pressure" }
                    extension.add(mapExt)
                }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                id = "calc-zero-test"
                title = FhirString.Builder().apply { value = "Vitals Calculation" }
                item.add(weightItem)
                item.add(heightItem)
                item.add(bmiItem)
                item.add(mapItem)
            }.build()
    }

    /**
     * Verifies that unentered denominator does not evaluate to Infinity or NaN.
     */
    @Test
    fun testZeroDenominatorEvaluatesGracefully() {
        val q = createFormulaQuestionnaire()
        val answers = mapOf("weight" to 70.0)

        val updatedAnswers = SdcEvaluator.evaluateCalculatedExpressions(q, answers)

        val bmi = updatedAnswers["bmi"]
        if (bmi != null) {
            val fl = (bmi as? Number)?.toFloat() ?: 0f
            assertTrue(!fl.isInfinite() && !fl.isNaN(), "Evaluated BMI must not be Infinity or NaN")
        }
    }

    /**
     * Verifies that encounter finalization succeeds without crashing when calculations contain zero denominators.
     */
    @Test
    fun testEncounterFinalizationWithZeroDenominatorSucceeds() =
        runTest(testDispatcher) {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat-sdc"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Doe" }
                                given.add(FhirString.Builder().apply { value = "Jane" })
                            },
                        )
                        gender = Enumeration(value = AdministrativeGender.Female)
                    }.build()
            fhirRepo.savePatient(patient)

            val practitioner = createFhirPractitioner("doc-sdc", "Smith", "John", true)
            fhirRepo.savePractitioner(practitioner)
            authRepo.login("doc-sdc", "pass")
            testDispatcher.scheduler.advanceUntilIdle()

            val q = createFormulaQuestionnaire()
            fhirRepo.saveQuestionnaire(q)

            val vm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo)
            vm.initialize(patientId = "pat-sdc", visitId = "new", photosMap = emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()

            vm.selectQuestionnaire(q)
            vm.onAnswerChanged("weight", 70.0)
            // height is deliberately left empty

            // Finalize encounter: must not throw ArithmeticException or NumberFormatException
            vm.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.uiState.value.isFinalized, "Encounter should finalize smoothly")
            assertNotNull(vm.uiState.value.encounter)

            // Direct check on QuestionnaireResponseGenerator with Infinity
            val rawAnswersWithInfinity = mapOf<String, Any>("weight" to 70.0, "bmi" to Float.POSITIVE_INFINITY)
            val qr = QuestionnaireResponseGenerator.generate(q, rawAnswersWithInfinity)
            assertNotNull(qr)
        }

    /**
     * Verifies that malformed math expressions are caught without uncaught exceptions.
     */
    @Test
    fun testMalformedArithmeticExpressionsReturnNull() {
        val answers = mapOf<String, Any?>("weight" to 70f, "height" to 1.75f)

        assertNull(SdcEvaluator.evaluateExpression("%weight / ", answers))
        assertNull(SdcEvaluator.evaluateExpression("%weight + * %height", answers))
        assertNull(SdcEvaluator.evaluateExpression("((%weight + 2)", answers))
        assertEquals(70f, SdcEvaluator.evaluateExpression("%weight", answers))
    }
}
