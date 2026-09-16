/**
 * @file DicomEncapsulationE2ETest.kt
 * Contains declarations for DicomEncapsulationE2ETest.kt.
 *
 * Validates binary DICOM dataset generation, preamble, file meta headers,
 * and clinical tag mapping from FHIR resources.
 */
package io.healthplatform.chartcam.dicom

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.terminologies.AdministrativeGender
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.repository.DicomExportService
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End verification of the FHIR-to-DICOM translation and Part 10 binary encapsulation pipeline.
 */
class DicomEncapsulationE2ETest {
    /**
     * Minimal in-memory file storage mock.
     */
    private class MemoryFileStorage : FileStorage {
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

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var fileStorage: MemoryFileStorage
    private lateinit var exportService: DicomExportService

    /**
     * Initializes test database and mock services.
     */
    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        fileStorage = MemoryFileStorage()
        exportService = DicomExportService(db, fileStorage)
    }

    /**
     * Tears down test database connection.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests end-to-end DICOM Part 10 binary encoding, headers, and Patient module tags.
     */
    @Test
    fun testDicomPart10BinaryGenerationAndTagMapping() =
        runTest {
            // 1. Seed clinical patient with MRN, name, gender, birth date
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat-dicom-01"
                        identifier.add(
                            Identifier
                                .Builder()
                                .apply {
                                    value = FhirString.Builder().apply { value = "MRN-DICOM-7788" }
                                },
                        )
                        name.add(
                            HumanName
                                .Builder()
                                .apply {
                                    family = FhirString.Builder().apply { value = "Franklin" }
                                    given.add(FhirString.Builder().apply { value = "Rosalind" })
                                },
                        )
                        gender = Enumeration(value = AdministrativeGender.Female)
                        birthDate =
                            com.google.fhir.model.r4.Date
                                .Builder()
                                .apply {
                                    value = FhirDate.fromString("1920-07-25")
                                }
                    }.build()
            fhirRepo.savePatient(patient)

            // Seed practitioner
            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac-dicom-01"
                        name.add(
                            HumanName
                                .Builder()
                                .apply {
                                    family = FhirString.Builder().apply { value = "Watson" }
                                    given.add(FhirString.Builder().apply { value = "James" })
                                },
                        )
                    }.build()
            fhirRepo.savePractitioner(practitioner)

            // Seed encounter
            val encounter =
                createFhirEncounter(
                    id = "enc-dicom-01",
                    patientId = "Patient/pat-dicom-01",
                    practitionerId = "Practitioner/prac-dicom-01",
                    dateStr = "2026-09-14T09:30:00Z",
                )
            fhirRepo.saveEncounter(encounter)

            // Seed clinical photo payload (JPEG markers)
            val jpegPhoto = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xAA.toByte(), 0xBB.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("rosalind_photo.jpg", jpegPhoto)

            val docRef =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-dicom-01",
                        patientId = "Patient/pat-dicom-01",
                        encounterId = "Encounter/enc-dicom-01",
                        dateStr = "2026-09-14T09:30:00Z",
                        desc = "Crystallography / Lesion Finding",
                        mime = "image/jpeg",
                        urlPath = "rosalind_photo.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(docRef)

            // 2. Generate DICOM Part 10 binary stream
            val dicomBytes = exportService.exportPhotoAsDicom("doc-dicom-01", anonymize = false).getOrThrow()
            assertNotNull(dicomBytes, "DICOM output stream must not be null")
            assertTrue(dicomBytes.size > 132, "DICOM Part 10 file must be larger than preamble + magic header")

            // 3. Validate 128-byte preamble and "DICM" magic bytes
            for (i in 0 until 128) {
                assertEquals(0.toByte(), dicomBytes[i], "Preamble byte $i must be zero")
            }
            val magicHeader = dicomBytes.sliceArray(128 until 132).decodeToString()
            assertEquals("DICM", magicHeader, "Magic header at offset 128 must be 'DICM'")

            // 4. Validate metadata content via string decoding
            val dicomStr = dicomBytes.decodeToString()

            // File Meta Information
            assertTrue(
                dicomStr.contains(DicomTag.UID_JPEG_BASELINE),
                "Must contain JPEG Baseline Transfer Syntax UID",
            )
            assertTrue(dicomStr.contains(DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE), "Must contain VL Photographic Image SOP Class UID")

            // Patient Module
            assertTrue(dicomStr.contains("Franklin^Rosalind"), "Patient's Name tag (0010,0010) must match FHIR Patient name")
            assertTrue(dicomStr.contains("MRN-DICOM-7788"), "Patient ID tag (0010,0020) must match FHIR Patient MRN")
            assertTrue(dicomStr.contains("19200725"), "Patient's Birth Date tag (0010,0030) must be formatted as YYYYMMDD")
            assertTrue(dicomStr.contains("F"), "Patient's Sex tag (0010,0040) must map to 'F'")

            // Verify image payload is encapsulated
            var foundPayload = false
            for (i in 0..(dicomBytes.size - jpegPhoto.size)) {
                if (dicomBytes.sliceArray(i until (i + jpegPhoto.size)).contentEquals(jpegPhoto)) {
                    foundPayload = true
                    break
                }
            }
            assertTrue(foundPayload, "Pixel Data / Encapsulated photo byte payload must remain intact in DICOM file")
        }
}
