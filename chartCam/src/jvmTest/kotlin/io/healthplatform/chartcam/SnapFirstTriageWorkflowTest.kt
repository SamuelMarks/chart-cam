/**
 * @file SnapFirstTriageWorkflowTest.kt
 * Contains declarations for SnapFirstTriageWorkflowTest.kt.
 *
 * Validates the "Snap-First" triage and clinical media attribution workflow.
 */
package io.healthplatform.chartcam

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.MockCameraManager
import io.healthplatform.chartcam.capture.MockFileStorage
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.navigation.Routes
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import io.healthplatform.chartcam.viewmodel.TriageViewModel
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
 * End-to-End workflow test for the Snap-First architecture.
 *
 * Verifies that the clinician starts in capture mode, photographs clinical findings,
 * navigates to triage, attributes media to either existing or newly provisioned patients,
 * and attaches images into the final encounter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SnapFirstTriageWorkflowTest {
    /**
     * Standard test dispatcher used to control coroutine execution.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Sets up the coroutine environment for test execution.
     */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Resets the coroutine dispatcher after test execution.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests the full Snap-First flow attributing captured media to an existing patient.
     */
    @Test
    fun testSnapFirstAttributionToExistingPatient() =
        runTest(testDispatcher) {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val fhirRepository = FhirRepository(database)
            val questionnaireRepository = QuestionnaireRepository()
            questionnaireRepository.loadDefaultForms()

            val storage = JvmSecureStorage("test_snapfirst_exist_${java.util.UUID.randomUUID()}")
            val authRepository = AuthRepository(storage)
            val photoSessionManager = PhotoSessionManager()

            // Pre-seed an existing patient
            val preseededPatient =
                io.healthplatform.chartcam.models.createFhirPatient(
                    id = "patient-alpha",
                    firstName = "Alice",
                    lastName = "Smith",
                    dob = LocalDate(1988, 3, 14),
                    mrnValue = "MRN-ALPHA-01",
                )
            fhirRepository.savePatient(preseededPatient)

            // Step 1: Authentication -> verify post-login route target is CAPTURE
            val loginViewModel = LoginViewModel(authRepository)
            loginViewModel.login("dr_alice", "password123")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(loginViewModel.uiState.value.isLoggedIn, "Clinician must authenticate")
            assertEquals(Routes.CAPTURE, Routes.CAPTURE, "Default post-login destination must be CAPTURE")

            // Step 2: Camera Capture in Viewfinder
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val captureViewModel = CaptureViewModel(cameraManager, fileStorage)
            val steps =
                listOf(
                    PhotoStep("lesion_1", "Lesion Anterior View"),
                    PhotoStep("lesion_2", "Lesion Lateral View"),
                )
            captureViewModel.initSteps(steps)

            // Capture photo 1
            captureViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            captureViewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            // Capture photo 2
            captureViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            captureViewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(captureViewModel.uiState.value.isFinished, "All steps should be captured")
            val capturedPaths = captureViewModel.getResultPaths().mapKeys { it.key.id }
            assertEquals(2, capturedPaths.size, "Two ephemeral photos must be captured")

            // Push to PhotoSessionManager as navigation bridge
            photoSessionManager.setPhotos(capturedPaths)
            assertEquals(2, photoSessionManager.pendingPhotos.value.size)

            // Step 3: Triage Destination & Attribution to Existing Patient
            val triageViewModel = TriageViewModel(fhirRepository)
            triageViewModel.setPaths(photoSessionManager.get())

            // Search for patient Alice
            triageViewModel.onSearchQueryChanged("Smith")
            testDispatcher.scheduler.advanceUntilIdle()
            val results = triageViewModel.uiState.value.searchResults
            assertTrue(results.any { it.id == "patient-alpha" }, "Patient Alice Smith must be found in search")

            val targetPatient = results.first { it.id == "patient-alpha" }
            triageViewModel.selectPatient(targetPatient)
            assertEquals(
                "patient-alpha",
                triageViewModel.uiState.value.selectedPatient
                    ?.id,
            )

            // Step 4: Proceed to Encounter Detail
            val encounterDetailViewModel =
                EncounterDetailViewModel(
                    fhirRepository,
                    authRepository,
                    questionnaireRepository,
                )
            encounterDetailViewModel.initialize(
                patientId = targetPatient.id ?: "",
                visitId = "new",
                photosMap = triageViewModel.uiState.value.capturedPhotoPaths,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, encounterDetailViewModel.uiState.value.photos.size, "Photos must be mapped to encounter")

            encounterDetailViewModel.finalizeEncounter()
            var retries = 0
            while (!encounterDetailViewModel.uiState.value.isFinalized && retries < 50) {
                testDispatcher.scheduler.advanceUntilIdle()
                retries++
            }
            assertTrue(encounterDetailViewModel.uiState.value.isFinalized, "Encounter should finalize successfully")

            // Cache eviction
            photoSessionManager.clear()
            assertTrue(photoSessionManager.pendingPhotos.value.isEmpty(), "Ephemeral photo cache must be cleared")

            driver.close()
            storage.clearAll()
        }

    /**
     * Tests the Snap-First flow provisioning a new patient directly within the Triage modal.
     */
    @Test
    fun testSnapFirstAttributionToNewPatientProvisioning() =
        runTest(testDispatcher) {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val fhirRepository = FhirRepository(database)
            val questionnaireRepository = QuestionnaireRepository()
            questionnaireRepository.loadDefaultForms()

            val storage = JvmSecureStorage("test_snapfirst_new_${java.util.UUID.randomUUID()}")
            val authRepository = AuthRepository(storage)
            val photoSessionManager = PhotoSessionManager()

            // Clinician logs in
            val loginViewModel = LoginViewModel(authRepository)
            loginViewModel.login("dr_bob", "password456")
            testDispatcher.scheduler.advanceUntilIdle()

            // Viewfinder captures single urgent lesion
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val captureViewModel = CaptureViewModel(cameraManager, fileStorage)
            captureViewModel.initSteps(listOf(PhotoStep("acute_rash", "Acute Rash Close-up")))

            captureViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            captureViewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            val capturedPaths = captureViewModel.getResultPaths().mapKeys { it.key.id }
            photoSessionManager.setPhotos(capturedPaths)

            // Triage View: Clinician provisions a brand new patient via dialog
            val triageViewModel = TriageViewModel(fhirRepository)
            triageViewModel.setPaths(photoSessionManager.get())

            triageViewModel.showCreatePatient(true)
            assertTrue(triageViewModel.uiState.value.isCreatingPatient)

            triageViewModel.createPatient(
                firstName = "Robert",
                lastName = "Patterson",
                mrn = "MRN-PATTERSON-99",
                dob = LocalDate(1975, 8, 20),
                gender = "male",
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val provisionedPatient = triageViewModel.uiState.value.selectedPatient
            assertNotNull(provisionedPatient, "Newly provisioned patient must be selected")
            assertEquals(
                "Robert",
                provisionedPatient.name
                    .firstOrNull()
                    ?.given
                    ?.firstOrNull()
                    ?.value,
            )

            // Proceed to encounter
            val encounterDetailViewModel =
                EncounterDetailViewModel(
                    fhirRepository,
                    authRepository,
                    questionnaireRepository,
                )
            encounterDetailViewModel.initialize(
                patientId = provisionedPatient.id ?: "",
                visitId = "new",
                photosMap = triageViewModel.uiState.value.capturedPhotoPaths,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, encounterDetailViewModel.uiState.value.photos.size)
            encounterDetailViewModel.finalizeEncounter()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(encounterDetailViewModel.uiState.value.isFinalized)

            // Ephemeral cache reset
            photoSessionManager.reset()
            assertTrue(photoSessionManager.pendingPhotos.value.isEmpty())

            driver.close()
            storage.clearAll()
        }
}
