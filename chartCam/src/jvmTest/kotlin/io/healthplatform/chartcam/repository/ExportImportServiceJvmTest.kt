/**
 * @file ExportImportServiceJvmTest.kt
 * Contains declarations for ExportImportServiceJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for ExportImportService on JVM.
 */
class ExportImportServiceJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var fileStorage: MockFileStorage
    private lateinit var cryptoService: CryptoService
    private lateinit var service: ExportImportService

    /**
     * Sets up the test environment.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fileStorage = MockFileStorage()
        cryptoService = CryptoService()
        service = ExportImportService(db, fileStorage)
    }

    /**
     * Tears down the test environment.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests exporting and importing data.
     */
    @Test
    fun testExportAndImportData() =
        runTest {
            // Insert some dummy data to export
            val fhirRepo = FhirRepository(db)

            // Setup Practitioner
            val prac =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac_1"
                        active =
                            com.google.fhir.model.r4.Boolean
                                .Builder()
                                .apply { value = true }
                    }.build()
            fhirRepo.savePractitioner(prac)

            // Test Export
            val password = "securePassword123"
            val encryptedData = service.exportData(password, exportAll = true)

            assertNotNull(encryptedData)
            assertTrue(encryptedData.isNotEmpty())

            // Test Import on a new database to verify
            val driver2 = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver2)
            val db2 = ChartCamDatabase(driver2)
            val service2 = ExportImportService(db2, fileStorage)

            service2.importData(encryptedData, password)

            val fhirRepo2 = FhirRepository(db2)
            val importedPrac = fhirRepo2.getPractitioner("prac_1")
            assertNotNull(importedPrac)
            assertEquals("prac_1", importedPrac.id)

            driver2.close()
        }

    /**
     * Tests malformed data import failure.
     */
    @Test
    fun testMalformedDataImportFails() =
        runTest {
            val password = "securePassword123"
            // Encrypt some garbage JSON that is not a Bundle
            val malformedJson = """{"resourceType": "Patient", "id": "fake"}"""
            val encryptedData = cryptoService.encrypt(malformedJson, password)

            var exceptionThrown = false
            try {
                service.importData(encryptedData, password)
            } catch (e: Exception) {
                exceptionThrown = true
            }

            assertTrue(exceptionThrown, "Expected an exception when importing malformed FHIR data")
        }

    /**
     * Tests wrong password import failure.
     */
    @Test
    fun testWrongPasswordFails() =
        runTest {
            val password = "securePassword123"
            val encryptedData = service.exportData(password, exportAll = true)

            var exceptionThrown = false
            try {
                service.importData(encryptedData, "wrong_password")
            } catch (e: Exception) {
                exceptionThrown = true
            }

            assertTrue(exceptionThrown, "Expected an exception when decrypting with the wrong password")
        }

    /**
     * Tests export with a missing image.
     */
    @Test
    fun testExportWithMissingImage() =
        runTest {
            val fhirRepo = FhirRepository(db)
            val docRef =
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc1",
                        patientId = "pat1",
                        encounterId = "enc1",
                        dateStr = "2026-07-09T00:00:00Z",
                        desc = "desc",
                        mime = "image/jpeg",
                        urlPath = "missing.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(docRef)

            val encryptedData = service.exportData("password", exportAll = true)
            assertTrue(encryptedData.isNotEmpty())
        }

    /**
     * Mock file storage for tests.
     */
    class MockFileStorage : FileStorage {
        /** Maps paths to byte arrays. */
        val files = mutableMapOf<String, ByteArray>()

        /**
         * Save an image.
         * @param fileName The name of the file.
         * @param bytes The byte array.
         * @return The saved path.
         */
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return fileName
        }

        /**
         * Read an image.
         * @param path The path of the file.
         * @return The byte array.
         */
        override fun readImage(path: String): ByteArray = files[path] ?: throw IllegalArgumentException("File not found")

        /** Clear cache. */
        override fun clearCache() {
            files.clear()
        }
    }
}
