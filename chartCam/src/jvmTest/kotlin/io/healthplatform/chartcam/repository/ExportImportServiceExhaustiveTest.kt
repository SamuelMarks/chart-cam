/**
 * @file ExportImportServiceExhaustiveTest.kt
 * Exhaustive unit tests for ExportImportService targeting 100% coverage.
 */

package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Exhaustive unit tests covering all execution paths in [ExportImportService].
 */
class ExportImportServiceExhaustiveTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var fileStorage: TestMemoryFileStorage
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirRepo: FhirRepository
    private lateinit var service: ExportImportService

    /**
     * Set up in-memory database and test file storage.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fileStorage = TestMemoryFileStorage()
        cryptoService = CryptoService()
        fhirRepo = FhirRepository(db)
        service = ExportImportService(db, fileStorage)
    }

    /**
     * Clean up driver.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests executeWithTemporaryCleanup under successful, export failure, and block failure conditions.
     */
    @Test
    fun testExecuteWithTemporaryCleanupPaths() =
        runTest {
            val patient = createFhirPatient("p-clean", "Alice", "Smith", LocalDate(1985, 5, 20), "MRN-CLEAN")
            fhirRepo.savePatient(patient)

            // Success case
            var cleanupExecuted = false
            val successResult =
                service.executeWithTemporaryCleanup("validPassword123") { data ->
                    cleanupExecuted = true
                    data.length
                }
            assertTrue(successResult.isSuccess)
            assertTrue(cleanupExecuted)
            assertTrue(successResult.getOrThrow() > 0)
            assertTrue(fileStorage.wasCacheCleared)

            // Export failure (weak password)
            fileStorage.wasCacheCleared = false
            val weakPassResult =
                service.executeWithTemporaryCleanup("123") { data ->
                    data.length
                }
            assertTrue(weakPassResult.isFailure)
            assertTrue(fileStorage.wasCacheCleared)

            // Block throws exception
            fileStorage.wasCacheCleared = false
            val blockFailResult =
                service.executeWithTemporaryCleanup("validPassword123") {
                    throw IllegalStateException("Simulated block crash") // allow-exception
                }
            assertTrue(blockFailResult.isFailure)
            assertTrue(fileStorage.wasCacheCleared)
        }

    /**
     * Tests importDataWithCleanup for both success and failure cases.
     */
    @Test
    fun testImportDataWithCleanupPaths() =
        runTest {
            val patient = createFhirPatient("p-imp-clean", "Bob", "Builder", LocalDate(1980, 1, 1), "MRN-IMP")
            fhirRepo.savePatient(patient)
            val exported = service.exportData("validPassword123", exportAll = true).getOrThrow()

            // Success
            fileStorage.wasCacheCleared = false
            val success = service.importDataWithCleanup(exported, "validPassword123")
            assertTrue(success.isSuccess)
            assertTrue(fileStorage.wasCacheCleared)

            // Failure
            fileStorage.wasCacheCleared = false
            val fail = service.importDataWithCleanup(exported, "wrongPassword123")
            assertTrue(fail.isFailure)
            assertTrue(fileStorage.wasCacheCleared)
        }

    /**
     * Tests practitioner-scoped export including matching and non-matching encounters, patients, and documents.
     */
    @Test
    fun testExportFilteredByPractitioner() =
        runTest {
            val prac1 =
                Practitioner(
                    id = "prac-1",
                    active = FhirBoolean(value = true),
                )
            val prac2 =
                Practitioner(
                    id = "prac-2",
                    active = FhirBoolean(value = true),
                )
            fhirRepo.savePractitioner(prac1)
            fhirRepo.savePractitioner(prac2)
            fhirRepo.saveDevice(Device(id = "dev-export"))

            val p1 =
                createFhirPatient("p-prac1", "John", "Doe", LocalDate(1990, 1, 1), "MRN-1")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Practitioner/prac-1" }
                            }
                    }.build()
            val p2 =
                createFhirPatient("p-prac2", "Jane", "Roe", LocalDate(1992, 2, 2), "MRN-2")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Practitioner/prac-2" }
                            }
                    }.build()
            fhirRepo.savePatient(p1)
            fhirRepo.savePatient(p2)

            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val enc1 =
                Encounter(
                    id = "enc-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-prac1")),
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "Practitioner/prac-1")),
                            ),
                        ),
                )
            val enc2 =
                Encounter(
                    id = "enc-2",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-prac2")),
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "Practitioner/prac-2")),
                            ),
                        ),
                )
            fhirRepo.saveEncounter(enc1)
            fhirRepo.saveEncounter(enc2)

            // DocumentReference for enc1
            fileStorage.saveImage("test-img.jpg", byteArrayOf(1, 2, 3, 4))
            val doc1 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-1",
                        patientId = "p-prac1",
                        encounterId = "enc-1",
                        dateStr = "2026-09-18T10:00:00Z",
                        desc = "Image 1",
                        mime = "image/jpeg",
                        urlPath = "test-img.jpg",
                    ),
                )
            // DocumentReference for enc2
            val doc2 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-2",
                        patientId = "p-prac2",
                        encounterId = "enc-2",
                        dateStr = "2026-09-18T10:00:00Z",
                        desc = "Image 2",
                        mime = "image/jpeg",
                        urlPath = "missing.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(doc1)
            fhirRepo.saveDocumentReference(doc2)

            // QuestionnaireResponse for enc1 and enc2
            val qr1 =
                QuestionnaireResponse(
                    id = "qr-1",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-1")),
                    subject = Reference(reference = FhirString(value = "Patient/p-prac1")),
                )
            val qr2 =
                QuestionnaireResponse(
                    id = "qr-2",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-2")),
                    subject = Reference(reference = FhirString(value = "Patient/p-prac2")),
                )
            fhirRepo.saveQuestionnaireResponse(qr1)
            fhirRepo.saveQuestionnaireResponse(qr2)

            // Edge cases for null references and empty contexts during practitioner export
            val docNoContext =
                DocumentReference(
                    id = "doc-no-context",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    content = emptyList(),
                )
            val docNullUrl =
                DocumentReference(
                    id = "doc-null-url",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4
                                        .Attachment(url = null),
                            ),
                        ),
                )
            val docNullMime =
                DocumentReference(
                    id = "doc-null-mime",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4.Attachment(
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Url(value = "test-img.jpg"),
                                        contentType = null,
                                    ),
                            ),
                        ),
                )
            val docBlankMime =
                DocumentReference(
                    id = "doc-blank-mime",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4.Attachment(
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Url(value = "test-img.jpg"),
                                        contentType = Code(value = "   "),
                                    ),
                            ),
                        ),
                )
            val docBlankUrl =
                DocumentReference(
                    id = "doc-blank-url",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4.Attachment(
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Url(value = "   "),
                                    ),
                            ),
                        ),
                )
            val docEmptyContent =
                DocumentReference(
                    id = "doc-empty-content",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content = emptyList(),
                )
            val docCrashRead =
                DocumentReference(
                    id = "doc-crash-read",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-1")))),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4.Attachment(
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Url(value = "crash-read.jpg"),
                                    ),
                            ),
                        ),
                )
            val docEmptyEncRef =
                DocumentReference(
                    id = "doc-empty-enc-ref",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context = DocumentReference.Context(encounter = listOf(Reference(reference = null))),
                    content = emptyList(),
                )
            fhirRepo.saveDocumentReference(docNoContext)
            fhirRepo.saveDocumentReference(docNullUrl)
            fhirRepo.saveDocumentReference(docNullMime)
            fhirRepo.saveDocumentReference(docBlankMime)
            fhirRepo.saveDocumentReference(docBlankUrl)
            fhirRepo.saveDocumentReference(docEmptyContent)
            fhirRepo.saveDocumentReference(docCrashRead)
            fhirRepo.saveDocumentReference(docEmptyEncRef)

            val qrNoEnc =
                QuestionnaireResponse(
                    id = "qr-no-enc",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                )
            val qrEmptyEncRef =
                QuestionnaireResponse(
                    id = "qr-empty-enc-ref",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = null),
                )
            fhirRepo.saveQuestionnaireResponse(qrNoEnc)
            fhirRepo.saveQuestionnaireResponse(qrEmptyEncRef)

            val encNoPrac =
                Encounter(
                    id = "enc-no-prac",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                )
            val encNullPracRef =
                Encounter(
                    id = "enc-null-prac-ref",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    participant = listOf(Encounter.Participant(individual = Reference())),
                )
            val encNullValPrac =
                Encounter(
                    id = "enc-null-val-prac",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = null)),
                            ),
                        ),
                )
            val encNullIndividual =
                Encounter(
                    id = "enc-null-indiv",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    participant = listOf(Encounter.Participant(individual = null)),
                )
            val encPrac1NoSubject =
                Encounter(
                    id = "enc-prac1-no-subj",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "Practitioner/prac-1")),
                            ),
                        ),
                )
            val encNullIdExport =
                Encounter(
                    id = null,
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "Practitioner/prac-1")),
                            ),
                        ),
                )
            fhirRepo.saveEncounter(encNoPrac)
            fhirRepo.saveEncounter(encNullPracRef)
            fhirRepo.saveEncounter(encNullValPrac)
            fhirRepo.saveEncounter(encNullIndividual)
            fhirRepo.saveEncounter(encPrac1NoSubject)
            fhirRepo.saveEncounter(encNullIdExport)

            val pOtherOrg =
                createFhirPatient("p-other-org", "Other", "Org", LocalDate(1990, 1, 1), "MRN-ORG")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Organization/org-1" }
                            }
                    }.build()
            val pManagedOnly =
                createFhirPatient("p-managed-only", "Managed", "Only", LocalDate(1993, 3, 3), "MRN-MO")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = "Practitioner/prac-1" }
                            }
                    }.build()
            val pNullValOrg =
                createFhirPatient("p-null-val-org", "NullVal", "Org", LocalDate(1994, 4, 4), "MRN-NVO")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply {
                                reference = FhirString.Builder().apply { value = null }
                            }
                    }.build()
            val pNoOrg = createFhirPatient("p-no-org", "No", "Org", LocalDate(1995, 5, 5), "MRN-NO-ORG")
            val pNullIdExport = Patient(id = null, active = FhirBoolean(value = true))
            fhirRepo.savePatient(pOtherOrg)
            fhirRepo.savePatient(pManagedOnly)
            fhirRepo.savePatient(pNullValOrg)
            fhirRepo.savePatient(pNoOrg)
            fhirRepo.savePatient(pNullIdExport)

            val exportResult = service.exportData("password123", exportAll = false, practitionerId = "prac-1")
            assertTrue(exportResult.isSuccess)

            val decryptedJson = cryptoService.decrypt(exportResult.getOrThrow(), "password123")
            val bundle =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .decodeTypedResource(Bundle.serializer(), decryptedJson)
                    .getOrThrow()

            val entryIds = bundle.entry.mapNotNull { it.resource?.id }
            assertTrue(entryIds.contains("enc-1"))
            assertFalse(entryIds.contains("enc-2"))
            assertTrue(entryIds.contains("p-prac1"))
            assertFalse(entryIds.contains("p-prac2"))
            assertTrue(entryIds.contains("doc-1"))
            assertFalse(entryIds.contains("doc-2"))
            assertTrue(entryIds.contains("qr-1"))
            assertFalse(entryIds.contains("qr-2"))
        }

    /**
     * Tests inspectArchive and buildStagingItem across all conflict types.
     */
    @Test
    fun testInspectArchiveConflictTypes() =
        runTest {
            val localExact = createFhirPatient("p-exact", "Exact", "Match", LocalDate(1990, 1, 1), "MRN-EXACT")
            val localDiff = createFhirPatient("p-diff", "Diff", "Original", LocalDate(1991, 2, 2), "MRN-DIFF")
            val localMrnClash = createFhirPatient("p-mrn-local", "Mrn", "Local", LocalDate(1992, 3, 3), "MRN-SHARED")
            fhirRepo.savePatient(localExact)
            fhirRepo.savePatient(localDiff)
            fhirRepo.savePatient(localMrnClash)

            // Build an incoming bundle with:
            // 1. Exact match (p-exact)
            // 2. ID collision with different data (p-diff with new family name)
            // 3. MRN collision with different ID (p-incoming-mrn with MRN-SHARED)
            // 4. No collision (brand new patient p-brand-new)
            // 5. Patient with null ID and empty MRN
            val incomingExact = localExact
            val incomingDiff = createFhirPatient("p-diff", "Diff", "ChangedData", LocalDate(1991, 2, 2), "MRN-DIFF")
            val incomingMrn = createFhirPatient("p-incoming-mrn", "Mrn", "Incoming", LocalDate(1992, 3, 3), "MRN-SHARED")
            val incomingNew = createFhirPatient("p-brand-new", "Brand", "New", LocalDate(1995, 5, 5), "MRN-NEW")
            val patientNoIdOrMrn = Patient()
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val encNoSubject =
                Encounter(
                    id = "enc-no-subj",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                )
            val docNoSubject =
                DocumentReference(
                    id = "doc-no-subj",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    content = emptyList(),
                    subject = null,
                )
            val qrNoSubject =
                QuestionnaireResponse(
                    id = "qr-no-subj",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    subject = null,
                )

            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(resource = incomingExact),
                            Bundle.Entry(resource = incomingDiff),
                            Bundle.Entry(resource = incomingMrn),
                            Bundle.Entry(resource = incomingNew),
                            Bundle.Entry(resource = patientNoIdOrMrn),
                            Bundle.Entry(resource = encNoSubject),
                            Bundle.Entry(resource = docNoSubject),
                            Bundle.Entry(resource = qrNoSubject),
                            Bundle.Entry(resource = null),
                        ),
                )
            val json =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle)
                    .getOrThrow()
            val encrypted = cryptoService.encrypt(json, "password123")

            val previewResult = service.inspectArchive(encrypted, "password123")
            assertTrue(previewResult.isSuccess)
            val preview = previewResult.getOrThrow()
            assertTrue(preview.hasConflicts)

            // Test archive with no conflicts
            val noConflictBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = listOf(Bundle.Entry(resource = incomingNew)),
                )
            val noConflictJson =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(
                        Bundle.serializer(),
                        noConflictBundle,
                    ).getOrThrow()
            val noConflictEncrypted = cryptoService.encrypt(noConflictJson, "password123")
            val noConflictPreview = service.inspectArchive(noConflictEncrypted, "password123").getOrThrow()
            assertFalse(noConflictPreview.hasConflicts)

            assertEquals(5, preview.stagedPatients.size)
            val exactStaging = preview.stagedPatients.first { it.incomingPatient.id == "p-exact" }
            assertEquals(ConflictType.EXACT_MATCH, exactStaging.conflictType)
            assertEquals(ConflictResolutionStrategy.OVERWRITE_LOCAL, exactStaging.resolutionStrategy)

            val diffStaging = preview.stagedPatients.first { it.incomingPatient.id == "p-diff" }
            assertEquals(ConflictType.ID_COLLISION_DIFFERENT_DATA, diffStaging.conflictType)
            assertEquals(ConflictResolutionStrategy.KEEP_LOCAL, diffStaging.resolutionStrategy)

            val mrnStaging = preview.stagedPatients.first { it.incomingPatient.id == "p-incoming-mrn" }
            assertEquals(ConflictType.MRN_COLLISION_DIFFERENT_ID, mrnStaging.conflictType)

            val newStaging = preview.stagedPatients.first { it.incomingPatient.id == "p-brand-new" }
            assertEquals(ConflictType.EXACT_MATCH, newStaging.conflictType)

            // Import selective on this bundle with MRN clash
            val importMrnClashResult = service.importDataSelective(encrypted, "password123")
            assertTrue(importMrnClashResult.isSuccess)

            // Test password validation edge cases
            assertFalse(service.isValidPassword(""))
            assertFalse(service.isValidPassword("   "))
            assertFalse(service.isValidPassword("12345"))
            assertTrue(service.isValidPassword("123456"))

            // Test exportData with exportAll = false, practitionerId = null
            val exportNullPracResult = service.exportData("password123", exportAll = false, practitionerId = null)
            assertTrue(exportNullPracResult.isSuccess)

            // Inspect archive error branches
            assertTrue(service.inspectArchive(encrypted, "123").isFailure)
            assertTrue(service.importDataSelective(encrypted, "123").isFailure)
            assertTrue(service.inspectArchive(encrypted, "wrongPassword").isFailure)
            assertTrue(service.importDataSelective(encrypted, "wrongPassword").isFailure)
            val malformedEncrypted = cryptoService.encrypt("not a bundle json", "password123")
            assertTrue(service.inspectArchive(malformedEncrypted, "password123").isFailure)
            assertTrue(service.importDataSelective(malformedEncrypted, "password123").isFailure)
        }

    /**
     * Tests importDataSelective with all conflict resolution strategies (KEEP_LOCAL, OVERWRITE_LOCAL, MERGE_RECORDS, CREATE_AS_NEW_ID).
     */
    @Test
    fun testImportDataSelectiveConflictResolution() =
        runTest {
            val localConflict = createFhirPatient("p-conflict", "Local", "Name", LocalDate(1980, 1, 1), "MRN-C")
            fhirRepo.savePatient(localConflict)

            val incomingPatient = createFhirPatient("p-conflict", "Incoming", "Updated", LocalDate(1980, 1, 1), "MRN-C")
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val enc =
                Encounter(
                    id = "enc-c",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-conflict")),
                )
            val doc =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-c",
                        patientId = "p-conflict",
                        encounterId = "enc-c",
                        dateStr = "2026-09-18T10:00:00Z",
                        desc = "Doc",
                        mime = "image/jpeg",
                        urlPath = "img-c.jpg",
                    ),
                )
            val qr =
                QuestionnaireResponse(
                    id = "qr-c",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-c")),
                    subject = Reference(reference = FhirString(value = "Patient/p-conflict")),
                )

            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(resource = incomingPatient),
                            Bundle.Entry(resource = enc),
                            Bundle.Entry(resource = doc),
                            Bundle.Entry(resource = qr),
                        ),
                )
            val json =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle)
                    .getOrThrow()
            val encrypted = cryptoService.encrypt(json, "password123")

            // 1. KEEP_LOCAL with conflict: local patient preserved, incoming enc/doc/qr skipped
            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    resolutionMap = mapOf("p-conflict" to ConflictResolutionStrategy.KEEP_LOCAL),
                ).getOrThrow()

            assertEquals(
                "Name",
                fhirRepo
                    .getPatient("p-conflict")
                    ?.name
                    ?.firstOrNull()
                    ?.family
                    ?.value,
            )
            assertEquals(null, fhirRepo.getEncounter("enc-c"))

            // 2. OVERWRITE_LOCAL: incoming patient saved, encounters imported
            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    resolutionMap = mapOf("p-conflict" to ConflictResolutionStrategy.OVERWRITE_LOCAL),
                ).getOrThrow()

            assertEquals(
                "Updated",
                fhirRepo
                    .getPatient("p-conflict")
                    ?.name
                    ?.firstOrNull()
                    ?.family
                    ?.value,
            )
            assertNotNull(fhirRepo.getEncounter("enc-c"))

            // 3. MERGE_RECORDS: merged demographics
            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    resolutionMap = mapOf("p-conflict" to ConflictResolutionStrategy.MERGE_RECORDS),
                ).getOrThrow()
            assertNotNull(fhirRepo.getPatient("p-conflict"))

            // 4. CREATE_AS_NEW_ID: re-keys patient to new UUID and re-parents encounters/docs/qr
            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    resolutionMap = mapOf("p-conflict" to ConflictResolutionStrategy.CREATE_AS_NEW_ID),
                ).getOrThrow()

            val allPatients = fhirRepo.getAllPatients()
            assertEquals(2, allPatients.size)
            val rekeyedPatient = allPatients.first { it.id != "p-conflict" }
            assertNotNull(rekeyedPatient)

            // 5. Ingest without conflict for KEEP_LOCAL and MERGE_RECORDS
            val nonConflictingPatient1 = createFhirPatient("p-no-conf-1", "NoConf", "One", LocalDate(2000, 1, 1), "MRN-NC1")
            val nonConflictingPatient2 = createFhirPatient("p-no-conf-2", "NoConf", "Two", LocalDate(2000, 2, 2), "MRN-NC2")
            val nonConfBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = listOf(Bundle.Entry(resource = nonConflictingPatient1), Bundle.Entry(resource = nonConflictingPatient2)),
                )
            val nonConfJson =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(
                        Bundle.serializer(),
                        nonConfBundle,
                    ).getOrThrow()
            val nonConfEncrypted = cryptoService.encrypt(nonConfJson, "password123")

            service
                .importDataSelective(
                    encryptedData = nonConfEncrypted,
                    password = "password123",
                    resolutionMap =
                        mapOf(
                            "p-no-conf-1" to ConflictResolutionStrategy.KEEP_LOCAL,
                            "p-no-conf-2" to ConflictResolutionStrategy.MERGE_RECORDS,
                        ),
                ).getOrThrow()
            assertNotNull(fhirRepo.getPatient("p-no-conf-1"))
            assertNotNull(fhirRepo.getPatient("p-no-conf-2"))
        }

    /**
     * Tests rollback of saved images when a batch import fails.
     */
    @Test
    fun testImportRollbackOnFailure() =
        runTest {
            val rollbackStorage =
                object : FileStorage {
                    val deletedFiles = mutableListOf<String>()
                    var count = 0

                    override fun saveImage(
                        fileName: String,
                        bytes: ByteArray,
                    ): String {
                        count++
                        if (count > 1) {
                            throw IllegalStateException() // allow-exception
                        }
                        return fileName
                    }

                    override fun readImage(path: String): ByteArray = ByteArray(0)

                    override fun deleteImage(path: String): Result<Unit> {
                        deletedFiles.add(path)
                        return Result.success(Unit)
                    }

                    override fun clearCache() {}
                }

            val rollbackService = ExportImportService(db, rollbackStorage)
            val bin1 =
                Binary(
                    id = "bin-rollback-1",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(1).toByteString().base64()),
                )
            val bin2 =
                Binary(
                    id = "bin-rollback-2",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(2).toByteString().base64()),
                )

            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = listOf(Bundle.Entry(resource = bin1), Bundle.Entry(resource = bin2)),
                )
            val json =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle)
                    .getOrThrow()
            val encrypted = cryptoService.encrypt(json, "password123")

            val result = rollbackService.importDataSelective(encrypted, "password123")
            assertTrue(result.isFailure)
            assertEquals(listOf("bin-rollback-1"), rollbackStorage.deletedFiles)

            // Test rollback with explicit message
            rollbackStorage.count = 0
            val rollbackStorageWithMsg =
                object : FileStorage {
                    var count = 0

                    override fun saveImage(
                        fileName: String,
                        bytes: ByteArray,
                    ): String {
                        count++
                        if (count > 1) {
                            throw IllegalStateException("Explicit rollback error") // allow-exception
                        }
                        return fileName
                    }

                    override fun readImage(path: String): ByteArray = ByteArray(0)

                    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

                    override fun clearCache() {}
                }
            val rollbackServiceWithMsg = ExportImportService(db, rollbackStorageWithMsg)
            val resultWithMsg = rollbackServiceWithMsg.importDataSelective(encrypted, "password123")
            assertTrue(resultWithMsg.isFailure)

            // Test rollback with blank message
            val rollbackStorageWithBlankMsg =
                object : FileStorage {
                    var count = 0

                    override fun saveImage(
                        fileName: String,
                        bytes: ByteArray,
                    ): String {
                        count++
                        if (count > 1) {
                            throw IllegalStateException("   ") // allow-exception
                        }
                        return fileName
                    }

                    override fun readImage(path: String): ByteArray = ByteArray(0)

                    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

                    override fun clearCache() {}
                }
            val rollbackServiceWithBlankMsg = ExportImportService(db, rollbackStorageWithBlankMsg)
            val resultWithBlankMsg = rollbackServiceWithBlankMsg.importDataSelective(encrypted, "password123")
            assertTrue(resultWithBlankMsg.isFailure)

            // Test ingestPatient with localConflict having null ID
            val patientWithNullIdConflict = Patient(id = null)
            val incoming = createFhirPatient("p-incoming-test", "Inc", "Test", LocalDate(2000, 1, 1), "MRN-T")
            val ingestedId = service.ingestPatient(incoming, ConflictResolutionStrategy.MERGE_RECORDS, patientWithNullIdConflict)
            assertEquals("p-incoming-test", ingestedId)
        }

    /**
     * Tests importDataSelective with disabled filter categories and patient selection filter.
     */
    @Test
    fun testImportDataSelectiveCategoryFiltering() =
        runTest {
            val patient = createFhirPatient("p-filter", "Filter", "Test", LocalDate(1980, 1, 1), "MRN-FILT")
            val prac = Practitioner(id = "prac-filter", active = FhirBoolean(value = true))
            val device = Device(id = "dev-filter")
            val questionnaire =
                Questionnaire(id = "q-filter", status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
            val provenance =
                Provenance(
                    id = "prov-filter",
                    target = listOf(Reference(reference = FhirString(value = "Patient/p-filter"))),
                    recorded = Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                    agent = emptyList(),
                )
            val binary =
                Binary(
                    id = "bin-filter",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(5, 6, 7).toByteString().base64()),
                )
            val binNullData = Binary(id = "bin-null-data", contentType = Code(value = "image/jpeg"), data = null)
            val binNullId =
                Binary(
                    id = null,
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(1).toByteString().base64()),
                )
            val binCorruptData =
                Binary(
                    id = "bin-corrupt",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = "not base64"),
                )
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val encUnknownPat =
                Encounter(
                    id = "enc-unknown",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-unknown")),
                )
            val docUnknownPat =
                DocumentReference(
                    id = "doc-unknown",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    content = emptyList(),
                    subject = Reference(reference = FhirString(value = "Patient/p-unknown")),
                )
            val qrUnknownPat =
                QuestionnaireResponse(
                    id = "qr-unknown",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    subject = Reference(reference = FhirString(value = "Patient/p-unknown")),
                )
            val basic =
                dev.ohs.fhir.model.r4.Basic(
                    id = "basic-unknown",
                    code =
                        dev.ohs.fhir.model.r4
                            .CodeableConcept(text = FhirString(value = "unknown")),
                )

            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(resource = patient),
                            Bundle.Entry(resource = prac),
                            Bundle.Entry(resource = device),
                            Bundle.Entry(resource = questionnaire),
                            Bundle.Entry(resource = provenance),
                            Bundle.Entry(resource = binary),
                            Bundle.Entry(resource = binNullData),
                            Bundle.Entry(resource = binNullId),
                            Bundle.Entry(resource = binCorruptData),
                            Bundle.Entry(resource = encUnknownPat),
                            Bundle.Entry(resource = docUnknownPat),
                            Bundle.Entry(resource = qrUnknownPat),
                            Bundle.Entry(resource = basic),
                        ),
                )
            val json =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle)
                    .getOrThrow()
            val encrypted = cryptoService.encrypt(json, "password123")

            // Test with all categories disabled
            val disabledOptions =
                ImportFilterOptions(
                    enabledCategories = emptySet(),
                )

            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    filterOptions = disabledOptions,
                ).getOrThrow()

            // Patient, Practitioner, Questionnaire, Binary should be skipped
            assertEquals(null, fhirRepo.getPatient("p-filter"))
            assertEquals(null, fhirRepo.getPractitioner("prac-filter"))
            assertEquals(null, fhirRepo.getQuestionnaire("q-filter"))
            assertFalse(fileStorage.files.containsKey("bin-filter"))

            // Device and Provenance should be imported
            assertNotNull(fhirRepo.getDevice("dev-filter"))
            assertEquals(1, fhirRepo.getAllProvenances().size)

            // Import with QUESTIONNAIRES enabled
            service
                .importDataSelective(
                    encryptedData = encrypted,
                    password = "password123",
                    filterOptions = ImportFilterOptions(enabledCategories = setOf(ImportCategory.QUESTIONNAIRES)),
                ).getOrThrow()
            assertNotNull(fhirRepo.getQuestionnaire("q-filter"))

            // Patient Selection Filter: selectedPatientIds restricts import
            val patient2 = createFhirPatient("p-unselected", "Skip", "Me", LocalDate(1990, 1, 1), "MRN-SKIP")
            val bundle2 =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = listOf(Bundle.Entry(resource = patient), Bundle.Entry(resource = patient2)),
                )
            val json2 =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle2)
                    .getOrThrow()
            val encrypted2 = cryptoService.encrypt(json2, "password123")

            service
                .importDataSelective(
                    encryptedData = encrypted2,
                    password = "password123",
                    selectedPatientIds = setOf("p-filter"),
                ).getOrThrow()

            assertNotNull(fhirRepo.getPatient("p-filter"))
            assertEquals(null, fhirRepo.getPatient("p-unselected"))
        }

    /**
     * Tests import of unknown patient references and binary edge cases with all categories enabled.
     */
    @Test
    fun testImportAllWithUnknownPatientAndBinaries() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val encUnknownPat =
                Encounter(
                    id = "enc-unk",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-unk")),
                )
            val encNullRefVal =
                Encounter(
                    id = "enc-null-ref-val",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = null)),
                )
            val docUnknownPat =
                DocumentReference(
                    id = "doc-unk",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    content = emptyList(),
                    subject = Reference(reference = FhirString(value = "Patient/p-unk")),
                )
            val qrUnknownPat =
                QuestionnaireResponse(
                    id = "qr-unk",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    subject = Reference(reference = FhirString(value = "Patient/p-unk")),
                )
            val binValid =
                Binary(
                    id = "bin-valid-2",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(9).toByteString().base64()),
                )
            val binNullData = Binary(id = "bin-null-data-2", contentType = Code(value = "image/jpeg"), data = null)
            val binNullId =
                Binary(
                    id = null,
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(1).toByteString().base64()),
                )
            val binBlankId =
                Binary(
                    id = "   ",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = byteArrayOf(1).toByteString().base64()),
                )
            val binCorruptData =
                Binary(
                    id = "bin-corrupt-2",
                    contentType = Code(value = "image/jpeg"),
                    data =
                        dev.ohs.fhir.model.r4
                            .Base64Binary(value = "not base64"),
                )

            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(resource = encUnknownPat),
                            Bundle.Entry(resource = encNullRefVal),
                            Bundle.Entry(resource = docUnknownPat),
                            Bundle.Entry(resource = qrUnknownPat),
                            Bundle.Entry(resource = binValid),
                            Bundle.Entry(resource = binNullData),
                            Bundle.Entry(resource = binNullId),
                            Bundle.Entry(resource = binBlankId),
                            Bundle.Entry(resource = binCorruptData),
                        ),
                )
            val json =
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .encodeTypedResource(Bundle.serializer(), bundle)
                    .getOrThrow()
            val encrypted = cryptoService.encrypt(json, "password123")

            val result = service.importDataSelective(encrypted, "password123", filterOptions = ImportFilterOptions.all())
            assertTrue(result.isSuccess)
            assertNotNull(fhirRepo.getEncounter("enc-unk"))
            assertNotNull(fhirRepo.getDocumentReference("doc-unk"))
            assertNotNull(fhirRepo.getQuestionnaireResponse("qr-unk"))
            assertTrue(fileStorage.files.containsKey("bin-valid-2"))
        }

    /**
     * Memory-backed file storage for testing cleanup and persistence.
     */
    class TestMemoryFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()
        var wasCacheCleared = false

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray {
            if (path == "crash-read.jpg") throw IllegalStateException("Read crashed") // allow-exception
            return files[path] ?: ByteArray(0)
        }

        override fun deleteImage(path: String): Result<Unit> {
            files.remove(path)
            return Result.success(Unit)
        }

        override fun clearCache() {
            wasCacheCleared = true
            files.clear()
        }
    }
}
