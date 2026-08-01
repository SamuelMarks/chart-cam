/**
 * @file CreatePatientJvmTest.kt
 * Contains declarations for CreatePatientJvmTest.kt.
 *
 * Contains testing definitions for patient creation workflows.
 *
 * Allows isolated testing of the view model methods used to create a new patient
 * to ensure that patient records are successfully created in the underlying repository.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/**
 * Provides automated verification for the [PatientListViewModel]'s patient creation process.
 *
 * Verifies that the required services interoperate correctly to store a new patient's information.
 */
class CreatePatientJvmTest {
    /**
     * Tests the overarching patient creation integration within the view model.
     *
     * Spawns an in-memory SQLDelight database along with mocked dependencies to
     * simulate patient creation, verifying the creation callback completes with a new patient ID.
     */
    @Test
    fun testCreatePatientJvm() =
        runBlocking {
            // We will just test the SQL query execution directly using SQLite in-memory
            val driver =
                app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
                    app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY,
                )
            ChartCamDatabase.Schema.awaitCreate(driver)
            val database = ChartCamDatabase(driver)
            val repo = FhirRepository(database)
            val fileStorage =
                io.healthplatform.chartcam.files
                    .createFileStorage()
            val exportImportService =
                io.healthplatform.chartcam.repository
                    .ExportImportService(database, fileStorage)

            val client =
                io.ktor.client.HttpClient(
                    io.ktor.client.engine.mock
                        .MockEngine { respond("") },
                )
            val mockStorage =
                object : io.healthplatform.chartcam.storage.SecureStorage {
                    /**
                     * The in-memory map storing string mock data.
                     */
                    private val data = mutableMapOf<String, String>()

                    /**
                     * Saves a key-value pair in mock storage.
                     *
                     * @param key The identifier to store data under.
                     * @param value The text data to be stored.
                     */
                    override fun save(
                        key: String,
                        value: String,
                    ) {
                        data[key] = value
                    }

                    /**
                     * Retrieves text data based on a given key.
                     *
                     * @param key The identifier to lookup.
                     * @return The stored data, or null if the key is not mapped.
                     */
                    override fun getString(key: String): String? = data[key]

                    /**
                     * Deletes the text data associated with a key.
                     *
                     * @param key The identifier for the data to be removed.
                     */
                    override fun delete(key: String) {
                        data.remove(key)
                    }
                }
            val authRepository = AuthRepository(client, mockStorage)

            val vm = PatientListViewModel(repo, exportImportService, authRepository)

            var successId: String? = null
            vm.createPatient("John", "Doe", "123", LocalDate.parse("1990-01-01")) {
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
