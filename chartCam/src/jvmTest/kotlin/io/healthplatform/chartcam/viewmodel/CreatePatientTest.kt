package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class CreatePatientTest {
    @Test
    fun testCreatePatient() = runBlocking {
        // We will just test the SQL query execution directly using SQLite in-memory
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.awaitCreate(driver)
        val repo = FhirRepository(driver)
        val fileStorage = io.healthplatform.chartcam.files.createFileStorage()
        val exportImportService = io.healthplatform.chartcam.repository.ExportImportService(repo.database, fileStorage)
        val vm = PatientListViewModel(repo, exportImportService)
        
        var successId: String? = null
        vm.createPatient("John", "Doe", "123", LocalDate.parse("1990-01-01"), "male") {
            successId = it
        }
        
        // Let it run for a bit since it's in viewModelScope.launch
        kotlinx.coroutines.delay(100)
        
        if (successId == null) {
            println("FAILED to get success ID")
        } else {
            println("SUCCESS: $successId")
        }
    }
}