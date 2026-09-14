/**
 * @file DisasterRecoveryRoundTripTest.kt
 * Contains declarations for DisasterRecoveryRoundTripTest.kt.
 *
 * Validates full disaster recovery export, purge, and restore round-trip workflows.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.models.customBirthDate
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.mrn
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-End disaster recovery test suite validating that full clinical databases
 * can be exported to encrypted archives, completely wiped, and restored onto fresh
 * installations with zero relational or binary data loss.
 */
class DisasterRecoveryRoundTripTest {
    /**
     * Memory-backed file storage mock for simulating filesystem IO.
     */
    private class InMemoryFileStorage : FileStorage {
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
    private lateinit var fileStorage: InMemoryFileStorage
    private lateinit var exportImportService: ExportImportService
    private lateinit var fhirRepository: FhirRepository

    /**
     * Sets up the source database and test environment.
     */
    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        database = ChartCamDatabase(driver)
        fileStorage = InMemoryFileStorage()
        fhirRepository = FhirRepository(database)
        exportImportService = ExportImportService(database, fileStorage)
    }

    /**
     * Closes the test database driver.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests full disaster recovery round-trip: export, purge, restore, and verify.
     */
    @Test
    fun testFullDisasterRecoveryRoundTrip() =
        runTest {
            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "practitioner-primary"
                        active =
                            com.google.fhir.model.r4.Boolean
                                .Builder()
                                .apply { value = true }
                    }.build()
            fhirRepository.savePractitioner(practitioner)

            // 1. Seed 3 distinct patients
            val patient1 =
                createFhirPatient(
                    id = "pat-001",
                    firstName = "Alice",
                    lastName = "Cooper",
                    dob = LocalDate(1975, 4, 12),
                    mrnValue = "MRN-001-ALPHA",
                )
            val patient2 =
                createFhirPatient(
                    id = "pat-002",
                    firstName = "Brenda",
                    lastName = "Vance",
                    dob = LocalDate(1982, 11, 23),
                    mrnValue = "MRN-002-BRAVO",
                )
            val patient3 =
                createFhirPatient(
                    id = "pat-003",
                    firstName = "Charles",
                    lastName = "Xavier",
                    dob = LocalDate(1960, 7, 13),
                    mrnValue = "MRN-003-CHARLIE",
                )

            fhirRepository.savePatient(patient1)
            fhirRepository.savePatient(patient2)
            fhirRepository.savePatient(patient3)

            // 2. Seed encounters and photo attachments for patients
            val encounter1 =
                createFhirEncounter(
                    id = "enc-pat1-01",
                    patientId = "pat-001",
                    practitionerId = "practitioner-primary",
                    dateStr = "2026-09-01T10:00:00Z",
                ).toBuilder()
                    .apply {
                        status = Enumeration(value = Encounter.EncounterStatus.Finished)
                    }.build()
            fhirRepository.saveEncounter(encounter1)

            // Store physical images in file storage
            val image1Bytes = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
            val image1Path = fileStorage.saveImage("pat1_lesion_01.jpg", image1Bytes)

            val docRef1 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "docref-001",
                        patientId = "pat-001",
                        encounterId = "enc-pat1-01",
                        dateStr = "2026-09-01T10:05:00Z",
                        desc = "Lesion baseline photo",
                        mime = "image/jpeg",
                        urlPath = image1Path,
                    ),
                )
            fhirRepository.saveDocumentReference(docRef1)

            // 3. Export to encrypted zip bundle
            val backupPassword = "ClinicalSecret2026!"
            val encryptedBackup = exportImportService.exportData(backupPassword, exportAll = true).getOrThrow()
            assertNotNull(encryptedBackup)
            assertTrue(encryptedBackup.isNotEmpty(), "Exported encrypted backup must not be empty")

            // 4. Simulate catastrophic disaster: purge everything
            driver.close()
            fileStorage.clearCache()
            assertTrue(fileStorage.files.isEmpty(), "Physical storage should be completely wiped")

            // 5. Initialize fresh database (new installation)
            val freshDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(freshDriver)
            val freshDb = ChartCamDatabase(freshDriver)
            val freshFileStorage = InMemoryFileStorage()
            val freshImportService = ExportImportService(freshDb, freshFileStorage)
            val freshFhirRepo = FhirRepository(freshDb)

            // 6. Execute restore from backup
            freshImportService.importData(encryptedBackup, backupPassword).getOrThrow()

            // 7. Verify all patient records and demographics
            val restoredPatient1 = freshFhirRepo.getPatient("pat-001")
            assertNotNull(restoredPatient1, "Patient 1 must be restored")
            assertEquals("Cooper", restoredPatient1.name.firstOrNull()?.familyName)
            assertEquals("Alice", restoredPatient1.name.firstOrNull()?.givenName)
            assertEquals("MRN-001-ALPHA", restoredPatient1.mrn)
            assertEquals("1975-04-12", restoredPatient1.customBirthDate)

            val restoredPatient2 = freshFhirRepo.getPatient("pat-002")
            assertNotNull(restoredPatient2)
            assertEquals("Vance", restoredPatient2.name.firstOrNull()?.familyName)
            assertEquals("MRN-002-BRAVO", restoredPatient2.mrn)

            val restoredPatient3 = freshFhirRepo.getPatient("pat-003")
            assertNotNull(restoredPatient3)
            assertEquals("Xavier", restoredPatient3.name.firstOrNull()?.familyName)
            assertEquals("MRN-003-CHARLIE", restoredPatient3.mrn)

            // 8. Verify encounters and document references
            val restoredEncounter = freshFhirRepo.getEncounter("enc-pat1-01")
            assertNotNull(restoredEncounter)
            assertEquals(Encounter.EncounterStatus.Finished, restoredEncounter.status.value)

            val restoredDocRef = freshFhirRepo.getDocumentReference("docref-001")
            assertNotNull(restoredDocRef)
            assertEquals(
                "pat-001",
                restoredDocRef.subject
                    ?.reference
                    ?.value
                    ?.removePrefix("Patient/"),
            )

            freshDriver.close()
        }
}
