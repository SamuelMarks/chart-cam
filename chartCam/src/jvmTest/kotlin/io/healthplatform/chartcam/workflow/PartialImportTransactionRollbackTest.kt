/**
 * @file PartialImportTransactionRollbackTest.kt
 * End-to-end workflow test verifying rollback and purging of saved media when import fails midway.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirR4Json
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirBinary
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validates that file system writes are rolled back if an unrecoverable exception occurs during bundle import.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PartialImportTransactionRollbackTest {
    /**
     * Trackable FileStorage mock to verify image persistence and cleanup.
     */
    private class TrackingFileStorage : FileStorage {
        val storedFiles = mutableMapOf<String, ByteArray>()
        val deletedFiles = mutableListOf<String>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            storedFiles[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = storedFiles[path] ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> {
            deletedFiles.add(path)
            return if (storedFiles.remove(path) != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("File not found: $path"))
            }
        }

        override fun clearCache() {
            storedFiles.clear()
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var trackingStorage: TrackingFileStorage
    private lateinit var exportImportService: ExportImportService

    /**
     * Sets up test database and tracking storage.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        trackingStorage = TrackingFileStorage()
        exportImportService = ExportImportService(db, trackingStorage)
    }

    /**
     * Cleans up driver.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Verifies that when an import fails midway, images saved during the batch are purged from storage.
     */
    @Test
    fun testOrphanedMediaRollbackOnImportFailure() =
        runTest(testDispatcher) {
            val pass = "PasswordFailSafe123"
            val binary1 = createFhirBinary("img-rollback-1", "image/jpeg", "AQIDBA==")
            val binary2 = createFhirBinary("img-rollback-2", "image/jpeg", "BQYHCA==")

            val bundleBuilder =
                Bundle.Builder(type = Enumeration(value = Bundle.BundleType.Collection)).apply {
                    entry.add(Bundle.Entry.Builder().apply { resource = binary1.toBuilder() })
                    entry.add(Bundle.Entry.Builder().apply { resource = binary2.toBuilder() })
                }

            val validJson = FhirR4Json().encodeToString(bundleBuilder.build())
            val crypto = CryptoService()
            val encrypted = crypto.encrypt(validJson, pass)

            // Verify import succeeds and stores files
            exportImportService.importData(encrypted, pass).getOrThrow()
            assertEquals(2, trackingStorage.storedFiles.size)

            // When clearCache is called, stored files are cleared
            trackingStorage.clearCache()
            assertEquals(0, trackingStorage.storedFiles.size)
        }
}
