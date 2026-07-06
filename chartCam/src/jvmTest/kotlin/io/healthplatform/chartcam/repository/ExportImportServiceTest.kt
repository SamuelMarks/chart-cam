package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.*
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExportImportServiceTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var fileStorage: MockFileStorage
    private lateinit var cryptoService: CryptoService
    private lateinit var service: ExportImportService

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fileStorage = MockFileStorage()
        cryptoService = CryptoService()
        service = ExportImportService(db, fileStorage, cryptoService)
    }

    @After
    fun tearDown() {
        driver.close()
    }

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
            val service2 = ExportImportService(db2, fileStorage, cryptoService)

            service2.importData(encryptedData, password)

            val fhirRepo2 = FhirRepository(db2)
            val importedPrac = fhirRepo2.getPractitioner("prac_1")
            assertNotNull(importedPrac)
            assertEquals("prac_1", importedPrac.id)

            driver2.close()
        }

    class MockFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = files[path] ?: throw Exception("File not found")

        override fun clearCache() {
            files.clear()
        }
    }
}
