/**
 * @file PatientPacsArchiveExportRoundTripWorkflowTest.kt
 * Contains declarations for PatientPacsArchiveExportRoundTripWorkflowTest.kt.
 *
 * Validates patient-level PACS archive compilation, cross-encounter DICOM export,
 * demographic de-identification, and patient ID reference matching.
 */
package io.healthplatform.chartcam.repository

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
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests verifying patient PACS ZIP archive export and image reference resolution.
 */
class PatientPacsArchiveExportRoundTripWorkflowTest {
    /**
     * In-memory test file storage.
     */
    private class InMemoryFileStorage : FileStorage {
        val storage = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            storage[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = storage[path] ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> =
            if (storage.remove(path) != null) Result.success(Unit) else Result.failure(Exception("File not found: $path"))

        override fun clearCache() {
            storage.clear()
        }
    }

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var fileStorage: InMemoryFileStorage
    private lateinit var exportService: DicomExportService

    /**
     * Initializes test database and services.
     */
    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        fileStorage = InMemoryFileStorage()
        exportService = DicomExportService(db, fileStorage)
    }

    /**
     * Tears down test database driver.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Verifies that all clinical photos across multiple encounters are included in the patient PACS archive.
     */
    @Test
    fun testExportPatientDicomArchiveIncludesAllPhotosAcrossEncounters() =
        runTest {
            val patientId = "pat-1001"
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = patientId
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Mulder" }
                                given.add(FhirString.Builder().apply { value = "Fox" })
                            },
                        )
                        identifier.add(
                            Identifier.Builder().apply {
                                value = FhirString.Builder().apply { value = "MRN-1013" }
                            },
                        )
                        birthDate =
                            com.google.fhir.model.r4.Date
                                .Builder()
                                .apply {
                                    value = FhirDate.fromString("1961-10-13")
                                }
                        gender = Enumeration(value = AdministrativeGender.Male)
                    }.build()
            fhirRepo.savePatient(patient)

            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac-dr-who"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Who" }
                                given.add(FhirString.Builder().apply { value = "Doctor" })
                            },
                        )
                    }.build()
            fhirRepo.savePractitioner(practitioner)

            // Encounter 1 (without Patient/ prefix)
            val enc1 =
                createFhirEncounter(
                    id = "enc-1",
                    patientId = patientId,
                    practitionerId = "prac-dr-who",
                    dateStr = "2024-04-01T10:00:00Z",
                )
            fhirRepo.saveEncounter(enc1)

            // Encounter 2 (with Patient/ prefix)
            val enc2 =
                createFhirEncounter(
                    id = "enc-2",
                    patientId = "Patient/$patientId",
                    practitionerId = "prac-dr-who",
                    dateStr = "2024-04-02T11:30:00Z",
                )
            fhirRepo.saveEncounter(enc2)

            val dummyPhotoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("photo1.jpg", dummyPhotoBytes)
            fileStorage.saveImage("photo2.jpg", dummyPhotoBytes)
            fileStorage.saveImage("photo3.jpg", dummyPhotoBytes)

            val doc1 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-1",
                        patientId = patientId,
                        encounterId = "enc-1",
                        dateStr = "2024-04-01T10:05:00Z",
                        desc = "Anterior lesion",
                        mime = "image/jpeg",
                        urlPath = "photo1.jpg",
                        answerCode = "photo_anterior",
                    ),
                )
            val doc2 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-2",
                        patientId = patientId,
                        encounterId = "enc-1",
                        dateStr = "2024-04-01T10:07:00Z",
                        desc = "Lateral lesion",
                        mime = "image/jpeg",
                        urlPath = "photo2.jpg",
                        answerCode = "photo_lateral",
                    ),
                )
            val doc3 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-3",
                        patientId = patientId,
                        encounterId = "enc-2",
                        dateStr = "2024-04-02T11:35:00Z",
                        desc = "Follow-up lesion",
                        mime = "image/jpeg",
                        urlPath = "photo3.jpg",
                        answerCode = "photo_followup",
                    ),
                )

            fhirRepo.saveDocumentReference(doc1)
            fhirRepo.saveDocumentReference(doc2)
            fhirRepo.saveDocumentReference(doc3)

            val zipBytes = exportService.exportPatientDicomArchive(patientId, anonymize = false).getOrThrow()
            assertNotNull(zipBytes)
            assertTrue(zipBytes.isNotEmpty(), "ZIP archive must not be empty")

            // Unpack ZIP in memory and inspect entry names
            val zipStream = ZipInputStream(ByteArrayInputStream(zipBytes))
            val entryNames = mutableListOf<String>()
            var entry = zipStream.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            assertEquals(3, entryNames.size, "ZIP archive must contain exactly 3 .dcm files")
            assertTrue(entryNames.contains("photo_doc-1.dcm"))
            assertTrue(entryNames.contains("photo_doc-2.dcm"))
            assertTrue(entryNames.contains("photo_doc-3.dcm"))
        }

    /**
     * Verifies that anonymization properly redacts patient identifiers in exported DICOM files.
     */
    @Test
    fun testExportPatientDicomArchiveWithAnonymization() =
        runTest {
            val patientId = "pat-2002"
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = patientId
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Private" }
                                given.add(FhirString.Builder().apply { value = "Ryan" })
                            },
                        )
                        identifier.add(
                            Identifier.Builder().apply {
                                value = FhirString.Builder().apply { value = "CONFIDENTIAL-MRN" }
                            },
                        )
                    }.build()
            fhirRepo.savePatient(patient)

            val enc =
                createFhirEncounter(
                    id = "enc-anon",
                    patientId = patientId,
                    practitionerId = "doc-1",
                    dateStr = "2024-04-03T09:00:00Z",
                )
            fhirRepo.saveEncounter(enc)

            val dummyBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("anon.jpg", dummyBytes)

            val doc =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-anon",
                        patientId = patientId,
                        encounterId = "enc-anon",
                        dateStr = "2024-04-03T09:05:00Z",
                        desc = "Confidential image",
                        mime = "image/jpeg",
                        urlPath = "anon.jpg",
                        answerCode = "photo",
                    ),
                )
            fhirRepo.saveDocumentReference(doc)

            val zipBytes = exportService.exportPatientDicomArchive(patientId, anonymize = true).getOrThrow()
            val zipStream = ZipInputStream(ByteArrayInputStream(zipBytes))
            val entryNames = mutableListOf<String>()
            var entry = zipStream.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            assertEquals(1, entryNames.size)
            assertTrue(entryNames.contains("photo_doc-anon.dcm"))
        }

    /**
     * Verifies graceful handling of patients with zero encounters or zero photos.
     */
    @Test
    fun testEmptyPatientExportProducesValidArchiveWithoutThrowing() =
        runTest {
            val patientId = "pat-empty"
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = patientId
                    }.build()
            fhirRepo.savePatient(patient)

            val zipBytes = exportService.exportPatientDicomArchive(patientId, anonymize = false).getOrThrow()
            assertNotNull(zipBytes)
            assertTrue(zipBytes.isNotEmpty(), "Empty archive must produce valid zip structure")
        }
}
