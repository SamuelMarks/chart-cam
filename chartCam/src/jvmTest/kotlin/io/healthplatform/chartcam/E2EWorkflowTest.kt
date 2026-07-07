/**
 * Contains End-to-End (E2E) workflow tests for the ChartCam application.
 */
package io.healthplatform.chartcam

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Encounter
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.storage.createSecureStorage
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import io.healthplatform.chartcam.viewmodel.PatientDetailViewModel
import io.healthplatform.chartcam.viewmodel.PatientListViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
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
 * Validates the full End-to-End (E2E) workflows across multiple ViewModels and Repositories.
 * Uses an in-memory SQLite database and mocked network requests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class E2EWorkflowTest {
    /**
     * A standard test dispatcher used to control coroutine execution during tests.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Sets up the testing environment, including the main coroutine dispatcher and clearing secure storage.
     */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        (createSecureStorage() as JvmSecureStorage).clearAll()
    }

    /**
     * Cleans up the testing environment by resetting the main coroutine dispatcher.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Executes a complete end-to-end workflow simulating a user session.
     * Includes login, patient creation, visit creation, note taking, photo capture simulation,
     * encounter finalization, and logout.
     */
    @Test
    fun testEndToEndWorkflow() =
        runTest(testDispatcher) {
            // 1. Setup in-memory DB and Repositories
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)

            val fhirRepository = FhirRepository(ChartCamDatabase(driver))

            val mockEngine =
                MockEngine { request ->
                    respondOk()
                }
            val client = NetworkClient.create(mockEngine)

            val storage = createSecureStorage()
            val authRepository = AuthRepository(client, storage)
            val fileStorage = createFileStorage()
            val exportImportService = ExportImportService(fhirRepository.database, fileStorage)
            val syncManager =
                SyncManager(
                    fhirRepository,
                    client,
                    io.healthplatform.chartcam.files
                        .createFileStorage(),
                )

            // 2. Login Workflow
            val loginViewModel = LoginViewModel(authRepository)
            loginViewModel.login("testuser", "password123")
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(loginViewModel.uiState.value.isLoggedIn, "User should be logged in")
            assertNotNull(authRepository.currentUser.value, "Current user should be populated")

            // 3. Create Patient Workflow
            val patientListViewModel = PatientListViewModel(fhirRepository, exportImportService, authRepository)
            patientListViewModel.setShowAllPatients(true)
            testDispatcher.scheduler.advanceUntilIdle() // Wait for initial load

            var newPatientId: String? = null
            patientListViewModel.createPatient(
                firstName = "John",
                lastName = "Doe",
                mrn = "MRN-12345",
                dob = LocalDate(1980, 1, 1),
                gender = "male",
            ) { id ->
                newPatientId = id
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(newPatientId, "Patient ID should not be null after creation")
            val patientList = patientListViewModel.uiState.value.patients
            assertTrue(patientList.any { it.id == newPatientId }, "Patient list should contain the new patient")

            // 4. Create Visit (Encounter)
            val patientDetailViewModel = PatientDetailViewModel(fhirRepository)
            patientDetailViewModel.loadPatientData(newPatientId!!)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                newPatientId,
                patientDetailViewModel.uiState.value.patient
                    ?.id,
            )

            // 5. Camera Capture & Note Taking
            val encounterDetailViewModel =
                EncounterDetailViewModel(
                    fhirRepository,
                    authRepository,
                    syncManager,
                    io.healthplatform.chartcam.repository
                        .QuestionnaireRepository(),
                )

            // Simulating the user passing photos to the encounter detail after capture
            val mockPhotosMap = mapOf("Left Eye" to "/path/to/left_eye.jpg", "Right Eye" to "/path/to/right_eye.jpg")

            encounterDetailViewModel.initialize(
                patientId = newPatientId!!,
                visitId = "new",
                photosMap = mockPhotosMap,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val currentEncounter = encounterDetailViewModel.uiState.value.encounter
            assertNotNull(currentEncounter, "Encounter should be created")
            assertEquals(2, encounterDetailViewModel.uiState.value.photos.size, "Should have 2 photos attached")

            // Note taking
            val testNotes = "Patient presents with dry eyes."
            encounterDetailViewModel.onNotesChanged(testNotes)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(testNotes, encounterDetailViewModel.uiState.value.answers["notes"])

            // Finalize Encounter
            encounterDetailViewModel.finalizeEncounter()
            var retries = 0
            while (!encounterDetailViewModel.uiState.value.isFinalized &&
                retries < 200
            ) {
                kotlinx.coroutines.delay(50)
                testDispatcher.scheduler.advanceUntilIdle()
                retries++
            }

            assertTrue(encounterDetailViewModel.uiState.value.isFinalized, "Encounter should be finalized")

            // 6. Return to list of visits (Patient Detail)
            patientDetailViewModel.loadPatientData(newPatientId!!)
            testDispatcher.scheduler.advanceUntilIdle()
            val encounters = patientDetailViewModel.uiState.value.encounters
            assertEquals(1, encounters.size, "Patient should have 1 encounter")
            val qrs = fhirRepository.getQuestionnaireResponsesForEncounter("Encounter/${encounters[0].id!!}")
            val notes =
                qrs
                    .firstOrNull()
                    ?.item
                    ?.find { it.linkId.value == "notes" }
                    ?.answer
                    ?.firstOrNull()
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value ?: ""
            assertEquals(testNotes, notes, "Encounter notes should match")
            assertEquals(Encounter.EncounterStatus.Finished, encounters[0].status?.value, "Encounter should be marked as finished")

            // 7. Return to list of patients
            patientListViewModel.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                patientListViewModel.uiState.value.patients
                    .isNotEmpty(),
                "Patient list should not be empty",
            )

            // 8. Test Questionnaire Management
            val questionnaireRepository = QuestionnaireRepository()
            val initialForms = questionnaireRepository.getAvailableQuestionnaires()
            assertTrue(initialForms.isNotEmpty(), "Should have preloaded standard forms")

            // Simulate user creating a new questionnaire form
            val newCustomForm =
                questionnaireRepository.createQuestionnaire(
                    title = "Diabetic Foot Exam",
                    photos = 3,
                    labels = "Sole, Heel, Toes",
                )

            assertNotNull(newCustomForm, "New form should be created successfully")
            assertEquals("Diabetic Foot Exam", newCustomForm.title?.value, "Title should match")

            // Verify items include notes and the 3 specified photos
            val formItems = newCustomForm.item
            assertEquals(4, formItems.size, "Should have 1 notes field + 3 photos")
            assertEquals("Clinical Notes", formItems[0].text?.value)
            assertEquals("Sole", formItems[1].text?.value)
            assertEquals("Heel", formItems[2].text?.value)
            assertEquals("Toes", formItems[3].text?.value)

            // Verify new form exists in repository
            val finalForms = questionnaireRepository.getAvailableQuestionnaires()
            assertEquals(initialForms.size + 1, finalForms.size, "Repository should contain the new form")

            // 9. Logout
            authRepository.logout()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(null, authRepository.currentUser.value, "User should be logged out")

            driver.close()
        }
}
