/**
 * @file FhirRepositoryJvmTest.kt
 * Contains declarations for FhirRepositoryJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test class for FhirRepository on JVM.
 */
class FhirRepositoryJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Sets up the test environment.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Tears down the test environment.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests practitioner CRUD operations.
     */
    @Test
    fun testPractitionerCrud() =
        runTest {
            val prac =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac_1"
                    }.build()

            repository.savePractitioner(prac)

            val fetched = repository.getPractitioner("prac_1")
            assertNotNull(fetched)
            assertEquals("prac_1", fetched.id)

            repository.deletePractitioner("prac_1")
            assertNull(repository.getPractitioner("prac_1"))
        }

    /**
     * Tests patient CRUD operations.
     */
    @Test
    fun testPatientCrud() =
        runTest {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat_1"
                    }.build()

            repository.savePatient(patient)

            val fetched = repository.getPatient("pat_1")
            assertNotNull(fetched)
            assertEquals("pat_1", fetched.id)

            val allPatients = repository.getAllPatients()
            assertEquals(1, allPatients.size)

            repository.deletePatient("pat_1")
            assertNull(repository.getPatient("pat_1"))
        }

    /**
     * Tests encounter CRUD operations.
     */
    @Test
    fun testEncounterCrud() =
        runTest {
            val encounter =
                Encounter
                    .Builder(
                        status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
                        `class` =
                            dev.ohs.fhir.model.r4.Coding
                                .Builder(),
                    ).apply {
                        id = "enc_1"
                    }.build()

            repository.saveEncounter(encounter)

            val fetched = repository.getEncounter("enc_1")
            assertNotNull(fetched)
            assertEquals("enc_1", fetched.id)

            repository.updateEncounterStatus("enc_1", "finished", "all good")
            val updated = repository.getEncounter("enc_1")
            assertEquals(Encounter.EncounterStatus.Finished, updated?.status?.value)
            assertNotNull(updated?.text?.div?.value)

            repository.deleteEncounter("enc_1")
            assertNull(repository.getEncounter("enc_1"))
        }

    /**
     * Tests database failures are handled or thrown.
     */
    @Test
    fun testDatabaseFailuresHandledOrThrown() =
        runTest {
            val patient = Patient.Builder().apply { id = "pat_fail" }.build()
            driver.close() // Close DB to simulate failure

            val saveResult = repository.savePatient(patient)
            kotlin.test.assertTrue(saveResult.isFailure, "Expected Result.failure when saving to a closed database")

            val getResult = repository.getPatientCatching("pat_fail")
            kotlin.test.assertTrue(getResult.isFailure, "Expected Result.failure when reading from a closed database")
        }

    /**
     * Tests quantity search with prefix modifiers (ge, le, gt, lt, eq).
     */
    @Test
    fun testQuantityPrefixAndRangeSearch() =
        runTest {
            val obs1 =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-high",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                    code =
                        dev.ohs.fhir.model.r4.CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system =
                                            dev.ohs.fhir.model.r4
                                                .Uri(value = "http://loinc.org"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "8480-6"),
                                    ),
                                ),
                        ),
                    value =
                        dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                            dev.ohs.fhir.model.r4.Quantity(
                                value =
                                    dev.ohs.fhir.model.r4.Decimal(
                                        value =
                                            dev.ohs.fhir.model.r4.FhirDecimal
                                                .fromString("145.0"),
                                    ),
                                unit =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "mm[Hg]"),
                                system =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = "http://unitsofmeasure.org"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = "mm[Hg]"),
                            ),
                        ),
                )
            val obs2 =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-normal",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                    code =
                        dev.ohs.fhir.model.r4.CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system =
                                            dev.ohs.fhir.model.r4
                                                .Uri(value = "http://loinc.org"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "8480-6"),
                                    ),
                                ),
                        ),
                    value =
                        dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                            dev.ohs.fhir.model.r4.Quantity(
                                value =
                                    dev.ohs.fhir.model.r4.Decimal(
                                        value =
                                            dev.ohs.fhir.model.r4.FhirDecimal
                                                .fromString("118.0"),
                                    ),
                                unit =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "mm[Hg]"),
                                system =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = "http://unitsofmeasure.org"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = "mm[Hg]"),
                            ),
                        ),
                )
            repository.saveObservation(obs1)
            repository.saveObservation(obs2)

            val geMatches =
                repository
                    .searchByParam<dev.ohs.fhir.model.r4.Observation>(
                        dev.ohs.fhir.model.r4.search.ObservationSearchParams.valueQuantity,
                        "ge140|http://unitsofmeasure.org|mm[Hg]",
                    ).getOrThrow()
            assertEquals(1, geMatches.size)
            assertEquals("obs-high", geMatches.first().id)

            val ltMatches =
                repository
                    .searchByParam<dev.ohs.fhir.model.r4.Observation>(
                        dev.ohs.fhir.model.r4.search.ObservationSearchParams.valueQuantity,
                        "lt120",
                    ).getOrThrow()
            assertEquals(1, ltMatches.size)
            assertEquals("obs-normal", ltMatches.first().id)
        }

    /**
     * Tests chained search query (e.g. searching Encounters by subject:Patient.family).
     */
    @Test
    fun testChainedReferenceSearch() =
        runTest {
            val patient =
                Patient(
                    id = "pat-chain-1",
                    name =
                        listOf(
                            dev.ohs.fhir.model.r4.HumanName(
                                family =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Armstrong"),
                                given =
                                    listOf(
                                        dev.ohs.fhir.model.r4
                                            .String(value = "Neil"),
                                    ),
                            ),
                        ),
                )
            val encounter =
                Encounter(
                    id = "enc-chain-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = "AMB"),
                            ),
                    subject =
                        dev.ohs.fhir.model.r4
                            .Reference(
                                reference =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Patient/pat-chain-1"),
                            ),
                )
            repository.savePatient(patient)
            repository.saveEncounter(encounter)

            val matches =
                repository
                    .searchByChainedParam<Encounter>(
                        referenceParam = "subject",
                        targetResourceType = "Patient",
                        targetParamName = "family",
                        targetParamValue = "Armstrong",
                    ).getOrThrow()

            assertEquals(1, matches.size)
            assertEquals("enc-chain-1", matches.first().id)
        }

    /**
     * Tests reverse-chaining query (e.g. finding Patients having an Observation with code 8480-6).
     */
    @Test
    fun testReverseChainingSearch() =
        runTest {
            val patient =
                Patient(
                    id = "pat-rev-1",
                    name =
                        listOf(
                            dev.ohs.fhir.model.r4.HumanName(
                                family =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Aldrin"),
                            ),
                        ),
                )
            val obs =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-rev-1",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                    code =
                        dev.ohs.fhir.model.r4.CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system =
                                            dev.ohs.fhir.model.r4
                                                .Uri(value = "http://loinc.org"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "8480-6"),
                                    ),
                                ),
                        ),
                    subject =
                        dev.ohs.fhir.model.r4
                            .Reference(
                                reference =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Patient/pat-rev-1"),
                            ),
                )
            repository.savePatient(patient)
            repository.saveObservation(obs)

            val matchedPatients =
                repository
                    .searchByReverseChain<Patient>(
                        targetResourceType = "Patient",
                        sourceResourceType = "Observation",
                        sourceRefParam = "subject",
                        sourceFilterParam = "code",
                        sourceFilterValue = "8480-6",
                    ).getOrThrow()

            assertEquals(1, matchedPatients.size)
            assertEquals("pat-rev-1", matchedPatients.first().id)
        }
}
