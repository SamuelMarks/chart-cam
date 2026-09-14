/**
 * @file ImportedDataSyncEnginePoisoningTest.kt
 * End-to-end workflow test verifying that imported data does not poison the sync engine tracking tables.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Validates that ingesting an external archive does not record new mutations in LocalChangeEntity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportedDataSyncEnginePoisoningTest {
    /**
     * FileStorage stub.
     */
    private class DummyFileStorage : FileStorage {
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String = fileName

        override fun readImage(path: String): ByteArray = ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> = Result.failure(Exception("Not supported in stub"))

        override fun clearCache() {}
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driverSource: JdbcSqliteDriver
    private lateinit var driverTarget: JdbcSqliteDriver
    private lateinit var repoSource: FhirRepository
    private lateinit var repoTarget: FhirRepository
    private lateinit var exportImportSource: ExportImportService
    private lateinit var exportImportTarget: ExportImportService

    /**
     * Prepares test databases.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        driverSource = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverSource)
        val dbSource = ChartCamDatabase(driverSource)
        repoSource = FhirRepository(dbSource)
        exportImportSource = ExportImportService(dbSource, DummyFileStorage())

        driverTarget = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverTarget)
        val dbTarget = ChartCamDatabase(driverTarget)
        repoTarget = FhirRepository(dbTarget)
        exportImportTarget = ExportImportService(dbTarget, DummyFileStorage())
    }

    /**
     * Cleans up test databases.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverSource.close()
        driverTarget.close()
    }

    /**
     * Verifies that importing records creates zero rows in LocalChangeEntity.
     */
    @Test
    fun testImportDoesNotPoisonLocalChangeTable() =
        runTest(testDispatcher) {
            // Populate source device
            for (i in 1..4) {
                val pid = "sync-test-p-$i"
                repoSource.savePatient(createFhirPatient(pid, "First$i", "Last$i", LocalDate(1988, 4, i), "MRN-SYNC-$i"))
                repoSource.saveEncounter(createFhirEncounter("sync-test-e-$i", pid, "dr-sync", "2026-09-14T10:0$i:00Z"))
            }

            val pass = "PasswordSyncClean!"
            val archive = exportImportSource.exportData(pass, exportAll = true).getOrThrow()

            // Target starts with zero pending local changes
            assertEquals(0, repoTarget.getPendingLocalChangesCount())

            // Import archive into target
            exportImportTarget.importData(archive, pass).getOrThrow()

            // Verify patients and encounters are in target database
            assertNotNull(repoTarget.getPatient("sync-test-p-1"))
            assertNotNull(repoTarget.getEncounter("sync-test-e-1"))

            // Verify local change entity table is STILL 0 (NOT poisoned)
            assertEquals(
                0,
                repoTarget.getPendingLocalChangesCount(),
                "Imported external records must not be marked as new local mutations",
            )
        }
}
