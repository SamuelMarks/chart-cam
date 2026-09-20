/**
 * @file FhirRepositoryPerformanceTest.kt
 * Performance and typed serialization verification test for FhirRepository.
 */

package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.search.PatientSearchParams
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class verifying typed serialization throughput, safe reflection-free search, and typed retrieval.
 */
class FhirRepositoryPerformanceTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Initializes an in-memory database and repository before each test.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Closes the database driver after each test.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests typed save and retrieval across all core clinical resource types.
     */
    @Test
    fun testTypedSaveAndRetrieveAllEntityTypes() =
        runTest {
            // Patient
            val patient =
                Patient(
                    id = "p-1",
                    name =
                        listOf(
                            HumanName(
                                family =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Test"),
                            ),
                        ),
                )
            assertTrue(repository.savePatient(patient).isSuccess)
            val fetchedPatient = repository.getPatientCatching("p-1").getOrThrow()
            assertNotNull(fetchedPatient)
            assertEquals("p-1", fetchedPatient.id)

            // Encounter
            val encounter =
                Encounter(
                    id = "e-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code("AMB"),
                            ),
                )
            assertTrue(repository.saveEncounter(encounter).isSuccess)
            val fetchedEncounter = repository.getEncounterCatching("e-1").getOrThrow()
            assertNotNull(fetchedEncounter)
            assertEquals("e-1", fetchedEncounter.id)

            // Practitioner
            val practitioner =
                Practitioner(
                    id = "pr-1",
                    name =
                        listOf(
                            HumanName(
                                family =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Doctor"),
                            ),
                        ),
                )
            assertTrue(repository.savePractitioner(practitioner).isSuccess)
            val fetchedPractitioner = repository.getPractitionerCatching("pr-1").getOrThrow()
            assertNotNull(fetchedPractitioner)
            assertEquals("pr-1", fetchedPractitioner.id)

            // DocumentReference
            val doc =
                DocumentReference(
                    id = "doc-1",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    content = emptyList(),
                    category = emptyList(),
                )
            assertTrue(repository.saveDocumentReference(doc).isSuccess)
            val fetchedDoc = repository.getDocumentReferenceCatching("doc-1").getOrThrow()
            assertNotNull(fetchedDoc)
            assertEquals("doc-1", fetchedDoc.id)

            // Questionnaire
            val questionnaire =
                Questionnaire(
                    id = "q-1",
                    status = Enumeration(value = PublicationStatus.Active),
                )
            assertTrue(repository.saveQuestionnaire(questionnaire).isSuccess)
            val fetchedQ = repository.getQuestionnaireCatching("q-1").getOrThrow()
            assertNotNull(fetchedQ)
            assertEquals("q-1", fetchedQ.id)

            // QuestionnaireResponse
            val qr =
                QuestionnaireResponse(
                    id = "qr-1",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                )
            assertTrue(repository.saveQuestionnaireResponse(qr).isSuccess)
            val fetchedQr = repository.getQuestionnaireResponseCatching("qr-1").getOrThrow()
            assertNotNull(fetchedQr)
            assertEquals("qr-1", fetchedQr.id)

            // Device
            val device = Device(id = "dev-1")
            assertTrue(repository.saveDevice(device).isSuccess)
            val fetchedDevice = repository.getDeviceCatching("dev-1").getOrThrow()
            assertNotNull(fetchedDevice)
            assertEquals("dev-1", fetchedDevice.id)

            // Observation
            val observation =
                Observation(
                    id = "obs-1",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code =
                        dev.ohs.fhir.model.r4
                            .CodeableConcept(),
                )
            assertTrue(repository.saveObservation(observation).isSuccess)
            val fetchedObs = repository.getObservationCatching("obs-1").getOrThrow()
            assertNotNull(fetchedObs)
            assertEquals("obs-1", fetchedObs.id)

            // Provenance
            val provenance =
                Provenance(
                    id = "prov-1",
                    target = emptyList(),
                    recorded =
                        dev.ohs.fhir.model.r4
                            .Instant("2026-09-17T12:00:00Z"),
                    agent = emptyList(),
                )
            assertTrue(repository.saveProvenance(provenance).isSuccess)
            val fetchedProv = repository.getProvenanceCatching("prov-1").getOrThrow()
            assertNotNull(fetchedProv)
            assertEquals("prov-1", fetchedProv.id)
        }

    /**
     * Tests reflection-free searchByParam and searchByToken functionality.
     */
    @Test
    fun testSearchByParamAndToken() =
        runTest {
            val patient =
                Patient(
                    id = "p-search",
                    name =
                        listOf(
                            HumanName(
                                family =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Searchable"),
                            ),
                        ),
                )
            repository.savePatient(patient)

            val searchResult = repository.searchByParam(PatientSearchParams.name, "Searchable")
            assertTrue(searchResult.isSuccess)
            val found = searchResult.getOrThrow()
            assertEquals(1, found.size)
            assertEquals("p-search", found[0].id)

            val tokenResult = repository.searchByToken<Patient>("name", null, "Searchable")
            assertTrue(tokenResult.isSuccess)
        }

    /**
     * Tests that corrupted or malformed database entities return Result.failure during typed retrieval.
     */
    @Test
    fun testCorruptedEntityReturnsFailure() =
        runTest {
            db.chartCamQueries.insertResource("bad-id", "Patient", "{ corrupted fhir json ...", "2026-09-17")
            val result = repository.getResourceTyped<Patient>("Patient", "bad-id")
            assertTrue(result.isFailure)
        }
}
