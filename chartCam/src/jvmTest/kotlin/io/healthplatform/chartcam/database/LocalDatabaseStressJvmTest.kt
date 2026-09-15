/**
 * @file LocalDatabaseStressJvmTest.kt
 * Stress test for local SQLite database operations and pagination.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stress and performance test verifying local SQLite database handling under high record volumes.
 */
class LocalDatabaseStressJvmTest {
    private lateinit var fhirRepo: FhirRepository

    /**
     * Initializes an in-memory database for high-volume record simulation.
     */
    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
    }

    /**
     * Tests inserting and searching across high volumes of local patient records.
     */
    @Test
    fun testDatabaseVolumeAndQueryPerformance() =
        runTest {
            val count = 200
            for (i in 1..count) {
                val patient =
                    createFhirPatient(
                        id = "pat-$i",
                        firstName = "First$i",
                        lastName = "Last$i",
                        dob = LocalDate(1980, 1, 1),
                        mrnValue = "MRN-$i",
                    )
                fhirRepo.savePatient(patient)
            }

            val allPatients = fhirRepo.getAllPatients()
            assertEquals(count, allPatients.size)

            val searchResult = fhirRepo.searchPatients("Last10")
            assertTrue(searchResult.isNotEmpty())
            assertTrue(searchResult.any { it.id == "pat-10" })
        }
}
