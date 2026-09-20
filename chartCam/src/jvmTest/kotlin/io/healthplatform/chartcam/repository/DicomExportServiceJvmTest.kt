/**
 * @file DicomExportServiceJvmTest.kt
 * Integration tests for DicomExportService covering photo and PDF export and ZIP archive generation.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.String
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
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
     * Seeds a standard patient, practitioner, encounter, and document reference.
     */
    private suspend fun seedPatientAndEncounter(): Triple<Patient, Encounter, DocumentReference> {
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
                        dev.ohs.fhir.model.r4.Date
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
        return Triple(patient, encounter, docRef)
    }

    /**
     * Tests exportPhotoAsDicom with populated records.
     */
    @Test
    fun testExportPhotoAsDicomSuccess(): Unit =
        runBlocking {
            val (_, encounter, docRef) = seedPatientAndEncounter()
            val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

            // Export photo
            val dicomBytes = service.exportPhotoAsDicom("doc-1", anonymize = false).getOrThrow()
            assertNotNull(dicomBytes)
            val dcmStr = dicomBytes.decodeToString()
            assertTrue(dcmStr.contains("DICM"))
            assertTrue(dcmStr.contains(DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE))
            assertTrue(dcmStr.contains("Adams^John"))

            // Export photo anonymized
            val anonBytes = service.exportPhotoAsDicom("doc-1", anonymize = true).getOrThrow()
            assertNotNull(anonBytes)
            val anonStr = anonBytes.decodeToString()
            assertTrue(anonStr.contains("ANONYMOUS^PATIENT"))

            // Non-existent document reference
            assertNull(service.exportPhotoAsDicom("doc-999").getOrThrow())

            // Document reference with empty photo file
            val emptyDocRef = docRef.toBuilder().apply { id = "doc-2" }.build()
            fhirRepo.saveDocumentReference(emptyDocRef)
            fileStorage.files.remove("photo1.jpg")
            assertNull(service.exportPhotoAsDicom("doc-2").getOrThrow())

            // Document reference with empty content
            val emptyContentDoc =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-empty-content"
                        content.clear()
                    }.build()
            fhirRepo.saveDocumentReference(emptyContentDoc)
            assertNull(service.exportPhotoAsDicom("doc-empty-content").getOrThrow())

            // Document reference without encounter in context
            fileStorage.saveImage("photo1.jpg", photoBytes)
            val noEncDoc =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-no-enc"
                        context = null
                    }.build()
            fhirRepo.saveDocumentReference(noEncDoc)
            assertNotNull(service.exportPhotoAsDicom("doc-no-enc").getOrThrow())

            // Document reference without subject
            val noSubjDoc =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-no-subj"
                        subject = null
                    }.build()
            fhirRepo.saveDocumentReference(noSubjDoc)
            assertNotNull(service.exportPhotoAsDicom("doc-no-subj").getOrThrow())

            // Encounter without participant
            val noPartEnc =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-no-part"
                        participant.clear()
                    }.build()
            fhirRepo.saveEncounter(noPartEnc)
            val docWithNoPartEnc =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-no-part",
                        patientId = "Patient/pat-1",
                        encounterId = "Encounter/enc-no-part",
                        dateStr = "2026-09-11T12:00:00Z",
                        desc = "No Part Photo",
                        mime = "image/jpeg",
                        urlPath = "photo1.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(docWithNoPartEnc)
            assertNotNull(service.exportPhotoAsDicom("doc-no-part").getOrThrow())
        }

    /**
     * Tests exportEncounterReportAsDicomPdf.
     */
    @Test
    fun testExportEncounterReportAsDicomPdf(): Unit =
        runBlocking {
            val (_, encounter, _) = seedPatientAndEncounter()
            val pdfData = "%PDF-1.4 report payload".encodeToByteArray()
            val dcmBytes = service.exportEncounterReportAsDicomPdf("enc-100", pdfData, "Clinical Note", anonymize = false).getOrThrow()
            val dcmStr = dcmBytes.decodeToString()
            assertTrue(dcmStr.contains("DICM"))
            assertTrue(dcmStr.contains(DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF))
            assertTrue(dcmStr.contains("Clinical Note"))

            // Populated encounter with patient and practitioner
            val popBytes = service.exportEncounterReportAsDicomPdf("enc-1", pdfData, "Populated Report", anonymize = false).getOrThrow()
            val popStr = popBytes.decodeToString()
            assertTrue(popStr.contains("Adams^John"))
            assertTrue(popStr.contains("House^Gregory"))

            // Populated encounter with anonymization
            val anonBytes = service.exportEncounterReportAsDicomPdf("enc-1", pdfData, "Populated Report", anonymize = true).getOrThrow()
            val anonStr = anonBytes.decodeToString()
            assertTrue(anonStr.contains("ANONYMOUS^PATIENT"))

            // Default parameters test
            val defaultParamsBytes = service.exportEncounterReportAsDicomPdf("enc-1", pdfData).getOrThrow()
            assertTrue(defaultParamsBytes.decodeToString().contains("Clinical Encounter Report"))

            // Encounter with subject having null reference
            val encNullSubjRef =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-null-subj-ref"
                        subject =
                            dev.ohs.fhir.model.r4.Reference
                                .Builder()
                                .apply { reference = null }
                    }.build()
            fhirRepo.saveEncounter(encNullSubjRef)
            assertNotNull(service.exportEncounterReportAsDicomPdf("enc-null-subj-ref", pdfData).getOrThrow())

            // Encounter with participant individual having null reference
            val encNullPartRef =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-null-part-ref"
                        participant[0].individual =
                            dev.ohs.fhir.model.r4.Reference
                                .Builder()
                                .apply { reference = null }
                    }.build()
            fhirRepo.saveEncounter(encNullPartRef)
            assertNotNull(service.exportEncounterReportAsDicomPdf("enc-null-part-ref", pdfData).getOrThrow())

            // Encounter with participant having null individual
            val encNullInd =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-null-ind"
                        participant[0].individual = null
                    }.build()
            fhirRepo.saveEncounter(encNullInd)
            assertNotNull(service.exportEncounterReportAsDicomPdf("enc-null-ind", pdfData).getOrThrow())

            // Encounter with null subject
            val encNullSubj =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-null-subj"
                        subject = null
                    }.build()
            fhirRepo.saveEncounter(encNullSubj)
            assertNotNull(service.exportEncounterReportAsDicomPdf("enc-null-subj", pdfData).getOrThrow())

            // Encounter with empty participants
            val encNoPart =
                encounter
                    .toBuilder()
                    .apply {
                        id = "enc-no-part"
                        participant.clear()
                    }.build()
            fhirRepo.saveEncounter(encNoPart)
            assertNotNull(service.exportEncounterReportAsDicomPdf("enc-no-part", pdfData).getOrThrow())
        }

    /**
     * Tests exportPatientDicomArchive and ZIP archive generation.
     */
    @Test
    fun testExportPatientDicomArchive(): Unit =
        runBlocking {
            val (_, encounter, docRef) = seedPatientAndEncounter()

            // Encounter with null ID
            val encNullId =
                encounter
                    .toBuilder()
                    .apply {
                        id = null
                    }.build()
            fhirRepo.saveEncounter(encNullId)

            val emptyZip = service.exportPatientDicomArchive("pat-empty", anonymize = false).getOrThrow()
            assertTrue(emptyZip.size >= 22) // Valid empty ZIP has at least 22 bytes (End of Central Directory)

            // Patient with populated encounters and photos
            val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            fileStorage.saveImage("photo1.jpg", photoBytes)

            // Add a document reference without ID and one with missing photo file
            val docNoId = docRef.toBuilder().apply { id = null }.build()
            fhirRepo.saveDocumentReference(docNoId)

            val docMissingPhoto =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-missing-photo"
                        content[0].attachment.url =
                            dev.ohs.fhir.model.r4.Url
                                .Builder()
                                .apply { value = "nonexistent.jpg" }
                    }.build()
            fhirRepo.saveDocumentReference(docMissingPhoto)

            // Document with null URL in attachment
            val docNullUrl =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-null-url"
                        content[0].attachment.url = null
                    }.build()
            fhirRepo.saveDocumentReference(docNullUrl)
            assertNull(service.exportPhotoAsDicom("doc-null-url").getOrThrow())

            // Document with non-null URL but null value
            val docNullUrlVal =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-null-url-val"
                        content[0].attachment.url =
                            dev.ohs.fhir.model.r4.Url
                                .Builder()
                                .apply { value = null }
                    }.build()
            fhirRepo.saveDocumentReference(docNullUrlVal)
            assertNull(service.exportPhotoAsDicom("doc-null-url-val").getOrThrow())

            // Document with subject but null reference
            val docNullRef =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-null-ref"
                        subject =
                            dev.ohs.fhir.model.r4.Reference
                                .Builder()
                                .apply { reference = null }
                    }.build()
            fhirRepo.saveDocumentReference(docNullRef)
            assertNotNull(service.exportPhotoAsDicom("doc-null-ref").getOrThrow())

            // Document with null encounter reference and empty encounter list
            val docNullEncRef =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-null-enc-ref"
                        context =
                            DocumentReference
                                .Context(
                                    encounter =
                                        listOf(
                                            dev.ohs.fhir.model.r4
                                                .Reference(reference = null),
                                        ),
                                ).toBuilder()
                    }.build()
            fhirRepo.saveDocumentReference(docNullEncRef)
            assertNotNull(service.exportPhotoAsDicom("doc-null-enc-ref").getOrThrow())

            val docEmptyEncList =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-empty-enc-list"
                        context = DocumentReference.Context(encounter = emptyList()).toBuilder()
                    }.build()
            fhirRepo.saveDocumentReference(docEmptyEncList)
            assertNotNull(service.exportPhotoAsDicom("doc-empty-enc-list").getOrThrow())

            // Document for another encounter and document with no encounter
            val otherEncDoc =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-other-enc",
                        patientId = "Patient/pat-other",
                        encounterId = "Encounter/other-enc",
                        dateStr = "2026-09-11T12:00:00Z",
                        desc = "Other Encounter Photo",
                        mime = "image/jpeg",
                        urlPath = "photo1.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(otherEncDoc)

            val noEncDocArchive =
                docRef
                    .toBuilder()
                    .apply {
                        id = "doc-no-enc-archive"
                        context = null
                    }.build()
            fhirRepo.saveDocumentReference(noEncDocArchive)

            val populatedZip = service.exportPatientDicomArchive("pat-1").getOrThrow()
            assertTrue(populatedZip.size > 100)
            val zipStr = populatedZip.decodeToString()
            assertTrue(zipStr.contains("photo_doc-1.dcm"))
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
