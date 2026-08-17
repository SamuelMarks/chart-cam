/**
 * @file TenPartFlowE2EJvmTest.kt
 * Contains declarations for TenPartFlowE2EJvmTest.kt.
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
import io.healthplatform.chartcam.repository.QuestionnaireSharingService
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.sync.SyncWorker
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import io.healthplatform.chartcam.viewmodel.PatientDetailViewModel
import io.healthplatform.chartcam.viewmodel.PatientListViewModel
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
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
 * End-to-End test that validates the specific 10-part flow requested by the user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TenPartFlowE2EJvmTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Sets up the coroutine dispatcher for the tests.
     */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Resets the main coroutine dispatcher after tests.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests the 10-part end-to-end flow from login to export and logout.
     */
    @Test
    fun testTenPartFlow() =
        runTest(testDispatcher) {
            // Setup in-memory DB and Repositories
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)

            val fhirRepository = FhirRepository(ChartCamDatabase(driver))
            val questionnaireRepository = QuestionnaireRepository()
            kotlinx.coroutines.runBlocking { questionnaireRepository.loadDefaultForms() }

            val mockEngine = MockEngine { respondOk() }
            val client = NetworkClient.create(mockEngine)
            val storage = JvmSecureStorage("test_e2e_10part_${java.util.UUID.randomUUID()}")
            val authRepository = AuthRepository(client, storage)
            val fileStorage = createFileStorage()
            val exportImportService = ExportImportService(fhirRepository.database, fileStorage)
            val questionnaireSharingService = QuestionnaireSharingService()
            val syncWorker = SyncWorker(fhirRepository, client)

            // Step 0: Login/signup
            val loginViewModel = LoginViewModel(authRepository)
            loginViewModel.login("testuser", "password")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(loginViewModel.uiState.value.isLoggedIn, "Should be logged in")

            // Step 1: Create patient
            val patientListViewModel = PatientListViewModel(fhirRepository, exportImportService, authRepository)
            patientListViewModel.setShowAllPatients(true)
            testDispatcher.scheduler.advanceUntilIdle()

            var patientId: String? = null
            patientListViewModel.createPatient(
                firstName = "Jane",
                lastName = "Doe",
                mrn = "MRN-TENPART",
                dob = LocalDate(1995, 5, 5),
            ) { id ->
                patientId = id
            }
            var retries = 0
            while (patientId == null && retries < 50) {
                java.lang.Thread.sleep(50)
                testDispatcher.scheduler.advanceUntilIdle()
                retries++
            }
            assertNotNull(patientId, "Patient ID should be created")

            // Step 2: List patients
            patientListViewModel.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()
            val allPatients = patientListViewModel.uiState.value.patients
            assertTrue(allPatients.any { it.id == patientId }, "Newly created patient should be listed")

            // Step 3: Create questionnaire with radio, select, free text, camera + label0, camera + label1
            val builderViewModel = QuestionnaireBuilderViewModel(questionnaireRepository)
            builderViewModel.updateTitle("Ten Part Form")

            // Radio/Select (Single Select)
            builderViewModel.addItem(WidgetType.SINGLE_SELECT)
            var currentItem =
                builderViewModel.state.value.items
                    .last()
            builderViewModel.updateItem(currentItem.linkId, "Radio Selection", listOf("Option 1", "Option 2"))

            // Free Text
            builderViewModel.addItem(WidgetType.MULTI_LINE_TEXT)
            currentItem =
                builderViewModel.state.value.items
                    .last()
            builderViewModel.updateItem(currentItem.linkId, "Free Text Input", emptyList())

            // Camera + label0
            builderViewModel.addItem(WidgetType.PHOTO_CAMERA)
            currentItem =
                builderViewModel.state.value.items
                    .last()
            builderViewModel.updateItem(currentItem.linkId, "Label0", emptyList())

            // Camera + label1
            builderViewModel.addItem(WidgetType.PHOTO_CAMERA)
            currentItem =
                builderViewModel.state.value.items
                    .last()
            builderViewModel.updateItem(currentItem.linkId, "Label1", emptyList())

            val customFormId = builderViewModel.saveQuestionnaire()
            assertNotNull(customFormId, "Questionnaire should be saved successfully")

            val customQuestionnaire = questionnaireRepository.getQuestionnaire(customFormId)
            assertNotNull(customQuestionnaire, "Should retrieve the custom questionnaire")
            assertEquals(4, customQuestionnaire.item.size, "Should have 4 items")

            // Step 4: Fill in questionnaire for a given patient (Create Encounter)
            val encounterDetailViewModel =
                EncounterDetailViewModel(
                    fhirRepository,
                    authRepository,
                    syncWorker,
                    questionnaireRepository,
                )
            // By initializing with this customFormId (in practice this requires selecting the form)
            // Let's pretend photos map has two photos matching our labels
            val photosMap = mapOf("Label0" to "/path/0.jpg", "Label1" to "/path/1.jpg")
            encounterDetailViewModel.initialize(patientId = patientId, visitId = "new", photosMap = photosMap)
            testDispatcher.scheduler.advanceUntilIdle()

            // Select the custom form
            encounterDetailViewModel.selectQuestionnaireById(customFormId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Fill answers
            encounterDetailViewModel.onFormUpdated(
                mapOf(
                    customQuestionnaire.item[0].linkId.value!! to "Option 1",
                    customQuestionnaire.item[1].linkId.value!! to "This is free text",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Finalize Encounter
            encounterDetailViewModel.finalizeEncounter()
            retries = 0
            while (!encounterDetailViewModel.uiState.value.isFinalized && retries < 100) {
                java.lang.Thread.sleep(50)
                testDispatcher.scheduler.advanceUntilIdle()
                retries++
            }
            assertTrue(encounterDetailViewModel.uiState.value.isFinalized, "Encounter should be finalized")
            val encounterId =
                encounterDetailViewModel.uiState.value.encounter
                    ?.id

            // Step 5: View questionnaires for a given patient (Encounters list)
            val patientDetailViewModel = PatientDetailViewModel(fhirRepository)
            patientDetailViewModel.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            val encounters = patientDetailViewModel.uiState.value.encounters
            assertTrue(encounters.isNotEmpty(), "Should list encounters for patient")
            assertEquals(encounterId, encounters[0].id, "Encounter ID should match")
            assertEquals(Encounter.EncounterStatus.Finished, encounters[0].status.value, "Encounter should be Finished")

            // Step 6: View specific questionnaire filled out for given patient
            val readOnlyEncounterViewModel =
                EncounterDetailViewModel(
                    fhirRepository,
                    authRepository,
                    syncWorker,
                    questionnaireRepository,
                )
            readOnlyEncounterViewModel.initialize(patientId = patientId, visitId = encounterId!!, photosMap = emptyMap())

            testDispatcher.scheduler.advanceUntilIdle()

            val answers = readOnlyEncounterViewModel.uiState.value.answers
            assertEquals("Option 1", answers[customQuestionnaire.item[0].linkId.value!!], "Answers should be persisted")
            assertEquals("This is free text", answers[customQuestionnaire.item[1].linkId.value!!])
            assertEquals(2, readOnlyEncounterViewModel.uiState.value.photos.size, "Photos should be loaded")

            // Step 7: Export questionnaire
            val serializedForm = questionnaireSharingService.serializeQuestionnaire(customQuestionnaire)
            assertTrue(serializedForm.contains("Ten Part Form"), "Exported form should contain title")
            assertTrue(serializedForm.contains("Label0"), "Exported form should contain labels")
            val deserializedForm = questionnaireSharingService.deserializeQuestionnaire(serializedForm)
            assertEquals(customQuestionnaire.id, deserializedForm.id, "Deserialized form should match original")

            // Step 8: Export dataset (incl. with password)
            // The exportData method typically writes a zip file and returns a success result.
            // In JVM context with createFileStorage it will create a real file.
            exportImportService.exportData(password = "securePassword123")
            testDispatcher.scheduler.advanceUntilIdle()

            // Step 9: Logout
            authRepository.logout()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(null, authRepository.currentUser.value, "User should be logged out")

            driver.close()
            storage.clearAll()
        }
}
