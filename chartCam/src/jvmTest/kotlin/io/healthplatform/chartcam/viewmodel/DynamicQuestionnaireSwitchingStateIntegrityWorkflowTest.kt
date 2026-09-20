/**
 * @file DynamicQuestionnaireSwitchingStateIntegrityWorkflowTest.kt
 * Contains declarations for DynamicQuestionnaireSwitchingStateIntegrityWorkflowTest.kt.
 *
 * Validates state preservation and type compatibility sanitization when switching
 * clinical questionnaire forms mid-encounter before finalization.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.validation.FhirValidator
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests verifying questionnaire switching safety and type incompatibility pruning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DynamicQuestionnaireSwitchingStateIntegrityWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var questionnaireRepo: QuestionnaireRepository

    /**
     * Initializes test environment before each test.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_dyn_${java.util.UUID.randomUUID()}")
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
     * Builds Ophthalmology Intake Questionnaire (Form A).
     *
     * @return Built [Questionnaire].
     */
    private fun buildFormA(): Questionnaire {
        val findings =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "findings" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = FhirString.Builder().apply { value = "Describe corneal observations" }
                }

        val severity =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "severity" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                ).apply {
                    text = FhirString.Builder().apply { value = "Severity" }
                    answerOption.add(
                        Questionnaire.Item.AnswerOption.Builder(
                            value =
                                Questionnaire.Item.AnswerOption.Value.String(
                                    FhirString.Builder().apply { value = "Moderate" }.build(),
                                ),
                        ),
                    )
                }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                id = "form-ophthalmology"
                title = FhirString.Builder().apply { value = "Ophthalmology Intake" }
                item.add(findings)
                item.add(severity)
            }.build()
    }

    /**
     * Builds Dermatology Triage Questionnaire (Form B) with Integer severity.
     *
     * @return Built [Questionnaire].
     */
    private fun buildFormB(): Questionnaire {
        val findings =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "findings" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Text),
                ).apply {
                    text = FhirString.Builder().apply { value = "Detailed lesion description" }
                }

        val severity =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "severity" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                ).apply {
                    text = FhirString.Builder().apply { value = "Numeric severity score (1-10)" }
                }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                id = "form-dermatology"
                title = FhirString.Builder().apply { value = "Dermatology Triage" }
                item.add(findings)
                item.add(severity)
            }.build()
    }

    /**
     * Verifies switching forms mid-encounter carries compatible fields while sanitizing incompatible types.
     */
    @Test
    fun testSwitchingQuestionnairesPrunesIncompatibleTypes() =
        runTest(testDispatcher) {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "p-switch"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Johnson" }
                                given.add(FhirString.Builder().apply { value = "Mark" })
                            },
                        )
                        gender = Enumeration(value = AdministrativeGender.Male)
                    }.build()
            fhirRepo.savePatient(patient)

            val practitioner = createFhirPractitioner("doc-switch", "Adams", "Sam", true)
            fhirRepo.savePractitioner(practitioner)
            authRepo.login("doc-switch", "pass")
            testDispatcher.scheduler.advanceUntilIdle()

            val formA = buildFormA()
            val formB = buildFormB()
            fhirRepo.saveQuestionnaire(formA)
            fhirRepo.saveQuestionnaire(formB)

            val vm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo)
            vm.initialize(patientId = "p-switch", visitId = "new", photosMap = emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()

            // Select Form A and enter values
            vm.selectQuestionnaire(formA)
            vm.onAnswerChanged("findings", "Bilateral conjunctival injection")
            vm.onAnswerChanged("severity", "Moderate") // String value for Choice

            assertEquals("Moderate", vm.uiState.value.answers["severity"])

            // Switch to Form B (where severity is Integer)
            vm.selectQuestionnaire(formB)

            // Assert string-compatible 'findings' is preserved
            assertEquals("Bilateral conjunctival injection", vm.uiState.value.answers["findings"])

            // Assert incompatible string 'Moderate' was pruned from Integer 'severity'
            assertNull(vm.uiState.value.answers["severity"], "String answer must be pruned for Integer field")

            // Enter a valid integer for severity in Form B
            vm.onAnswerChanged("severity", 7)
            assertEquals(7, vm.uiState.value.answers["severity"])

            // Finalize encounter
            vm.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.uiState.value.isFinalized)

            val encId =
                vm.uiState.value.encounter
                    ?.id ?: ""
            val responses = fhirRepo.getQuestionnaireResponsesForEncounter(encId)
            assertEquals(1, responses.size)
            val qr = responses.first()

            // Validate against FhirValidator
            assertTrue(FhirValidator.validate(qr).isSuccess)
        }
}
