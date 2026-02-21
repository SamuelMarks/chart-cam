package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Practitioner
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.sync.SyncManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EncounterDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fhirRepository: FhirRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var syncManager: SyncManager
    private lateinit var questionnaireRepository: QuestionnaireRepository
    private lateinit var viewModel: EncounterDetailViewModel
    private lateinit var driver: JdbcSqliteDriver

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        io.healthplatform.chartcam.database.ChartCamDatabase.Schema.synchronous().create(driver)
        fhirRepository = FhirRepository(driver)
        
        val mockEngine = MockEngine { request -> respondOk() }
        val httpClient = NetworkClient.create(mockEngine)
        
        authRepository = AuthRepository(httpClient, MockStorage())
        authRepository.login("dr_smith", "password123")
        
        syncManager = SyncManager(fhirRepository, httpClient)
        questionnaireRepository = QuestionnaireRepository()

        val patient = Patient(
            id = "pat-123",
            name = listOf(HumanName("Doe", listOf("John"))),
            birthDate = LocalDate.parse("1980-01-01"),
            mrn = "MRN-123",
            gender = "male"
        )
        fhirRepository.savePatient(patient)

        viewModel = EncounterDetailViewModel(fhirRepository, authRepository, syncManager, questionnaireRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    @Test
    fun `initialize new encounter creates draft and processes photos`() = runTest(testDispatcher) {
        val photosMap = mapOf("Front" to "path/front.jpg", "Left" to "path/left.jpg")
        
        viewModel.initialize("pat-123", "new", photosMap)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("pat-123", state.patient?.id)
        assertEquals("in-progress", state.encounter?.status)
        assertEquals(2, state.photos.size)
        assertEquals("", state.notes)
    }

    @Test
    fun `finalize encounter sets status to finished and syncs`() = runTest(testDispatcher) {
        viewModel.initialize("pat-123", "new", emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.finalizeEncounter()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.isFinalized)
        
        val enc = fhirRepository.getEncounter(state.encounter!!.id)
        assertEquals("finished", enc?.status)
    }

    @Test
    fun `update notes persists locally`() = runTest(testDispatcher) {
        viewModel.initialize("pat-123", "new", emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNotesChanged("Patient looks stable.")
        testDispatcher.scheduler.advanceUntilIdle()

        val encId = viewModel.uiState.value.encounter!!.id
        val dbEnc = fhirRepository.getEncounter(encId)
        
        assertEquals("Patient looks stable.", viewModel.uiState.value.notes)
        assertEquals("Patient looks stable.", dbEnc?.text)
    }

    @Test
    fun `selectQuestionnaire updates state`() = runTest(testDispatcher) {
        viewModel.initialize("pat-123", "new", emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()
        
        val q = questionnaireRepository.getQuestionnaire("basic-followup")!!
        viewModel.selectQuestionnaire(q)
        
        assertEquals(q, viewModel.uiState.value.selectedQuestionnaire)
    }

    @Test
    fun `createAndSelectQuestionnaire updates state with new form`() = runTest(testDispatcher) {
        viewModel.initialize("pat-123", "new", emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.createAndSelectQuestionnaire("My Custom Form", 2)
        
        val q = viewModel.uiState.value.selectedQuestionnaire
        assertEquals("My Custom Form", q?.title)
        assertEquals(3, q?.item?.size) // notes + 2 photos
    }

    @Test
    fun `resetFinalized resets flag`() = runTest(testDispatcher) {
        viewModel.initialize("pat-123", "new", emptyMap())
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.finalizeEncounter()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFinalized)
        
        viewModel.resetFinalized()
        assertFalse(viewModel.uiState.value.isFinalized)
    }
}

class MockStorage : SecureStorage {
    private val data = mutableMapOf<String, String>()
    override fun save(key: String, value: String) { data[key] = value }
    override fun getString(key: String): String? = data[key]
    override fun delete(key: String) { data.remove(key) }
}
