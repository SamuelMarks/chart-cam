/**
 * @file SessionCrashRecoveryWorkflowTest.kt
 * Contains declarations for SessionCrashRecoveryWorkflowTest.kt.
 *
 * Validates ephemeral photo session preservation and crash recovery workflows across process interruptions.
 */
package io.healthplatform.chartcam.capture

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.navigation.PhotoSessionManager
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-End workflow tests verifying crash resiliency and orphan photo recovery
 * during interrupted multi-step photo acquisition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionCrashRecoveryWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Sets up test coroutine dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Resets test coroutine dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests crash recovery during multi-step photo acquisition, verifying that
     * captured photos are preserved and capture resumes at the correct step index.
     */
    @Test
    fun testMultiStepCaptureCrashRecoveryFlow() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val sessionManager = PhotoSessionManager()

            val step1 = PhotoStep("step_1", "Anterior Lesion")
            val step2 = PhotoStep("step_2", "Lateral Lesion")
            val step3 = PhotoStep("step_3", "Dermoscopic Close-up")
            val allSteps = listOf(step1, step2, step3)

            // Phase 1: First app session - capture step 1 and step 2
            var initialViewModel: CaptureViewModel? = CaptureViewModel(cameraManager, fileStorage)
            initialViewModel?.initSteps(allSteps)
            testDispatcher.scheduler.advanceUntilIdle()

            // Capture Step 1
            initialViewModel?.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            initialViewModel?.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            // Capture Step 2
            initialViewModel?.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            initialViewModel?.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            val initialResults = initialViewModel?.getResultPaths() ?: emptyMap()
            assertEquals(2, initialResults.size, "Two photos must be captured before crash")

            // Simulate persistent session bridge caching ephemeral paths
            val preservedMap = HashMap(initialResults)
            sessionManager.setPhotos(initialResults.mapKeys { it.key.id })
            assertEquals(2, sessionManager.pendingPhotos.value.size)

            // Phase 2: Simulate abrupt crash / process teardown
            initialViewModel = null

            // Phase 3: Cold restart - instantiate fresh ViewModel
            val restoredViewModel = CaptureViewModel(cameraManager, fileStorage)
            restoredViewModel.initSteps(allSteps)
            restoredViewModel.restoreSession(preservedMap)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify state restored
            assertEquals(2, restoredViewModel.uiState.value.capturedCount, "Must restore 2 captured photos")
            assertEquals(
                "step_3",
                restoredViewModel.uiState.value.currentStep
                    ?.id,
                "Viewfinder must resume at step 3",
            )

            // Capture final step (Step 3)
            restoredViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            restoredViewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(restoredViewModel.uiState.value.isFinished, "Capture session must be fully completed")
            val finalResults = restoredViewModel.getResultPaths()
            assertEquals(3, finalResults.size, "All 3 photos must be accounted for")

            // Phase 4: Commit to clinical encounter in database
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val fhirRepo = FhirRepository(database)
            val questionnaireRepo = QuestionnaireRepository()
            questionnaireRepo.loadDefaultForms()
            val storage = JvmSecureStorage("crash_recov_${java.util.UUID.randomUUID()}")
            val authRepo = AuthRepository(storage)
            authRepo.login("dr_recovery", "pass123")
            testDispatcher.scheduler.advanceUntilIdle()

            val patient =
                createFhirPatient(
                    id = "pat-recovery-01",
                    firstName = "Crash",
                    lastName = "Survivor",
                    dob = LocalDate(1995, 5, 5),
                    mrnValue = "MRN-RECOV-01",
                )
            fhirRepo.savePatient(patient)

            val encounterDetailViewModel =
                EncounterDetailViewModel(
                    fhirRepo,
                    authRepo,
                    questionnaireRepo,
                )
            encounterDetailViewModel.initialize(
                patientId = patient.id ?: "",
                visitId = "new",
                photosMap = finalResults.mapKeys { it.key.id },
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(3, encounterDetailViewModel.uiState.value.photos.size, "Encounter should have all 3 photos")
            encounterDetailViewModel.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(encounterDetailViewModel.uiState.value.isFinalized)

            // Verify encounter saved in DB
            val savedEncounters = fhirRepo.getEncountersForPatient("pat-recovery-01")
            assertEquals(1, savedEncounters.size)
            assertNotNull(savedEncounters.first().id)

            sessionManager.clear()
            driver.close()
            storage.clearAll()
        }
}
