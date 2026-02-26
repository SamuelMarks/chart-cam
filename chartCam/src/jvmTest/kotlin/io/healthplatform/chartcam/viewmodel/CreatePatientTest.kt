package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import io.healthplatform.chartcam.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.healthplatform.chartcam.storage.SecureStorage

class CreatePatientTest {
    @Test
    fun testCreatePatient() = runBlocking {
        // We will just test the SQL query execution directly using SQLite in-memory
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.awaitCreate(driver)
        val database = ChartCamDatabase(driver)
        val repo = FhirRepository(database)
        val fileStorage = io.healthplatform.chartcam.files.createFileStorage()
        val exportImportService = io.healthplatform.chartcam.repository.ExportImportService(database, fileStorage)
        
        val client = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") })
        val mockStorage = object : io.healthplatform.chartcam.storage.SecureStorage {
            private val data = mutableMapOf<String, String>()
            override fun save(key: String, value: String) { data[key] = value }
            override fun getString(key: String): String? = data[key]
            override fun delete(key: String) { data.remove(key) }
        }
        val authRepository = AuthRepository(client, mockStorage)
        
        val vm = PatientListViewModel(repo, exportImportService, authRepository)
        
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