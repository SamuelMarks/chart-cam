package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.failed_to_import
import chartcam.chartcam.generated.resources.failed_to_load_patients
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockFhirRepository: MockFhirRepository
    private lateinit var mockExportImportService: MockExportImportService
    private lateinit var mockAuthRepository: MockAuthRepository
    private lateinit var viewModel: PatientListViewModel

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: ChartCamDatabase

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.create(driver)
        database = ChartCamDatabase(driver)

        mockFhirRepository = MockFhirRepository(database)

        val fakeFileStorage =
            object : FileStorage {
                override fun saveImage(
                    fileName: String,
                    bytes: ByteArray,
                ): String = fileName

                override fun readImage(path: String): ByteArray = ByteArray(0)

                override fun clearCache() {}
            }
        mockExportImportService = MockExportImportService(database, fakeFileStorage)

        val fakeSecureStorage =
            object : SecureStorage {
                override fun save(
                    key: String,
                    value: String,
                ) {}

                override fun getString(key: String): String? = null

                override fun delete(key: String) {}
            }
        val mockHttpClient = HttpClient(MockEngine) { engine { addHandler { respondOk() } } }

        mockAuthRepository = MockAuthRepository(mockHttpClient, fakeSecureStorage)

        mockAuthRepository.currentUserFlow.value =
            io.healthplatform.chartcam.models.createFhirPractitioner(
                "practitioner-1",
                "Smith",
                "John",
                true,
            )

        viewModel =
            PatientListViewModel(
                mockFhirRepository,
                mockExportImportService,
                mockAuthRepository,
            )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPatients sets patients in state`() =
        runTest(testDispatcher) {
            val patient = createFhirPatient("1", "John", "Doe", LocalDate(1990, 1, 1), "mrn1", "male")
            mockFhirRepository.patientsToReturn = listOf(patient)

            viewModel.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.patients.size)
            assertEquals(
                "1",
                viewModel.uiState.value.patients
                    .first()
                    .id,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `loadPatients handles error`() =
        runTest(testDispatcher) {
            mockFhirRepository.shouldThrow = true

            viewModel.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(Res.string.failed_to_load_patients, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `onSearchQueryChanged updates query and reloads`() =
        runTest(testDispatcher) {
            viewModel.onSearchQueryChanged("Jane")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Jane", viewModel.uiState.value.searchQuery)
            assertEquals("Jane", mockFhirRepository.lastSearchQuery)
        }

    @Test
    fun `setShowAllPatients updates state and reloads`() =
        runTest(testDispatcher) {
            viewModel.setShowAllPatients(true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showAllPatients)
            assertTrue(mockFhirRepository.lastShowAll)
        }

    @Test
    fun `createPatient saves patient and reloads`() =
        runTest(testDispatcher) {
            var createdId = ""
            viewModel.createPatient(
                firstName = "New",
                lastName = "Patient",
                mrn = "MRN2",
                dob = LocalDate(1980, 2, 2),
                gender = "female",
            ) { id ->
                createdId = id
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(mockFhirRepository.savedPatient)
            assertTrue(createdId.isNotEmpty())
        }

    @Test
    fun `exportData calls service and updates state`() =
        runTest(testDispatcher) {
            viewModel.exportData("password123", true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("exported-data", viewModel.uiState.value.exportedData)
            assertEquals("password123", viewModel.uiState.value.exportPassword)
        }

    @Test
    fun `clearExportData resets state`() =
        runTest(testDispatcher) {
            viewModel.exportData("pass", true)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearExportData()
            assertEquals(null, viewModel.uiState.value.exportedData)
            assertEquals(null, viewModel.uiState.value.exportPassword)
        }

    @Test
    fun `importData success reloads patients`() =
        runTest(testDispatcher) {
            var successCalled = false
            viewModel.importData("data", "pass") {
                successCalled = true
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals("data", mockExportImportService.lastImportData)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `importData failure sets error`() =
        runTest(testDispatcher) {
            mockExportImportService.shouldThrow = true
            viewModel.importData("data", "pass") {}
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(Res.string.failed_to_import, viewModel.uiState.value.error)
        }

    @Test
    fun `deleteAccount deletes practitioner and patients`() =
        runTest(testDispatcher) {
            val patient = createFhirPatient("p1", "Jane", "Doe", LocalDate(1990, 1, 1), "mrn", "female")
            mockFhirRepository.patientsToReturn = listOf(patient)

            var successCalled = false
            viewModel.deleteAccount {
                successCalled = true
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals("p1", mockFhirRepository.deletedPatientId)
            assertEquals("practitioner-1", mockFhirRepository.deletedPractitionerId)
            assertTrue(mockAuthRepository.deleteAccountCalled)
        }

    @Test
    fun `clearError sets error to null`() =
        runTest(testDispatcher) {
            mockExportImportService.shouldThrow = true
            viewModel.importData("data", "pass") {}
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearError()
            assertEquals(null, viewModel.uiState.value.error)
        }
}

class MockFhirRepository(
    database: ChartCamDatabase,
) : FhirRepository(database) {
    var patientsToReturn: List<Patient> = emptyList()
    var shouldThrow = false
    var lastSearchQuery = ""
    var lastShowAll = false
    var savedPatient: Patient? = null
    var deletedPatientId: String? = null
    var deletedPractitionerId: String? = null

    override suspend fun getAllPatients(
        showAll: Boolean,
        practitionerId: String?,
    ): List<Patient> {
        if (shouldThrow) throw Exception("DB Error")
        lastShowAll = showAll
        return patientsToReturn
    }

    override suspend fun searchPatients(
        query: String,
        showAll: Boolean,
        practitionerId: String?,
    ): List<Patient> {
        if (shouldThrow) throw Exception("DB Error")
        lastSearchQuery = query
        lastShowAll = showAll
        return patientsToReturn
    }

    override suspend fun savePatient(patient: Patient) {
        savedPatient = patient
    }

    override suspend fun deletePatient(id: String) {
        deletedPatientId = id
    }

    override suspend fun deletePractitioner(id: String) {
        deletedPractitionerId = id
    }
}

class MockExportImportService(
    database: ChartCamDatabase,
    fileStorage: FileStorage,
) : ExportImportService(database, fileStorage) {
    var shouldThrow = false
    var lastImportData: String? = null

    override suspend fun exportData(
        password: String,
        exportAll: Boolean,
        practitionerId: String?,
    ): String {
        if (shouldThrow) throw Exception("Export error")
        return "exported-data"
    }

    override suspend fun importData(
        encryptedData: String,
        password: String,
    ) {
        if (shouldThrow) throw Exception("Import error")
        lastImportData = encryptedData
    }
}

class MockAuthRepository(
    client: HttpClient,
    storage: SecureStorage,
) : AuthRepository(client, storage) {
    val currentUserFlow = MutableStateFlow<Practitioner?>(null)
    var deleteAccountCalled = false

    override val currentUser = currentUserFlow

    override suspend fun login(
        username: String,
        password: String,
    ): Result<Practitioner> =
        Result.success(
            io.healthplatform.chartcam.models
                .createFhirPractitioner("1", "S", "J", true),
        )

    override suspend fun checkSession(): Boolean = true

    override fun logout() {}

    override fun deleteAccount(username: String) {
        deleteAccountCalled = true
    }
}
