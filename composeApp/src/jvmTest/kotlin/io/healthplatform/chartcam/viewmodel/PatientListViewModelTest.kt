package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.createFhirPatient
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.utils.CryptoService
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.familyName
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        exportImportService = ExportImportService(database, fileStorage, CryptoService())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchFilterStateLogic() = runTest {
        // Seed
        repo.savePatient(createFhirPatient("1", "John", "Doe", LocalDate(1990,1,1), "123", "male"))
        
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
        vm.createPatient("Alice", "Test", "MRN-A", LocalDate(2000,1,1), "female") { id ->
            createdId = id
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = vm.uiState.value
        assertEquals(1, state.patients.size)
        assertEquals("Alice", state.patients[0].name.firstOrNull()?.givenName)
        assertTrue(createdId != null)
    }

    @Test
    fun testExportImportFlow() = runTest {
        val vm = PatientListViewModel(repo, exportImportService)
        testDispatcher.scheduler.advanceUntilIdle()
        
        vm.createPatient("Exp", "Ort", "999", LocalDate(2000,1,1), "male") {}
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Test export
        vm.exportData("secret")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val exported = vm.uiState.value.exportedData
        assertNotNull(exported)
        assertEquals("secret", vm.uiState.value.exportPassword)
        
        // Test clear
        vm.clearExportData()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.exportedData)
        
        // Test import
        var importSuccess = false
        vm.importData(exported, "secret") {
            importSuccess = true
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(importSuccess)
        assertNull(vm.uiState.value.error)
        
        // Test import fail
        vm.importData(exported, "wrongpassword") {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        
        vm.clearError()
        assertNull(vm.uiState.value.error)
    }
}
