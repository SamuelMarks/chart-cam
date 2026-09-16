/**
 * @file ExportImportServiceTest.kt
 * Contains declarations for ExportImportServiceTest.kt.
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for the data export and import services.
 */
class ExportImportServiceTest {
    /**
     * Verifies that export with a password shorter than 6 characters fails with Result.failure.
     */
    @Test
    fun testExportWithWeakPasswordFails() =
        runTest {
            runCatching {
                val driver = DatabaseDriverFactory().createDriver()
                val database = ChartCamDatabase(driver)
                val service = ExportImportService(database, createFileStorage())

                val weakPasswordResult = service.exportData(password = "123")
                assertTrue(weakPasswordResult.isFailure)
            }.onFailure {
                assertTrue(it is IllegalStateException)
            }
        }
}
