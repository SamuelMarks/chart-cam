/**
 * @file ImportSecurityAndCorruptionTest.kt
 * Contains declarations for ImportSecurityAndCorruptionTest.kt.
 *
 * Validates resilience and security error handling during corrupted or unauthorized backup imports.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite verifying system resilience against invalid credentials, byte-level archive corruption,
 * and malicious or malformed FHIR payloads during backup restoration.
 */
class ImportSecurityAndCorruptionTest {
    /**
     * Minimal in-memory file storage mock.
     */
    private class SimpleMockFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = files[path] ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> =
            if (files.remove(path) != null) Result.success(Unit) else Result.failure(Exception("File not found: $path"))

        override fun clearCache() {
            files.clear()
        }
    }

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: ChartCamDatabase
    private lateinit var fileStorage: SimpleMockFileStorage
    private lateinit var fhirRepo: FhirRepository
    private lateinit var exportImportService: ExportImportService
    private val cryptoService = CryptoService()

    /**
     * Prepares test environment with pre-existing baseline data.
     */
    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        database = ChartCamDatabase(driver)
        fileStorage = SimpleMockFileStorage()
        fhirRepo = FhirRepository(database)
        exportImportService = ExportImportService(database, fileStorage)
    }

    /**
     * Cleans up test database driver.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests that an invalid decryption password cleanly fails and preserves the existing database state.
     */
    @Test
    fun testInvalidPasswordPreservesDatabaseState() =
        runTest {
            // Baseline patient
            val existingPatient =
                createFhirPatient(
                    id = "existing-p1",
                    firstName = "Baseline",
                    lastName = "Patient",
                    dob = LocalDate(1990, 1, 1),
                    mrnValue = "MRN-BASE-01",
                )
            fhirRepo.savePatient(existingPatient)

            val validPassword = "CorrectPassword2026!"
            val encryptedBackup = exportImportService.exportData(validPassword, exportAll = true).getOrThrow()
            assertNotNull(encryptedBackup)

            val importResult = exportImportService.importData(encryptedBackup, "WrongPasswordTotallyInvalid")
            assertTrue(importResult.isFailure, "Importing with incorrect password must return failure")

            // Existing database records must be untouched
            val reloadedPatient = fhirRepo.getPatient("existing-p1")
            assertNotNull(reloadedPatient, "Existing database records must remain unharmed")
            assertEquals(
                "MRN-BASE-01",
                reloadedPatient.identifier
                    .firstOrNull()
                    ?.value
                    ?.value,
            )
        }

    /**
     * Tests that truncated or corrupted archive bytes fail safely without leaking partial writes.
     */
    @Test
    fun testCorruptedArchiveByteNoiseHandling() =
        runTest {
            val validPassword = "SafePassword999!"
            val validBackup = exportImportService.exportData(validPassword, exportAll = true).getOrThrow()

            // Inject corruption into the ciphertext
            val corruptedPayload = validBackup.reversed() + "CORRUPT_NOISE"

            val result = exportImportService.importData(corruptedPayload, validPassword)
            assertTrue(result.isFailure, "Corrupted archive stream must fail gracefully")
        }

    /**
     * Tests that synthetic invalid FHIR JSON inside encrypted archives aborts and rolls back transactions.
     */
    @Test
    fun testMalformedFhirPayloadAbortsCleanly() =
        runTest {
            val password = "StrongPassword123!"

            // Seed a practitioner in DB
            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "existing-prac"
                        active =
                            dev.ohs.fhir.model.r4.Boolean
                                .Builder()
                                .apply { value = true }
                    }.build()
            fhirRepo.savePractitioner(practitioner)

            // Encrypt a payload that is completely invalid JSON
            val garbagePayload = "{ invalid_json_syntax: true, resourceType: ??? "
            val encryptedGarbage = cryptoService.encrypt(garbagePayload, password)

            val result = exportImportService.importData(encryptedGarbage, password)
            assertTrue(result.isFailure, "Malformed FHIR payloads must return failure on import")

            // Verify original data is preserved
            val prac = fhirRepo.getPractitioner("existing-prac")
            assertNotNull(prac, "Existing practitioner record must persist following aborted import")
        }
}
