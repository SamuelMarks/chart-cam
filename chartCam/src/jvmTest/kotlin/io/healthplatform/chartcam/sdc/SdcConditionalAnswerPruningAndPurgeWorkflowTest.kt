/**
 * @file SdcConditionalAnswerPruningAndPurgeWorkflowTest.kt
 * Contains declarations for SdcConditionalAnswerPruningAndPurgeWorkflowTest.kt.
 *
 * Validates that dynamically hidden or disabled questionnaire items have their answers
 * purged from the final QuestionnaireResponse to protect medical record integrity.
 */
package io.healthplatform.chartcam.sdc

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.AdministrativeGender
import com.google.fhir.model.r4.terminologies.PublicationStatus
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.google.fhir.model.r4.Boolean as FhirBoolean
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests ensuring disabled or hidden branching questions do not emit answers into medical records.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SdcConditionalAnswerPruningAndPurgeWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var questionnaireRepo: QuestionnaireRepository

    /**
     * Initializes test environment.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_sdc_prune_${java.util.UUID.randomUUID()}")
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
     * Constructs dynamic allergy questionnaire with conditional enableWhen rules.
     *
     * @return Built [Questionnaire].
     */
    private fun buildAllergyQuestionnaire(): Questionnaire {
        val q1 =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "has_allergy" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                ).apply {
                    text = FhirString.Builder().apply { value = "Does patient have drug allergies?" }
                }

        val enableWhenCondition =
            Questionnaire.Item.EnableWhen
                .Builder(
                    answer =
                        Questionnaire.Item.EnableWhen.Answer.Boolean(
                            FhirBoolean.Builder().apply { value = true }.build(),
                        ),
                    operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                    question = FhirString.Builder().apply { value = "has_allergy" },
                )

        val q2 =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "allergy_details" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = FhirString.Builder().apply { value = "Specify drug allergies and adverse reaction severity" }
                    enableWhen.add(enableWhenCondition)
                }

        val q3 =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "allergy_photo" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    text = FhirString.Builder().apply { value = "Upload photo of allergic rash" }
                    enableWhen.add(enableWhenCondition)
                }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                id = "allergy-branching-q"
                title = FhirString.Builder().apply { value = "Allergy Intake" }
                item.add(q1)
                item.add(q2)
                item.add(q3)
            }.build()
    }

    /**
     * Verifies that unchecking a parent toggle prunes subsequent dependent answers upon finalization.
     */
    @Test
    fun testPruningDisabledBranchAnswersOnEncounterFinalization() =
        runTest(testDispatcher) {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat-branch"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Smith" }
                                given.add(FhirString.Builder().apply { value = "Bob" })
                            },
                        )
                        gender = Enumeration(value = AdministrativeGender.Male)
                    }.build()
            fhirRepo.savePatient(patient)

            val practitioner = createFhirPractitioner("doc-branch", "Jones", "Alice", true)
            fhirRepo.savePractitioner(practitioner)
            authRepo.login("doc-branch", "pass")
            testDispatcher.scheduler.advanceUntilIdle()

            val q = buildAllergyQuestionnaire()
            fhirRepo.saveQuestionnaire(q)

            val vm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo)
            vm.initialize(patientId = "pat-branch", visitId = "new", photosMap = emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()

            vm.selectQuestionnaire(q)

            // Step 1: User toggles allergy to true, inputs details
            vm.onAnswerChanged("has_allergy", true)
            vm.onAnswerChanged("allergy_details", "Anaphylactic reaction to Amoxicillin")
            vm.onAnswerChanged("allergy_photo", "rash_amoxicillin.jpg")

            // Both fields are evaluated as enabled
            assertTrue(SdcEvaluator.isItemEnabled(q.item[1], vm.uiState.value.answers))
            assertTrue(SdcEvaluator.isItemEnabled(q.item[2], vm.uiState.value.answers))

            // Step 2: User corrects/unchecks parent toggle to false
            vm.onAnswerChanged("has_allergy", false)

            // Now child items are evaluated as disabled
            assertFalse(SdcEvaluator.isItemEnabled(q.item[1], vm.uiState.value.answers))
            assertFalse(SdcEvaluator.isItemEnabled(q.item[2], vm.uiState.value.answers))

            // Step 3: Finalize Encounter
            vm.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.uiState.value.isFinalized)

            // Step 4: Verify serialized QuestionnaireResponse
            val encId =
                vm.uiState.value.encounter
                    ?.id ?: ""
            val responses = fhirRepo.getQuestionnaireResponsesForEncounter(encId)
            assertEquals(1, responses.size)
            val qr = responses.first()

            val includedLinkIds = qr.item.mapNotNull { it.linkId.value }
            assertTrue(includedLinkIds.contains("has_allergy"))
            assertFalse(includedLinkIds.contains("allergy_details"), "allergy_details should be pruned")
            assertFalse(includedLinkIds.contains("allergy_photo"), "allergy_photo should be pruned")
        }

    /**
     * Verifies recursive hierarchy pruning for nested question groups.
     */
    @Test
    fun testNestedHierarchicalGroupPruning() {
        val childItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "sub_question" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = FhirString.Builder().apply { value = "Sub Question" }
                }

        val parentGroup =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "parent_group" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    text = FhirString.Builder().apply { value = "Parent Group" }
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder(
                                answer =
                                    Questionnaire.Item.EnableWhen.Answer.Boolean(
                                        FhirBoolean.Builder().apply { value = true }.build(),
                                    ),
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                question = FhirString.Builder().apply { value = "root_toggle" },
                            ),
                    )
                    item.add(childItem)
                }

        val rootItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "root_toggle" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                ).apply {
                    text = FhirString.Builder().apply { value = "Show Advanced" }
                }

        val q =
            Questionnaire
                .Builder(Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "nested-group-q"
                    item.add(rootItem)
                    item.add(parentGroup)
                }.build()

        // Root toggle is false, but answers map contains stale child answer
        val answers = mapOf<String, Any>("root_toggle" to false, "sub_question" to "Secret answer")
        val qr = QuestionnaireResponseGenerator.generate(q, answers)

        val itemLinkIds = qr.item.mapNotNull { it.linkId.value }
        assertEquals(listOf("root_toggle"), itemLinkIds)
        assertTrue(qr.item.none { it.linkId.value == "parent_group" })
    }
}
