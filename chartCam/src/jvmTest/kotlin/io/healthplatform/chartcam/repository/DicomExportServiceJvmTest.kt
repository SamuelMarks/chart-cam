/**
 * @file DicomExportServiceJvmTest.kt
 * Integration tests for DicomExportService covering photo and PDF export and ZIP archive generation.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.AdministrativeGender
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.dicom.DicomTag
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM integration tests for DicomExportService.
 */
class DicomExportServiceJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var fhirRepo: FhirRepository
    private lateinit var fileStorage: TestMemoryFileStorage
    private lateinit var service: DicomExportService

    /**
     * In-memory test file storage.
     */
    private class TestMemoryFileStorage : FileStorage {
        val files = mutableMapOf<kotlin.String, ByteArray>()

        override fun saveImage(
            fileName: kotlin.String,
            bytes: ByteArray,
        ): kotlin.String {
            files[fileName] = bytes
            return fileName
        }

        override fun readImage(path: kotlin.String): ByteArray = files[path] ?: ByteArray(0)

        override fun deleteImage(path: kotlin.String): Result<Unit> =
            if (files.remove(path) != null) Result.success(Unit) else Result.failure(Exception("File not found: $path"))

        override fun clearCache() {
            files.clear()
        }
    }

    /**
     * Initializes in-memory SQLite database and test fakes.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        fileStorage = TestMemoryFileStorage()
        service = DicomExportService(db, fileStorage)
    }

    /**
     * Closes driver.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests exportPhotoAsDicom with populated records.
     */
    @Test
    fun testExportPhotoAsDicomSuccess(): Unit =
        runBlocking {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat-1"
                        name.add(
                            HumanName
                                .Builder()
                                .apply {
                                    family = String.Builder().apply { value = "Adams" }
                                    given.add(String.Builder().apply { value = "John" })
                                },
                        )
                        gender = Enumeration(value = AdministrativeGender.Male)
                        birthDate =
                            com.google.fhir.model.r4.Date
                                .Builder()
                                .apply {
                                    value = FhirDate.fromString("1980-01-01")
                                }
                    }.build()
            fhirRepo.savePatient(patient)

            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac-1"
                        name.add(
                            HumanName
                                .Builder()
                                .apply {
                                    family = String.Builder().apply { value = "House" }
                                    given.add(String.Builder().apply { value = "Gregory" })
                                },
                        )
                    }.build()
            fhirRepo.savePractitioner(practitioner)

            val encounter =
                createFhirEncounter(
                    id = "enc-1",
                    patientId = "Patient/pat-1",
                    practitionerId = "Practitioner/prac-1",
                    dateStr = "2026-09-11T12:00:00Z",
                )
            fhirRepo.saveEncounter(encounter)

            val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("photo1.jpg", photoBytes)

            val docRef =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-1",
                        patientId = "Patient/pat-1",
                        encounterId = "Encounter/enc-1",
                        dateStr = "2026-09-11T12:00:00Z",
                        desc = "Clinical Photo",
                        mime = "image/jpeg",
                        urlPath = "photo1.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(docRef)

            // Export photo
            val dicomBytes = service.exportPhotoAsDicom("doc-1", anonymize = false)
            assertNotNull(dicomBytes)
            val dcmStr = dicomBytes.decodeToString()
            assertTrue(dcmStr.contains("DICM"))
            assertTrue(dcmStr.contains(DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE))
            assertTrue(dcmStr.contains("Adams^John"))

            // Export photo anonymized
            val anonBytes = service.exportPhotoAsDicom("doc-1", anonymize = true)
            assertNotNull(anonBytes)
            val anonStr = anonBytes.decodeToString()
            assertTrue(anonStr.contains("ANONYMOUS^PATIENT"))

            // Non-existent document reference
            assertNull(service.exportPhotoAsDicom("doc-999"))

            // Document reference with empty photo file
            val emptyDocRef = docRef.toBuilder().apply { id = "doc-2" }.build()
            fhirRepo.saveDocumentReference(emptyDocRef)
            fileStorage.files.remove("photo1.jpg")
            assertNull(service.exportPhotoAsDicom("doc-2"))
        }

    /**
     * Tests exportEncounterReportAsDicomPdf.
     */
    @Test
    fun testExportEncounterReportAsDicomPdf(): Unit =
        runBlocking {
            val pdfData = "%PDF-1.4 report payload".encodeToByteArray()
            val dcmBytes = service.exportEncounterReportAsDicomPdf("enc-100", pdfData, "Clinical Note", anonymize = false)
            val dcmStr = dcmBytes.decodeToString()
            assertTrue(dcmStr.contains("DICM"))
            assertTrue(dcmStr.contains(DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF))
            assertTrue(dcmStr.contains("Clinical Note"))
        }

    /**
     * Tests exportPatientDicomArchive and ZIP archive generation.
     */
    @Test
    fun testExportPatientDicomArchive(): Unit =
        runBlocking {
            val zipData = service.exportPatientDicomArchive("pat-empty", anonymize = false)
            assertTrue(zipData.size >= 22) // Valid empty ZIP has at least 22 bytes (End of Central Directory)
        }

    /**
     * Tests CRC-32 computation against standard test vector.
     */
    @Test
    fun testCrc32() {
        // Standard CRC32 check for "123456789" is 0xCBF43926L (3421780262)
        val input = "123456789".encodeToByteArray()
        val crc = service.computeCrc32(input)
        assertEquals(0xCBF43926L, crc)
    }
}
