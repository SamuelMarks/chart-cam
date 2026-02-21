package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertTrue

class PatientListViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FhirRepository
    private lateinit var fileStorage: io.healthplatform.chartcam.files.FileStorage
    private lateinit var exportImportService: ExportImportService

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        val database = ChartCamDatabase(driver)
        repo = FhirRepository(database)
        fileStorage = createFileStorage()
        exportImportService = ExportImportService(database, fileStorage)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchFilterStateLogic() = runTest {
        // Seed
        repo.savePatient(Patient("1", listOf(HumanName("Doe", listOf("John"))), LocalDate(1990,1,1), "m", "123"))
        
        val vm = PatientListViewModel(repo, exportImportService)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for init load
        
        // Initial check
        assertEquals(1, vm.uiState.value.patients.size)
        
        // Filter empty
        vm.onSearchQueryChanged("Nobody")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.patients.size)
        
        // Clear filter
        vm.onSearchQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.patients.size)
    }
    
    @Test
    fun testPatientCreation() = runTest {
        val vm = PatientListViewModel(repo, exportImportService)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.patients.size)
        
        var createdId: String? = null
        vm.createPatient("Alice", "Test", "MRN-A", LocalDate(2000,1,1), "f") { id ->
            createdId = id
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = vm.uiState.value
        assertEquals(1, state.patients.size)
        assertEquals("Alice", state.patients[0].name[0].given[0])
        assertTrue(createdId != null)
    }
}