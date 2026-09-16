/**
 * @file DicomTimestampAndPacsConformanceWorkflowTest.kt
 * Contains declarations for DicomTimestampAndPacsConformanceWorkflowTest.kt.
 *
 * Validates DICOM PS 3.5 timestamp formatting conformance (DA and TM VRs),
 * ISO-8601 period datetime sanitization, timezone stripping, and PACS binary integrity.
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
 * End-to-End workflow tests verifying DICOM timestamp compliance and PACS ingestion standards.
 */
class DicomTimestampAndPacsConformanceWorkflowTest {
    /**
     * Minimal in-memory file storage double.
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
     * Prepares in-memory test database and services before each test.
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
     * Closes the database driver after test completion.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Verifies that ISO-8601 encounter period datetimes produce valid DICOM DA (YYYYMMDD) and TM (HHMMSS) tags.
     */
    @Test
    fun testIsoPeriodDatetimeProducesCompliantDicomTimestamps() =
        runTest {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "p-1234"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Skinner" }
                                given.add(FhirString.Builder().apply { value = "Walter" })
                            },
                        )
                        identifier.add(
                            Identifier.Builder().apply {
                                value = FhirString.Builder().apply { value = "MRN-1002" }
                            },
                        )
                        birthDate =
                            com.google.fhir.model.r4.Date
                                .Builder()
                                .apply {
                                    value = FhirDate.fromString("1965-05-12")
                                }
                        gender = Enumeration(value = AdministrativeGender.Male)
                    }.build()
            fhirRepo.savePatient(patient)

            val doc =
                Practitioner
                    .Builder()
                    .apply {
                        id = "doc-99"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Scully" }
                                given.add(FhirString.Builder().apply { value = "Dana" })
                            },
                        )
                    }.build()
            fhirRepo.savePractitioner(doc)

            val encounterIsoDate = "2024-03-15T09:30:00Z"
            val encounter =
                createFhirEncounter(
                    id = "enc-101",
                    patientId = "p-1234",
                    practitionerId = "doc-99",
                    dateStr = encounterIsoDate,
                )
            fhirRepo.saveEncounter(encounter)

            val dummyImage = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("lesion_capture.jpg", dummyImage)

            val docRef =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "docref-101",
                        patientId = "p-1234",
                        encounterId = "enc-101",
                        dateStr = encounterIsoDate,
                        desc = "Skin lesion close up",
                        mime = "image/jpeg",
                        urlPath = "lesion_capture.jpg",
                        answerCode = "lesion_photo",
                    ),
                )
            fhirRepo.saveDocumentReference(docRef)

            val dicomBytes = exportService.exportPhotoAsDicom("docref-101", anonymize = false).getOrThrow()
            assertNotNull(dicomBytes, "DICOM output bytes must not be null")
            assertTrue(dicomBytes.size > 132, "DICOM file must exceed preamble length")

            // Preamble check
            val magic = dicomBytes.copyOfRange(128, 132).decodeToString()
            assertEquals("DICM", magic)

            // Direct DicomElement check for format correctness
            val studyDateElem = DicomElement.createDate(DicomTag.STUDY_DATE, encounterIsoDate)
            assertEquals("20240315", studyDateElem.value.decodeToString().trim())

            val studyTimeElem = DicomElement.createTime(DicomTag.STUDY_TIME, encounterIsoDate)
            assertEquals("093000", studyTimeElem.value.decodeToString().trim())
        }

    /**
     * Verifies that millisecond precision and timezone offsets are correctly sanitized to 6-digit HHMMSS.
     */
    @Test
    fun testMillisecondAndTimezoneSanitization() =
        runTest {
            val tzIsoDate = "2024-03-15T14:22:05.123+02:00"
            val timeElem = DicomElement.createTime(DicomTag.STUDY_TIME, tzIsoDate)
            assertEquals("142205", timeElem.value.decodeToString().trim())

            val dateElem = DicomElement.createDate(DicomTag.STUDY_DATE, tzIsoDate)
            assertEquals("20240315", dateElem.value.decodeToString().trim())
        }

    /**
     * Verifies fallback behavior when period start timestamp is missing or empty.
     */
    @Test
    fun testFallbackTimestampOnEmptyOrNullPeriod() =
        runTest {
            val timeElem = DicomElement.createTime(DicomTag.CONTENT_TIME, "")
            val timeValue = timeElem.value.decodeToString().trim()
            assertEquals(6, timeValue.length, "Time tag must be exactly 6 characters (HHMMSS)")
            assertEquals("000000", timeValue)

            val dateElem = DicomElement.createDate(DicomTag.CONTENT_DATE, "")
            val dateValue = dateElem.value.decodeToString().trim()
            assertTrue(dateValue.isEmpty() || dateValue.length <= 8)
        }
}
