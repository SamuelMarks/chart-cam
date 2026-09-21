/**
 * @file FhirBundleOrchestratorTest.kt
 * Tests verifying decentralized encounter bundle creation, extraction, and air-gapped QR chunking/reassembly.
 */

package io.healthplatform.chartcam.fhir

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.transfer.AirGappedBundleTransferService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests verifying FHIR bundle orchestration and QR transfer pipelines.
 */
class FhirBundleOrchestratorTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Initializes in-memory database and repository.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Closes the in-memory driver.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests full encounter bundle creation, unpacking, and offline repository ingestion.
     */
    @Test
    fun testEncounterBundleCreationAndIngestion() =
        runTest {
            val patient =
                createFhirPatient(
                    id = "pat-bundle-1",
                    firstName = "Alice",
                    lastName = "Bundle",
                    dob = LocalDate(1992, 5, 10),
                    mrnValue = "MRN-B1",
                )
            repository.savePatient(patient)

            val encounter =
                createFhirEncounter(
                    id = "enc-bundle-1",
                    patientId = "pat-bundle-1",
                    practitionerId = "prac-bundle-1",
                    dateStr = "2026-09-17T10:00:00Z",
                )
            repository.saveEncounter(encounter)

            val qr =
                QuestionnaireResponse(
                    id = "qr-bundle-1",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                )
            repository.saveQuestionnaireResponse(qr)

            val bundleResult =
                FhirBundleOrchestrator.createEncounterBundle(
                    repository = repository,
                    encounterId = "enc-bundle-1",
                    patientId = "pat-bundle-1",
                    bundleType = Bundle.BundleType.Collection,
                )

            assertTrue(bundleResult.isSuccess)
            val bundle = bundleResult.getOrThrow()
            assertEquals(Bundle.BundleType.Collection, bundle.type.value)
            assertTrue(bundle.entry.size >= 2)

            val unpacked = FhirBundleOrchestrator.unpackEncounterBundle(bundle)
            assertTrue(unpacked.isSuccess)
            val resources = unpacked.getOrThrow()
            assertTrue(resources.any { it is Patient && it.id == "pat-bundle-1" })

            // Test ingestion into a clean second database
            val driver2 = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver2)
            val db2 = ChartCamDatabase(driver2)
            val repo2 = FhirRepository(db2)

            val ingestResult = FhirBundleOrchestrator.ingestBundle(repo2, bundle)
            assertTrue(ingestResult.isSuccess)
            assertTrue(ingestResult.getOrThrow() >= 2)

            val fetchedPatient = repo2.getPatientCatching("pat-bundle-1").getOrThrow()
            assertNotNull(fetchedPatient)
            assertEquals("pat-bundle-1", fetchedPatient.id)

            driver2.close()
        }

    /**
     * Tests QR chunking and reassembly with checksum verification.
     */
    @Test
    fun testAirGappedQrChunkingAndReassembly() {
        val sampleJson = """{"resourceType":"Bundle","id":"test-b1","type":"collection","entry":[{"fullUrl":"urn:uuid:p1"}]}"""
        val chunkResult = AirGappedBundleTransferService.chunkBundleForQr(sampleJson, maxChunkSize = 25)
        assertTrue(chunkResult.isSuccess)
        val chunks = chunkResult.getOrThrow()
        assertTrue(chunks.size > 1)

        // Shuffle chunks to test out-of-order scanning
        val shuffled = chunks.shuffled()
        val reassembledResult = AirGappedBundleTransferService.assembleQrChunks(shuffled)
        assertTrue(reassembledResult.isSuccess)
        assertEquals(sampleJson, reassembledResult.getOrThrow())

        // Test incomplete set
        val incomplete = chunks.drop(1)
        val failResult = AirGappedBundleTransferService.assembleQrChunks(incomplete)
        assertTrue(failResult.isFailure)
    }

    /**
     * Verifies that unpacking an empty bundle returns a failure result.
     */
    @Test
    fun testUnpackEmptyBundleReturnsFailure() {
        val emptyBundle =
            Bundle(
                type = Enumeration(value = Bundle.BundleType.Collection),
            )
        val result = FhirBundleOrchestrator.unpackEncounterBundle(emptyBundle)
        assertTrue(result.isFailure)
    }

    /**
     * Verifies that a self-contained bundle with valid internal references passes validation.
     */
    @Test
    fun testValidSelfContainedBundle() {
        val patient = Patient(id = "pat-1")
        val encounter =
            Encounter(
                id = "enc-1",
                status = Enumeration(value = Encounter.EncounterStatus.Finished),
                subject = Reference(reference = FhirString(value = "Patient/pat-1")),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code("AMB"),
                        ),
            )

        val bundle =
            Bundle(
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:pat-1"),
                            resource = patient,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:enc-1"),
                            resource = encounter,
                        ),
                    ),
            )

        val validationRes = FhirBundleOrchestrator.validateAirGappedSelfContainment(bundle)
        assertTrue(validationRes.isSuccess)
        val validation = validationRes.getOrThrow()
        assertTrue(validation.isValid)
        assertTrue(validation.danglingReferences.isEmpty())
        assertTrue(validation.unresolvedLocalReferences.isEmpty())
    }

    /**
     * Verifies that bundles with unresolvable external URLs or broken internal references are flagged.
     */
    @Test
    fun testInvalidAirGappedBundleFlagged() {
        val encounter =
            Encounter(
                id = "enc-broken",
                status = Enumeration(value = Encounter.EncounterStatus.Finished),
                subject = Reference(reference = FhirString(value = "https://external-ehr.hospital.com/Patient/999")),
                serviceProvider = Reference(reference = FhirString(value = "Organization/missing-org")),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code("AMB"),
                        ),
            )

        val bundle =
            Bundle(
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:enc-broken"),
                            resource = encounter,
                        ),
                    ),
            )

        val validationRes = FhirBundleOrchestrator.validateAirGappedSelfContainment(bundle)
        assertTrue(validationRes.isSuccess)
        val validation = validationRes.getOrThrow()
        kotlin.test.assertFalse(validation.isValid)
        assertEquals(1, validation.danglingReferences.size)
        assertTrue(validation.danglingReferences.first().contains("external-ehr.hospital.com"))
        assertEquals(1, validation.unresolvedLocalReferences.size)
        assertTrue(validation.unresolvedLocalReferences.first().contains("missing-org"))
    }

    /**
     * Tests executing a transaction bundle with POST, PUT, and DELETE operations.
     */
    @Test
    fun testExecuteTransactionBundle() =
        runTest {
            val patToCreate =
                createFhirPatient(
                    id = "pat-txn-1",
                    firstName = "Txn",
                    lastName = "User",
                    dob = LocalDate(1980, 1, 1),
                    mrnValue = "MRN-TXN",
                )
            val encToUpdate =
                createFhirEncounter(
                    id = "enc-txn-1",
                    patientId = "pat-txn-1",
                    practitionerId = "prac-1",
                    dateStr = "2026-09-17T10:00:00Z",
                )
            val postEntry =
                Bundle.Entry(
                    resource = patToCreate,
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Post),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Patient"),
                        ),
                )
            val putEntry =
                Bundle.Entry(
                    resource = encToUpdate,
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Put),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Encounter/enc-txn-1"),
                        ),
                )
            val txnBundle =
                Bundle(
                    id = "txn-bundle-1",
                    type = Enumeration(value = Bundle.BundleType.Transaction),
                    entry = listOf(postEntry, putEntry),
                )

            val respResult = FhirBundleOrchestrator.executeTransactionBundle(repository, txnBundle)
            assertTrue(respResult.isSuccess)
            val resp = respResult.getOrThrow()
            assertEquals(Bundle.BundleType.Transaction_Response, resp.type.value)
            assertEquals(2, resp.entry.size)
            assertEquals(
                "201 Created",
                resp.entry[0]
                    .response
                    ?.status
                    ?.value,
            )
            assertEquals(
                "200 OK",
                resp.entry[1]
                    .response
                    ?.status
                    ?.value,
            )

            val savedPat = repository.getPatientCatching("pat-txn-1").getOrThrow()
            assertNotNull(savedPat)

            val deleteEntry =
                Bundle.Entry(
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Delete),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Patient/pat-txn-1"),
                        ),
                )
            val deleteBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Transaction),
                    entry = listOf(deleteEntry),
                )
            val delRespResult = FhirBundleOrchestrator.executeTransactionBundle(repository, deleteBundle)
            assertTrue(delRespResult.isSuccess)
            val delResp = delRespResult.getOrThrow()
            assertEquals(
                "204 No Content",
                delResp.entry
                    .first()
                    .response
                    ?.status
                    ?.value,
            )
            kotlin.test.assertNull(repository.getPatientCatching("pat-txn-1").getOrThrow())
        }

    /**
     * Tests importBundlePayload and importProtobufBundlePayload in AirGappedBundleTransferService.
     */
    @Test
    fun testImportBundlePayloads() =
        runTest {
            val bundle =
                Bundle(
                    id = "b-import-1",
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(
                                resource =
                                    Patient(
                                        id = "p-imported-1",
                                        name =
                                            listOf(
                                                dev.ohs.fhir.model.r4
                                                    .HumanName(
                                                        family =
                                                            dev.ohs.fhir.model.r4
                                                                .String(value = "Imported"),
                                                    ),
                                            ),
                                    ),
                            ),
                        ),
                )
            val jsonStr = FhirJsonParser.encodeResource(bundle).getOrThrow()
            val res1 =
                io.healthplatform.chartcam.transfer.AirGappedBundleTransferService
                    .importBundlePayload(repository, jsonStr)
            assertTrue(res1.isSuccess)

            val protoBytes = FhirProtobufParser.encodeToProtobuf(bundle).getOrThrow()
            val res2 =
                io.healthplatform.chartcam.transfer.AirGappedBundleTransferService.importProtobufBundlePayload(
                    repository,
                    protoBytes,
                )
            assertTrue(res2.isSuccess)

            val fail1 =
                io.healthplatform.chartcam.transfer.AirGappedBundleTransferService
                    .importBundlePayload(repository, "invalid json")
            assertTrue(fail1.isFailure)

            val fail2 =
                io.healthplatform.chartcam.transfer.AirGappedBundleTransferService
                    .importProtobufBundlePayload(repository, byteArrayOf(1, 2, 3))
            assertTrue(fail2.isFailure)

            val emptyBundle = Bundle(type = Enumeration(value = Bundle.BundleType.Collection), entry = emptyList())
            val failEmptyIngest = FhirBundleOrchestrator.ingestBundle(repository, emptyBundle)
            assertTrue(failEmptyIngest.isFailure)

            driver.close()
            val failIngest = FhirBundleOrchestrator.ingestBundle(repository, bundle)
            assertTrue(failIngest.isFailure)

            val failSaveTxn =
                FhirBundleOrchestrator.executeTransactionBundle(
                    repository,
                    Bundle(
                        type = Enumeration(value = Bundle.BundleType.Transaction),
                        entry = listOf(Bundle.Entry(resource = bundle.entry.first().resource)),
                    ),
                )
            assertTrue(failSaveTxn.isFailure)

            val failDelTxn =
                FhirBundleOrchestrator.executeTransactionBundle(
                    repository,
                    Bundle(
                        type = Enumeration(value = Bundle.BundleType.Transaction),
                        entry =
                            listOf(
                                Bundle.Entry(
                                    request =
                                        Bundle.Entry.Request(
                                            method = Enumeration(value = Bundle.HTTPVerb.Delete),
                                            url =
                                                dev.ohs.fhir.model.r4
                                                    .Uri(value = "Patient/p1"),
                                        ),
                                ),
                            ),
                    ),
                )
            assertTrue(failDelTxn.isFailure)
        }

    /**
     * Tests reference extraction from Observation, DocumentReference, and QuestionnaireResponse, plus local fragment references.
     */
    @Test
    fun testReferenceExtractionAndSelfContainmentComprehensive() {
        val patient =
            Patient(
                id = "p-full",
                managingOrganization = Reference(reference = FhirString(value = "Organization/org-1")),
            )
        val encounter =
            Encounter(
                id = "enc-full",
                status = Enumeration(value = Encounter.EncounterStatus.Finished),
                subject = Reference(reference = FhirString(value = "Patient/p-full")),
                serviceProvider = Reference(reference = FhirString(value = "Organization/org-1")),
                participant =
                    listOf(
                        Encounter.Participant(
                            individual = Reference(reference = FhirString(value = "Practitioner/prac-1")),
                        ),
                        Encounter.Participant(
                            individual = Reference(reference = null),
                        ),
                        Encounter.Participant(
                            individual = null,
                        ),
                    ),
                `class` =
                    dev.ohs.fhir.model.r4.Coding(
                        code =
                            dev.ohs.fhir.model.r4
                                .Code("AMB"),
                    ),
            )
        val observation =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-full",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                subject = Reference(reference = FhirString(value = "Patient/p-full")),
                encounter = Reference(reference = FhirString(value = "Encounter/enc-full")),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(text = FhirString(value = "HR")),
            )
        val docRef =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "doc-full",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                subject = Reference(reference = FhirString(value = "#contained-patient")),
                author =
                    listOf(
                        Reference(reference = FhirString(value = "Practitioner/prac-1")),
                        Reference(reference = null),
                    ),
                content = emptyList(),
            )
        val qr =
            QuestionnaireResponse(
                id = "qr-full",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                subject = Reference(reference = FhirString(value = "Patient/p-full")),
                encounter = Reference(reference = FhirString(value = "Encounter/enc-full")),
                author = Reference(reference = FhirString(value = "Practitioner/prac-1")),
            )
        val org =
            dev.ohs.fhir.model.r4.Organization(
                id = "org-1",
                name = FhirString(value = "Main Hospital"),
            )
        val practitioner =
            dev.ohs.fhir.model.r4.Practitioner(
                id = "prac-1",
            )

        val bundle =
            Bundle(
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:p-full"),
                            resource = patient,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:enc-full"),
                            resource = encounter,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:obs-full"),
                            resource = observation,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:doc-full"),
                            resource = docRef,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "urn:uuid:qr-full"),
                            resource = qr,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Organization/org-1"),
                            resource = org,
                        ),
                        Bundle.Entry(
                            fullUrl =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Practitioner/prac-1"),
                            resource = practitioner,
                        ),
                        Bundle.Entry(fullUrl = null, resource = null),
                    ),
            )

        val validation = FhirBundleOrchestrator.validateAirGappedSelfContainment(bundle).getOrThrow()
        assertTrue(validation.isValid)
        assertTrue(validation.danglingReferences.isEmpty())
        assertTrue(validation.unresolvedLocalReferences.isEmpty())
    }

    /**
     * Tests error handling in createEncounterBundle when patient or encounter is not found.
     */
    @Test
    fun testCreateEncounterBundleNotFoundErrors() =
        runTest {
            val res1 = FhirBundleOrchestrator.createEncounterBundle(repository, "non-existent-enc", "non-existent-pat")
            assertTrue(res1.isFailure)

            val patient = createFhirPatient("p-only", "A", "B", LocalDate(1990, 1, 1), "MRN-O")
            repository.savePatient(patient)
            val res2 = FhirBundleOrchestrator.createEncounterBundle(repository, "non-existent-enc", "p-only")
            assertTrue(res2.isFailure)

            // Test when getPatientCatching returns failure Result
            val failingPatientRepo =
                object : FhirRepository(db) {
                    override suspend fun getPatientCatching(id: String): Result<Patient?> =
                        Result.failure(IllegalStateException("Simulated patient fetch failure"))
                }
            val resFailingPatient = FhirBundleOrchestrator.createEncounterBundle(failingPatientRepo, "enc-1", "p-1")
            assertTrue(resFailingPatient.isFailure)

            // Test when getEncounterCatching returns failure Result
            val failingEncounterRepo =
                object : FhirRepository(db) {
                    override suspend fun getPatientCatching(id: String): Result<Patient?> =
                        Result.success(patient)

                    override suspend fun getEncounterCatching(id: String): Result<Encounter?> =
                        Result.failure(IllegalStateException("Simulated encounter fetch failure"))
                }
            val resFailingEncounter = FhirBundleOrchestrator.createEncounterBundle(failingEncounterRepo, "enc-1", "p-only")
            assertTrue(resFailingEncounter.isFailure)
        }

    /**
     * Tests transaction processing error conditions (missing URLs, missing resources).
     */
    @Test
    fun testTransactionProcessingErrors() =
        runTest {
            val deleteNoUrl =
                Bundle.Entry(
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Delete),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = null),
                        ),
                )
            val txn1 = Bundle(type = Enumeration(value = Bundle.BundleType.Transaction), entry = listOf(deleteNoUrl))
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txn1).isFailure)

            val postNoRes =
                Bundle.Entry(
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Post),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Patient"),
                        ),
                    resource = null,
                )
            val txn2 = Bundle(type = Enumeration(value = Bundle.BundleType.Transaction), entry = listOf(postNoRes))
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txn2).isFailure)

            val putNoRes =
                Bundle.Entry(
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Put),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Patient/123"),
                        ),
                    resource = null,
                )
            val txn3 = Bundle(type = Enumeration(value = Bundle.BundleType.Transaction), entry = listOf(putNoRes))
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txn3).isFailure)
        }

    /**
     * Tests exhaustive branch cases for bundle creation, transaction entry with null ID, and reference inspections.
     */
    @Test
    fun testBundleOrchestratorExhaustiveBranches() =
        runTest {
            // 1. Transaction Post and Put with null resource IDs
            val postPatNullId = Patient(id = null)
            val putPatNullId = Patient(id = null)
            val txnBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Transaction),
                    entry =
                        listOf(
                            Bundle.Entry(
                                resource = postPatNullId,
                                request =
                                    Bundle.Entry.Request(
                                        method = Enumeration(value = Bundle.HTTPVerb.Post),
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Uri(value = "Patient"),
                                    ),
                            ),
                            Bundle.Entry(
                                resource = putPatNullId,
                                request =
                                    Bundle.Entry.Request(
                                        method = Enumeration(value = Bundle.HTTPVerb.Put),
                                        url =
                                            dev.ohs.fhir.model.r4
                                                .Uri(value = "Patient"),
                                    ),
                            ),
                        ),
                )
            val txnRes = FhirBundleOrchestrator.executeTransactionBundle(repository, txnBundle)
            assertTrue(txnRes.isSuccess)
            val txnEntries = txnRes.getOrThrow().entry
            assertEquals(2, txnEntries.size)
            assertEquals("201 Created", txnEntries[0].response?.status?.value)
            assertEquals("200 OK", txnEntries[1].response?.status?.value)

            // 2. Encounter bundle creation where resources have urn:uuid: prefix or null ID
            val patWithUrn =
                createFhirPatient(
                    id = "urn:uuid:pat-urn-prefix",
                    firstName = "Urn",
                    lastName = "User",
                    dob = LocalDate(1995, 1, 1),
                    mrnValue = "MRN-URN",
                )
            repository.savePatient(patWithUrn)
            val encWithUrn =
                createFhirEncounter(
                    id = "urn:uuid:enc-urn-prefix",
                    patientId = "urn:uuid:pat-urn-prefix",
                    practitionerId = "prac-1",
                    dateStr = "2026-09-17T10:00:00Z",
                )
            repository.saveEncounter(encWithUrn)

            val createdBundle =
                FhirBundleOrchestrator
                    .createEncounterBundle(
                        repository = repository,
                        encounterId = "urn:uuid:enc-urn-prefix",
                        patientId = "urn:uuid:pat-urn-prefix",
                    ).getOrThrow()
            assertTrue(createdBundle.entry.all { it.fullUrl?.value?.startsWith("urn:uuid:") == true })

            // 3. Ingest bundle where entry resource has null ID
            val nullIdBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = listOf(Bundle.Entry(resource = Patient(id = null))),
                )
            val ingestedCount = FhirBundleOrchestrator.ingestBundle(repository, nullIdBundle)
            assertTrue(ingestedCount.isSuccess)
            assertEquals(1, ingestedCount.getOrThrow())

            // 4. validateAirGappedSelfContainment with resolved external http/https and null reference fields
            val externalHttpUrl = "http://example.org/fhir/Patient/ext-1"
            val externalHttpsUrl = "https://secure.org/fhir/Encounter/ext-2"
            val selfContainedWithHttp =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(
                                fullUrl =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = externalHttpUrl),
                                resource =
                                    Patient(
                                        id = "ext-1",
                                        managingOrganization = Reference(reference = FhirString(value = null)),
                                    ),
                            ),
                            Bundle.Entry(
                                fullUrl =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = externalHttpsUrl),
                                resource =
                                    Encounter(
                                        id = "ext-2",
                                        status = Enumeration(value = Encounter.EncounterStatus.Finished),
                                        subject = Reference(reference = FhirString(value = externalHttpUrl)),
                                        serviceProvider = Reference(reference = FhirString(value = externalHttpsUrl)),
                                        `class` =
                                            dev.ohs.fhir.model.r4
                                                .Coding(
                                                    code =
                                                        dev.ohs.fhir.model.r4
                                                            .Code("AMB"),
                                                ),
                                    ),
                            ),
                            Bundle.Entry(
                                resource =
                                    dev.ohs.fhir.model.r4.Observation(
                                        status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .CodeableConcept(),
                                        subject = Reference(reference = FhirString(value = "#frag-local")),
                                    ),
                            ),
                            Bundle.Entry(
                                resource =
                                    dev.ohs.fhir.model.r4.DocumentReference(
                                        status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                                        content = emptyList(),
                                        subject = null,
                                        author = emptyList(),
                                    ),
                            ),
                            Bundle.Entry(
                                resource =
                                    QuestionnaireResponse(
                                        status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                                        subject = null,
                                        encounter = null,
                                        author = null,
                                    ),
                            ),
                        ),
                )
            val selfContainedResult = FhirBundleOrchestrator.validateAirGappedSelfContainment(selfContainedWithHttp).getOrThrow()
            assertTrue(selfContainedResult.isValid)
            assertTrue(selfContainedResult.danglingReferences.isEmpty())
            assertTrue(selfContainedResult.unresolvedLocalReferences.isEmpty())

            // 5. Encounter bundle creation where resources have null IDs
            val patNoId = Patient(id = null)
            repository.savePatient(patNoId)
            val encNoId =
                Encounter(
                    id = null,
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code("AMB"),
                            ),
                    subject = Reference(reference = FhirString(value = "Patient/")),
                )
            repository.saveEncounter(encNoId)
            val nullCreatedBundle =
                FhirBundleOrchestrator
                    .createEncounterBundle(
                        repository = repository,
                        encounterId = "",
                        patientId = "",
                    ).getOrThrow()
            assertTrue(nullCreatedBundle.entry.isNotEmpty())

            // 6. Transaction with missing request, missing method, missing url
            val noReqEntry = Bundle.Entry(resource = Patient(id = "p-noreq"))
            val noReqNullIdEntry = Bundle.Entry(resource = Patient(id = null))
            val noReqNullResEntry = Bundle.Entry(resource = null)
            val nullMethodEntry =
                Bundle.Entry(
                    resource = Patient(id = "p-nometh"),
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = null),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "Patient"),
                        ),
                )
            val delNoUrl =
                Bundle.Entry(
                    request =
                        Bundle.Entry.Request(
                            method = Enumeration(value = Bundle.HTTPVerb.Delete),
                            url =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = null),
                        ),
                )
            val txn4 =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Transaction),
                    entry = listOf(noReqEntry, noReqNullIdEntry, nullMethodEntry),
                )
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txn4).isSuccess)

            val txnNoReqFail = Bundle(type = Enumeration(value = Bundle.BundleType.Transaction), entry = listOf(noReqNullResEntry))
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txnNoReqFail).isFailure)

            val txnDelFail = Bundle(type = Enumeration(value = Bundle.BundleType.Transaction), entry = listOf(delNoUrl))
            assertTrue(FhirBundleOrchestrator.executeTransactionBundle(repository, txnDelFail).isFailure)

            // 7. collectBundleIdentifiers with blank resource id and null fullUrl / null fullUrl value
            val blankIdBundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry =
                        listOf(
                            Bundle.Entry(
                                fullUrl = null,
                                resource = Patient(id = "   "),
                            ),
                            Bundle.Entry(
                                fullUrl =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = null),
                                resource = Patient(id = "p-nuv"),
                            ),
                        ),
                )
            val blankIdRes = FhirBundleOrchestrator.validateAirGappedSelfContainment(blankIdBundle).getOrThrow()
            assertTrue(blankIdRes.isValid)
        }
}
