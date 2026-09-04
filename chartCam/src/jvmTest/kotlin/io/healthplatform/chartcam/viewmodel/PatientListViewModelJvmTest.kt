/**
 * @file PatientListViewModelJvmTest.kt
 * Contains declarations for PatientListViewModelJvmTest.kt.
 */
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

/**
 * Test class PatientListViewModelTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatientListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockFhirRepository: MockFhirRepository
    private lateinit var mockExportImportService: MockExportImportService
    private lateinit var mockAuthRepository: MockAuthRepository
    private lateinit var viewModel: PatientListViewModel

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: ChartCamDatabase

    /**
     * Setup setup.
     */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.create(driver)
        database = ChartCamDatabase(driver)

        mockFhirRepository = MockFhirRepository(database)

        val fakeFileStorage =
            object : FileStorage {
                /**
                 * Override saveImage.
                 * @param fileName The name.
                 * @param bytes The data.
                 * @return The path.
                 */
                override fun saveImage(
                    fileName: String,
                    bytes: ByteArray,
                ): String = fileName

                /**
                 * Override readImage.
                 * @param path The path.
                 * @return The data.
                 */
                override fun readImage(path: String): ByteArray = ByteArray(0)

                /** Override clearCache */
                override fun clearCache() {}
            }
        mockExportImportService = MockExportImportService(database, fakeFileStorage)

        val fakeSecureStorage =
            object : SecureStorage {
                /**
                 * Override save.
                 * @param key The key.
                 * @param value The value.
                 */
                override fun save(
                    key: String,
                    value: String,
                ) {}

                /**
                 * Override getString.
                 * @param key The key.
                 * @return The value or null.
                 */
                override fun getString(key: String): String? = null

                /**
                 * Override delete.
                 * @param key The key.
                 */
                override fun delete(key: String) {}
            }
        mockAuthRepository = MockAuthRepository(fakeSecureStorage)

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

    /**
     * Teardown tearDown.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
        Dispatchers.resetMain()
    }

    /**
     * Test loadPatients.
     */
    @Test
    fun `loadPatients sets patients in state`() =
        runTest(testDispatcher) {
            val patient = createFhirPatient("1", "John", "Doe", LocalDate(1990, 1, 1), "mrn1")
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

    /**
     * Test loadPatients error.
     */
    @Test
    fun `loadPatients handles error`() =
        runTest(testDispatcher) {
            mockFhirRepository.shouldThrow = true

            viewModel.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(Res.string.failed_to_load_patients, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    /**
     * Test onSearchQueryChanged.
     */
    @Test
    fun `onSearchQueryChanged updates query and reloads`() =
        runTest(testDispatcher) {
            viewModel.onSearchQueryChanged("Jane")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Jane", viewModel.uiState.value.searchQuery)
            assertEquals("Jane", mockFhirRepository.lastSearchQuery)
        }

    /**
     * Test setShowAllPatients.
     */
    @Test
    fun `setShowAllPatients updates state and reloads`() =
        runTest(testDispatcher) {
            viewModel.setShowAllPatients(true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showAllPatients)
            assertTrue(mockFhirRepository.lastShowAll)
        }

    /**
     * Test createPatient.
     */
    @Test
    fun `createPatient saves patient and reloads`() =
        runTest(testDispatcher) {
            var createdId = ""
            viewModel.createPatient(
                firstName = "New",
                lastName = "Patient",
                mrn = "MRN2",
                dob = LocalDate(1980, 2, 2),
            ) { id ->
                createdId = id
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(mockFhirRepository.savedPatient)
            assertTrue(createdId.isNotEmpty())
        }

    /**
     * Test exportData.
     */
    @Test
    fun `exportData calls service and updates state`() =
        runTest(testDispatcher) {
            viewModel.exportData("password123", true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("exported-data", viewModel.uiState.value.exportedData)
            assertEquals("password123", viewModel.uiState.value.exportPassword)
        }

    /**
     * Test clearExportData.
     */
    @Test
    fun `clearExportData resets state`() =
        runTest(testDispatcher) {
            viewModel.exportData("pass", true)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearExportData()
            assertEquals(null, viewModel.uiState.value.exportedData)
            assertEquals(null, viewModel.uiState.value.exportPassword)
        }

    /**
     * Test importData success.
     */
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

    /**
     * Test importData failure.
     */
    @Test
    fun `importData failure sets error`() =
        runTest(testDispatcher) {
            mockExportImportService.shouldThrow = true
            viewModel.importData("data", "pass") {}
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(Res.string.failed_to_import, viewModel.uiState.value.error)
        }

    /**
     * Test deleteAccount.
     */
    @Test
    fun `deleteAccount deletes practitioner and patients`() =
        runTest(testDispatcher) {
            val patient = createFhirPatient("p1", "Jane", "Doe", LocalDate(1990, 1, 1), "mrn")
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

    /**
     * Test clearError.
     */
    @Test
    fun `clearError sets error to null`() =
        runTest(testDispatcher) {
            mockExportImportService.shouldThrow = true
            viewModel.importData("data", "pass") {}
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearError()
            assertEquals(null, viewModel.uiState.value.error)
        }

    /**
     * Test createPatient error.
     */
    @Test
    fun `createPatient handles error gracefully`() =
        runTest(testDispatcher) {
            mockFhirRepository.shouldThrowOnSave = true
            var successCalled = false
            viewModel.createPatient(
                firstName = "Err",
                lastName = "Patient",
                mrn = "MRN",
                dob = LocalDate(1980, 2, 2),
            ) {
                successCalled = true
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(successCalled)
        }

    /**
     * Test exportData error.
     */
    @Test
    fun `exportData handles error gracefully`() =
        runTest(testDispatcher) {
            mockExportImportService.shouldThrow = true
            viewModel.exportData("password123", true)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.exportedData)
        }
}

/**
 * Test class MockFhirRepository.
 */
class MockFhirRepository(
    database: ChartCamDatabase,
) : FhirRepository(database) {
    var patientsToReturn: List<Patient> = emptyList()
    var shouldThrow = false
    var shouldThrowOnSave = false
    var lastSearchQuery = ""
    var lastShowAll = false
    var savedPatient: Patient? = null
    var deletedPatientId: String? = null
    var deletedPractitionerId: String? = null

    /**
     * Override getAllPatients.
     * @param showAll Boolean flag.
     * @param practitionerId ID.
     * @return List of patients.
     */
    override suspend fun getAllPatients(
        showAll: Boolean,
        practitionerId: String?,
    ): List<Patient> {
        if (shouldThrow) throw IllegalStateException("DB Error")
        lastShowAll = showAll
        return patientsToReturn
    }

    /**
     * Override searchPatients.
     * @param query Query string.
     * @param showAll Boolean flag.
     * @param practitionerId ID.
     * @return List of patients.
     */
    override suspend fun searchPatients(
        query: String,
        showAll: Boolean,
        practitionerId: String?,
    ): List<Patient> {
        if (shouldThrow) throw IllegalStateException("DB Error")
        lastSearchQuery = query
        lastShowAll = showAll
        return patientsToReturn
    }

    /**
     * Override savePatient.
     * @param patient The patient.
     */
    override suspend fun savePatient(patient: Patient) {
        if (shouldThrowOnSave) throw IllegalStateException("Save Error")
        savedPatient = patient
    }

    /**
     * Override deletePatient.
     * @param id The ID.
     */
    override suspend fun deletePatient(id: String) {
        deletedPatientId = id
    }

    /**
     * Override deletePractitioner.
     * @param id The ID.
     */
    override suspend fun deletePractitioner(id: String) {
        deletedPractitionerId = id
    }
}

/**
 * Test class MockExportImportService.
 */
class MockExportImportService(
    database: ChartCamDatabase,
    fileStorage: FileStorage,
) : ExportImportService(database, fileStorage) {
    var shouldThrow = false
    var lastImportData: String? = null

    /**
     * Override exportData.
     * @param password Password.
     * @param exportAll Export all flag.
     * @param practitionerId Practitioner ID.
     * @return Export string.
     */
    override suspend fun exportData(
        password: String,
        exportAll: Boolean,
        practitionerId: String?,
    ): String {
        if (shouldThrow) throw IllegalStateException("Export error")
        return "exported-data"
    }

    /**
     * Override importData.
     * @param encryptedData Encrypted data.
     * @param password Password.
     */
    override suspend fun importData(
        encryptedData: String,
        password: String,
    ) {
        if (shouldThrow) throw IllegalStateException("Import error")
        lastImportData = encryptedData
    }
}

/**
 * Test class MockAuthRepository.
 */
class MockAuthRepository(
    storage: SecureStorage,
) : AuthRepository(storage) {
    val currentUserFlow = MutableStateFlow<Practitioner?>(null)
    var deleteAccountCalled = false

    override val currentUser = currentUserFlow

    /**
     * Override login.
     * @param username Username.
     * @param password Password.
     * @return Result.
     */
    override suspend fun login(
        username: String,
        password: String,
    ): Result<Practitioner> =
        Result.success(
            io.healthplatform.chartcam.models
                .createFhirPractitioner("1", "S", "J", true),
        )

    /**
     * Override checkSession.
     * @return Boolean.
     */
    override suspend fun checkSession(): Boolean = true

    /** Override logout */
    override fun logout() {}

    /**
     * Override deleteAccount.
     * @param username Username.
     */
    override fun deleteAccount(username: String) {
        deleteAccountCalled = true
    }
}
