/**
 * @file EncounterLifecycleAndImmutabilityTest.kt
 * Contains declarations for EncounterLifecycleAndImmutabilityTest.kt.
 *
 * Validates clinical encounter lifecycle, status transitions, finalization guardrails, and immutability rules.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.QuestionnaireResponse
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite verifying that clinical encounters progress predictably through draft
 * and in-progress states, transition to immutable Finished states upon finalization,
 * and enforce strict editing guardrails.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EncounterLifecycleAndImmutabilityTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Prepares test dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up test dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests draft encounter editing, followed by finalization, immutability assertion,
     * and explicit clinician reopen workflows.
     */
    @Test
    fun testEncounterDraftFinalizationAndImmutabilityLifecycle() =
        runTest(testDispatcher) {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val fhirRepo = FhirRepository(database)
            val questionnaireRepo = QuestionnaireRepository()
            questionnaireRepo.loadDefaultForms()
            val storage = JvmSecureStorage("encounter_life_${java.util.UUID.randomUUID()}")
            val authRepo = AuthRepository(storage)

            authRepo.login("dr_auditor", "audit_pass")
            testDispatcher.scheduler.advanceUntilIdle()

            // 1. Provision patient
            val patient =
                createFhirPatient(
                    id = "pat-lifecycle-01",
                    firstName = "Frank",
                    lastName = "Sinatra",
                    dob = LocalDate(1965, 12, 12),
                    mrnValue = "MRN-FRANK-01",
                )
            fhirRepo.savePatient(patient)

            // 2. Initialize new draft encounter
            val viewModel =
                EncounterDetailViewModel(
                    fhirRepo,
                    authRepo,
                    questionnaireRepo,
                )
            viewModel.initialize(
                patientId = patient.id ?: "",
                visitId = "new",
                photosMap = emptyMap(),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val createdEncounter = viewModel.uiState.value.encounter
            assertNotNull(createdEncounter)
            val visitId = createdEncounter.id ?: ""
            assertFalse(viewModel.uiState.value.isFinalized, "New encounter must not be finalized initially")

            // 3. Draft edits: record clinical notes and answers
            viewModel.onNotesChanged("Suspected basal cell carcinoma on left cheek.")
            viewModel.onAnswerChanged("notes", "Suspected basal cell carcinoma on left cheek.")
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify encounter status in DB remains in-progress
            val inProgressEnc = fhirRepo.getEncounter(visitId)
            assertNotNull(inProgressEnc)
            assertEquals(Encounter.EncounterStatus.In_Progress, inProgressEnc.status.value)

            // 4. Clinician finalizes encounter
            viewModel.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isFinalized, "ViewModel must reflect finalized state")

            // Verify persistence in DB has transitioned status to Finished
            val finalizedEnc = fhirRepo.getEncounter(visitId)
            assertNotNull(finalizedEnc)
            assertEquals(Encounter.EncounterStatus.Finished, finalizedEnc.status.value)

            // Verify QuestionnaireResponse status is Completed
            val responses = fhirRepo.getQuestionnaireResponsesForEncounter(visitId)
            assertTrue(responses.isNotEmpty(), "Finalized encounter must have generated QuestionnaireResponses")
            assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.Completed, responses.first().status.value)

            // 5. Verify reloading existing finalized encounter maintains read-only finalized state
            val reloadedViewModel =
                EncounterDetailViewModel(
                    fhirRepo,
                    authRepo,
                    questionnaireRepo,
                )
            reloadedViewModel.initialize(
                patientId = patient.id ?: "",
                visitId = visitId,
                photosMap = emptyMap(),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Finished encounter in DB
            val loadedEnc = reloadedViewModel.uiState.value.encounter
            assertNotNull(loadedEnc)
            assertEquals(Encounter.EncounterStatus.Finished, loadedEnc.status.value)

            // 6. Clinician explicitly unlocks/reopens encounter
            reloadedViewModel.reopenEncounter()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(reloadedViewModel.uiState.value.isFinalized, "Encounter should no longer be locked")
            val reopenedEnc = fhirRepo.getEncounter(visitId)
            assertNotNull(reopenedEnc)
            assertEquals(Encounter.EncounterStatus.In_Progress, reopenedEnc.status.value)

            driver.close()
            storage.clearAll()
        }
}
