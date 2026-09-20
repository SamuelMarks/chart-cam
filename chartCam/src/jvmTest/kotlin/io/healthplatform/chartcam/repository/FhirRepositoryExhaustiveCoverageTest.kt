/**
 * @file FhirRepositoryExhaustiveCoverageTest.kt
 * Exhaustive unit tests for FhirRepository targeting 100% coverage.
 */

package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Id
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Meta
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Quantity
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.search.DocumentReferenceSearchParams
import dev.ohs.fhir.model.r4.search.EncounterSearchParams
import dev.ohs.fhir.model.r4.search.ObservationSearchParams
import dev.ohs.fhir.model.r4.search.PatientSearchParams
import dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirPatient
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Exhaustive coverage test suite for [FhirRepository].
 */
class FhirRepositoryExhaustiveCoverageTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository
    private lateinit var fileStorage: TestStorage

    /**
     * Initializes database and repository before each test.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
        fileStorage = TestStorage()
    }

    /**
     * Tears down driver after test completion.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests secondary constructor and basic driver initialization.
     */
    @Test
    fun testConstructorsAndPendingLocalChanges() =
        runTest {
            val repoFromDriver = FhirRepository(driver)
            assertNotNull(repoFromDriver)

            val repoFromFactory = FhirRepository(DatabaseDriverFactory())
            assertNotNull(repoFromFactory)

            val patient = createFhirPatient("p-sync-test", "Sync", "Patient", LocalDate(1990, 1, 1), "MRN-S")
            val saveSyncRes = repoFromDriver.saveResourceFromSync("Patient", "p-sync-test", patient)
            assertTrue(saveSyncRes.isSuccess)
            assertEquals(0, repoFromDriver.getPendingLocalChangesCount())

            // Save with local change tracking
            repoFromDriver.savePatient(patient, isLocalChange = true)
            assertEquals(1, repoFromDriver.getPendingLocalChangesCount())
            val changes = repoFromDriver.getAllLocalChanges()
            assertEquals(1, changes.size)
            repoFromDriver.deleteLocalChange(changes.first().id)
            assertEquals(0, repoFromDriver.getPendingLocalChangesCount())

            // Generic saveResource with default parameter
            val genericSave = repository.saveResource("Patient", "p-generic-default", patient)
            assertTrue(genericSave.isSuccess)

            // saveResource with versionId
            val patientWithVersion = patient.toBuilder().apply { meta = Meta(versionId = Id(value = "v2")).toBuilder() }.build()
            repository.saveResource("Patient", "p-versioned", patientWithVersion, isLocalChange = true)
            repository.deleteResource("Patient", "p-versioned", isLocalChange = true)
            repository.deleteResource("Patient", "p-sync-test", isLocalChange = false)

            // saveResource with unknown Resource type (fails serialization)
            val unknownResource =
                object : dev.ohs.fhir.model.r4.Resource() {
                    override val id: String = "anon-res"
                    override val meta: Meta? = null
                    override val implicitRules: Uri? = null
                    override val language: Code? = null

                    override fun toBuilder(): dev.ohs.fhir.model.r4.Resource.Builder = Patient.Builder()
                }
            val saveUnknownRes = repository.saveResource("Unknown", "anon-res", unknownResource)
            assertTrue(saveUnknownRes.isFailure)

            val failTypedSave = repository.saveResourceTyped("Unknown", "u1", unknownResource, true)
            assertTrue(failTypedSave.isFailure)

            val patientNullVerVal = patient.toBuilder().apply { meta = Meta(versionId = Id(value = null)).toBuilder() }.build()
            repository.saveResource("Patient", "p-null-ver", patientNullVerVal, isLocalChange = true)

            db.chartCamQueries.insertResource("invalid-res", "Patient", "{not-valid-json}", "2026-01-01")
            assertNull(repository.getResource("Patient", "invalid-res"))

            val refNullVal = Reference(reference = FhirString(value = null))
            assertEquals("", repository.extractPatientReferenceId(refNullVal))
            assertEquals("", repository.extractEncounterReferenceId(refNullVal))
            assertFalse(repository.organizationMatchesPractitioner(refNullVal, "prac-1"))

            val refNullRef = Reference(reference = null)
            assertEquals("", repository.extractPatientReferenceId(refNullRef))
            assertEquals("", repository.extractEncounterReferenceId(refNullRef))
            assertFalse(repository.organizationMatchesPractitioner(refNullRef, "prac-1"))

            val refWithVal = Reference(reference = FhirString(value = "Patient/p1"))
            assertEquals("p1", repository.extractPatientReferenceId(refWithVal))
            assertEquals("e1", repository.extractEncounterReferenceId(Reference(reference = FhirString(value = "Encounter/e1"))))
            assertTrue(repository.organizationMatchesPractitioner(Reference(reference = FhirString(value = "prac-1")), "prac-1"))

            val devWithNullVersion =
                Device(
                    id = "dev-ver-null",
                ).toBuilder().apply { meta = Meta(versionId = Id(value = null)).toBuilder() }.build()
            repository.saveDevice(devWithNullVersion, isLocalChange = true)

            val failToken = repository.searchByToken<UnregisteredCustomResource>("tag", null, "val")
            assertTrue(failToken.isFailure)
            val failTag = repository.searchByTag<UnregisteredCustomResource>("val")
            assertTrue(failTag.isFailure)
            val failProfile = repository.searchByProfile<UnregisteredCustomResource>("val")
            assertTrue(failProfile.isFailure)
            val failDate = repository.searchByDatePrefixCatching<UnregisteredCustomResource>("date", SearchPrefix.EQ, "2026-01-01")
            assertTrue(failDate.isFailure)
            val failQuantity = repository.searchByQuantityCatching<UnregisteredCustomResource>("value-quantity", SearchPrefix.EQ, "100")
            assertTrue(failQuantity.isFailure)
            val customParam =
                dev.ohs.fhir.model.r4.search.SearchParam<UnregisteredCustomResource, FhirString>(
                    name = "custom-param",
                    type = dev.ohs.fhir.model.r4.terminologies.SearchParamType.String,
                    expression = "custom",
                    target = emptyList(),
                    extractor = { emptyList() },
                )
            val failParam = repository.searchByParam<UnregisteredCustomResource>(customParam, "Alice")
            assertTrue(failParam.isFailure)
            val failChained =
                repository.searchByChainedParam<UnregisteredCustomResource>(
                    "subject",
                    "Patient",
                    "family",
                    "Smith",
                )
            assertTrue(failChained.isFailure)

            // saveResourceFromSync failure with closed driver
            val closedDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(closedDriver)
            val closedRepo = FhirRepository(closedDriver)
            closedDriver.close()
            val syncFail = closedRepo.saveResourceFromSync("Patient", "p-fail", patient)
            assertTrue(syncFail.isFailure)
            val failCompound =
                closedRepo.searchCompoundCatching<Patient>(
                    listOf(SearchCriterion(PatientSearchParams.name, "Alice")),
                )
            assertTrue(failCompound.isFailure)
            val failEncObs1 = closedRepo.searchEncountersWithObservationsCatching("Patient/p1")
            assertTrue(failEncObs1.isFailure)

            val closedDriver2 = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(closedDriver2)
            val testRepoPartial =
                object : FhirRepository(closedDriver2) {
                    override suspend fun searchEncountersByParam(
                        param: dev.ohs.fhir.model.r4.search.SearchParam<Encounter, *>,
                        value: String,
                    ): Result<List<Encounter>> = Result.success(emptyList())
                }
            closedDriver2.close()
            val failEncObs2 = testRepoPartial.searchEncountersWithObservationsCatching("Patient/p1")
            assertTrue(failEncObs2.isFailure)

            // getAllPatientsCatching default arguments
            val defaultAll = repository.getAllPatientsCatching()
            assertTrue(defaultAll.isSuccess)
        }

    /**
     * Tests SearchPrefix helper logic and edge cases.
     */
    @Test
    fun testSearchPrefixFromValue() {
        assertEquals(SearchPrefix.EQ to "raw", SearchPrefix.fromValue("raw"))
        assertEquals(SearchPrefix.GE to "100", SearchPrefix.fromValue("ge100"))
        assertEquals(SearchPrefix.LE to "50", SearchPrefix.fromValue("le50"))
        assertEquals(SearchPrefix.GT to "10", SearchPrefix.fromValue("gt10"))
        assertEquals(SearchPrefix.LT to "5", SearchPrefix.fromValue("lt5"))
        assertEquals(SearchPrefix.NE to "0", SearchPrefix.fromValue("ne0"))
        // Edge case: string starts with prefix but length is not greater
        assertEquals(SearchPrefix.EQ to "ge", SearchPrefix.fromValue("ge"))
    }

    /**
     * Tests matchesQuantityPrefix across all comparison operators.
     */
    @Test
    fun testMatchesQuantityPrefix() {
        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.GE, 10.0, 5.0))
        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.GE, 5.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.GE, 4.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.LE, 5.0, 5.0))
        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.LE, 4.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.LE, 6.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.GT, 6.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.GT, 5.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.LT, 4.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.LT, 5.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.NE, 4.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.NE, 5.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.EQ, 5.0, 5.0))
        assertFalse(repository.matchesQuantityPrefix(SearchPrefix.EQ, 4.0, 5.0))

        assertTrue(repository.matchesQuantityPrefix(SearchPrefix.SA, 5.0, 5.0))
    }

    /**
     * Tests searchPatients and getAllPatients with practitioner scoping.
     */
    @Test
    fun testPatientScopingAndSearch() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val p1 =
                createFhirPatient("p-sc-1", "John", "Doe", LocalDate(1980, 1, 1), "MRN-1")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply { reference = FhirString.Builder().apply { value = "Practitioner/prac-1" } }
                    }.build()
            val p2 =
                createFhirPatient("p-sc-2", "Jane", "Smith", LocalDate(1985, 2, 2), "MRN-2")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply { reference = FhirString.Builder().apply { value = "Practitioner/other" } }
                    }.build()
            val pNoOrg = createFhirPatient("p-no-org", "Alice", "Wonder", LocalDate(1990, 3, 3), "MRN-3")
            repository.savePatient(p1)
            repository.savePatient(p2)
            repository.savePatient(pNoOrg)

            val enc =
                Encounter(
                    id = "enc-sc-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-sc-1")),
                    participant =
                        listOf(
                            Encounter.Participant(individual = Reference(reference = FhirString(value = "Practitioner/prac-1"))),
                        ),
                )
            val encNoSubject =
                Encounter(
                    id = "enc-no-subj",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                    participant =
                        listOf(
                            Encounter.Participant(individual = Reference(reference = FhirString(value = "Practitioner/prac-1"))),
                        ),
                )
            repository.saveEncounter(enc)
            repository.saveEncounter(encNoSubject)

            // Scoped query: showAll = false, practitionerId = "prac-1"
            val scopedPatients = repository.searchPatients("Doe", showAll = false, practitionerId = "prac-1")
            assertEquals(1, scopedPatients.size)
            assertEquals("p-sc-1", scopedPatients.first().id)

            // Unscoped query: showAll = true
            val unscoped = repository.searchPatients("Doe", showAll = true)
            assertEquals(1, unscoped.size)

            val unscopedMrn = repository.searchPatients("MRN-1", showAll = true)
            assertEquals(1, unscopedMrn.size)

            // Scoped query with null practitionerId
            val scopedNullPrac = repository.searchPatients("Doe", showAll = false, practitionerId = null)
            assertEquals(1, scopedNullPrac.size)

            val scopedAll = repository.getAllPatients(showAll = false, practitionerId = "prac-1")
            assertEquals(1, scopedAll.size)

            val pNoId =
                Patient(
                    id = null,
                    name =
                        listOf(
                            dev.ohs.fhir.model.r4
                                .HumanName(family = FhirString(value = "Doe")),
                        ),
                )
            repository.savePatient(pNoId)
            repository.getAllPatients(showAll = false, practitionerId = "prac-1")

            val pMatchedOrg =
                createFhirPatient("p-sc-org", "Jane", "Doe", LocalDate(1980, 1, 1), "MRN-ORG")
                    .toBuilder()
                    .apply {
                        managingOrganization =
                            Reference.Builder().apply { reference = FhirString.Builder().apply { value = "Practitioner/prac-1" } }
                    }.build()
            repository.savePatient(pMatchedOrg)
            val scopedOrgMatch = repository.searchPatients("Doe", showAll = false, practitionerId = "prac-1")
            assertTrue(scopedOrgMatch.any { it.id == "p-sc-org" })

            val allCatching = repository.getAllPatientsCatching(showAll = true)
            assertTrue(allCatching.isSuccess)

            val getResourceCatchingRes = repository.getResourceCatching("Patient", "p-sc-1")
            assertTrue(getResourceCatchingRes.isSuccess)
            assertNotNull(getResourceCatchingRes.getOrThrow())

            // Test null enc branch with invalid JSON
            db.chartCamQueries.insertResource("enc-corrupt", "Encounter", "{invalid-json}", "2026-01-01")
            db.chartCamQueries.insertReferenceIndex("Encounter", "enc-corrupt", "practitioner", "prac-1")
            repository.getAllPatients(showAll = false, practitionerId = "prac-1")
            repository.searchPatients("Doe", showAll = false, practitionerId = "prac-1")
        }

    /**
     * Tests quantity search with various prefixes and system matching.
     */
    @Test
    fun testSearchByQuantityVariants() =
        runTest {
            val obs1 =
                Observation(
                    id = "obs-q-1",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                    value =
                        Observation.Value.Quantity(
                            Quantity(
                                value = Decimal(value = FhirDecimal.fromString("120")),
                                system = Uri(value = "http://unitsofmeasure.org"),
                                code = Code(value = "mm[Hg]"),
                            ),
                        ),
                )
            val obs2 =
                Observation(
                    id = "obs-q-2",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                    value =
                        Observation.Value.Quantity(
                            Quantity(
                                value = Decimal(value = FhirDecimal.fromString("140")),
                                system = Uri(value = "http://unitsofmeasure.org"),
                                code = Code(value = "mm[Hg]"),
                            ),
                        ),
                )
            val obsOtherSys =
                Observation(
                    id = "obs-q-other",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                    value =
                        Observation.Value.Quantity(
                            Quantity(
                                value = Decimal(value = FhirDecimal.fromString("130")),
                                system = Uri(value = "http://other-units.org"),
                                code = Code(value = "mm[Hg]"),
                            ),
                        ),
                )
            val obsNoValue =
                Observation(
                    id = "obs-q-noval",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                    value = null,
                )
            repository.saveObservation(obs1)
            repository.saveObservation(obs2)
            repository.saveObservation(obsOtherSys)
            repository.saveObservation(obsNoValue)

            // EQ search with system
            val eqRes =
                repository
                    .searchByQuantityCatching<Observation>(
                        "value-quantity",
                        SearchPrefix.EQ,
                        "120|http://unitsofmeasure.org",
                    ).getOrThrow()
            assertEquals(1, eqRes.size)

            // EQ search without system
            val eqNoSys = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.EQ, "120").getOrThrow()
            assertEquals(1, eqNoSys.size)

            // GT search
            val gtRes = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.GT, "130").getOrThrow()
            assertEquals(1, gtRes.size)
            assertEquals("obs-q-2", gtRes.first().id)

            // LE search with system
            val leRes =
                repository
                    .searchByQuantityCatching<Observation>(
                        "value-quantity",
                        SearchPrefix.LE,
                        "120|http://unitsofmeasure.org",
                    ).getOrThrow()
            assertEquals(1, leRes.size)

            // LT search
            val ltRes = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.LT, "130").getOrThrow()
            assertEquals(1, ltRes.size)

            // NE search
            val neRes = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.NE, "120").getOrThrow()
            assertTrue(neRes.size >= 1)

            // Non-numeric target value
            val nonNumRes = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.GT, "invalid-num").getOrThrow()
            assertEquals(4, nonNumRes.size)

            // Blank system query
            val blankSysRes = repository.searchByQuantityCatching<Observation>("value-quantity", SearchPrefix.EQ, "120|   ").getOrThrow()
            assertEquals(1, blankSysRes.size)

            // extractObservationQuantity direct branch tests
            assertNull(repository.extractObservationQuantity(Patient()))
            val obsNullVal =
                Observation(status = Enumeration(value = Observation.ObservationStatus.Final), code = CodeableConcept(), value = null)
            assertNull(repository.extractObservationQuantity(obsNullVal))
            val obsNullSys =
                Observation(
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(),
                    value = Observation.Value.Quantity(Quantity(value = Decimal(value = FhirDecimal.fromString("10")), system = null)),
                )
            val pairNullSys = repository.extractObservationQuantity(obsNullSys)
            assertNotNull(pairNullSys)
            assertNull(pairNullSys.second)
            val obsNullSysVal =
                Observation(
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(),
                    value =
                        Observation.Value.Quantity(
                            Quantity(value = Decimal(value = FhirDecimal.fromString("10")), system = Uri(value = null)),
                        ),
                )
            val pairNullSysVal = repository.extractObservationQuantity(obsNullSysVal)
            assertNotNull(pairNullSysVal)
            assertNull(pairNullSysVal.second)
        }

    /**
     * Tests date search across all range prefixes (GE, SA, LE, EB, GT, LT, NE, EQ).
     */
    @Test
    fun testSearchByDatePrefixes() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val enc1 =
                Encounter(
                    id = "enc-d-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    period =
                        dev.ohs.fhir.model.r4.Period(
                            start =
                                dev.ohs.fhir.model.r4
                                    .DateTime(value = FhirDateTime.fromString("2026-05-10T10:00:00Z")),
                        ),
                )
            repository.saveEncounter(enc1)

            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.GE, "2026-05-01").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.SA, "2026-05-01").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.LE, "2026-05-20").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.EB, "2026-05-20").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.GT, "2026-05-01").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.LT, "2026-05-20").getOrThrow().isNotEmpty())
            assertTrue(repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.NE, "2026-01-01").getOrThrow().isNotEmpty())
            assertTrue(
                repository.searchByDatePrefixCatching<Encounter>("date", SearchPrefix.EQ, "2026-05-10T10:00:00Z").getOrThrow().isNotEmpty(),
            )
        }

    /**
     * Tests chained search and reverse-chaining queries.
     */
    @Test
    fun testChainedAndReverseChaining() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val patient = createFhirPatient("p-chain", "Bob", "Smith", LocalDate(1985, 1, 1), "MRN-CHAIN")
            repository.savePatient(patient)

            val enc =
                Encounter(
                    id = "enc-chain",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-chain")),
                )
            repository.saveEncounter(enc)

            val obs =
                Observation(
                    id = "obs-chain",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "GLUCOSE")))),
                    subject = Reference(reference = FhirString(value = "Patient/p-chain")),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-chain")),
                )
            val obsNoEnc =
                Observation(
                    id = "obs-no-enc",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "GLUCOSE")))),
                    subject = Reference(reference = FhirString(value = "Patient/p-chain")),
                    encounter = null,
                )
            val obsNoSubj =
                Observation(
                    id = "obs-no-subj",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "GLUCOSE")))),
                    subject = null,
                )
            val encNoSubj =
                Encounter(
                    id = "enc-no-subj-chain",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                )
            repository.saveObservation(obs)
            repository.saveObservation(obsNoEnc)
            repository.saveObservation(obsNoSubj)
            repository.saveEncounter(encNoSubj)

            // Chained query: Encounters where subject.family is Smith
            val chained = repository.searchByChainedParam<Encounter>("subject", "Patient", "family", "Smith").getOrThrow()
            assertEquals(1, chained.size)
            assertEquals("enc-chain", chained.first().id)

            // Test corrupt encounter in chained query
            db.chartCamQueries.insertResource("enc-corrupt-chain", "Encounter", "{invalid-json}", "2026-01-01")
            db.chartCamQueries.insertReferenceIndex("Encounter", "enc-corrupt-chain", "subject", "Patient/p-chain")
            val chainedWithCorrupt = repository.searchByChainedParam<Encounter>("subject", "Patient", "family", "Smith").getOrThrow()
            assertEquals(1, chainedWithCorrupt.size)

            // Chained query: no matches
            val emptyChained = repository.searchByChainedParam<Encounter>("subject", "Patient", "family", "NonExistent").getOrThrow()
            assertTrue(emptyChained.isEmpty())

            db.chartCamQueries.insertResource("obs-ref-target", "Observation", "{}", "2026-01-01")
            db.chartCamQueries.insertReferenceIndex("Observation", "obs-ref-target", "subject", "obs-chain")

            db.chartCamQueries.insertResource("p-corrupt", "Observation", "{}", "2026-01-01")
            db.chartCamQueries.insertResource("p-corrupt", "Patient", "{invalid-json}", "2026-01-01")
            db.chartCamQueries.insertReferenceIndex("Observation", "p-corrupt", "subject", "obs-chain")

            // Reverse chain: Patients having Observation with code GLUCOSE
            val reverse = repository.searchByReverseChain<Patient>("Patient", "Observation", "subject", "code", "GLUCOSE").getOrThrow()
            assertEquals(1, reverse.size)
            assertEquals("p-chain", reverse.first().id)

            // Reverse chain with Encounter as referring resource
            val reverseEnc = repository.searchByReverseChain<Patient>("Patient", "Encounter", "subject", "_id", "enc-chain").getOrThrow()
            assertEquals(1, reverseEnc.size)

            val reverseEncNoSubj =
                repository
                    .searchByReverseChain<Patient>(
                        "Patient",
                        "Encounter",
                        "subject",
                        "_id",
                        "enc-no-subj-chain",
                    ).getOrThrow()
            assertTrue(reverseEncNoSubj.isEmpty())

            val reverseObsNoSubj =
                repository
                    .searchByReverseChain<Patient>(
                        "Patient",
                        "Observation",
                        "subject",
                        "_id",
                        "obs-no-subj",
                    ).getOrThrow()
            assertTrue(reverseObsNoSubj.isEmpty())

            // Reverse chain with Device (else -> {} branch)
            val dev = Device(id = "dev-chain")
            repository.saveDevice(dev)
            val reverseOther = repository.searchByReverseChain<Patient>("Patient", "Device", "patient", "_id", "dev-chain").getOrThrow()
            assertTrue(reverseOther.isEmpty())

            // Reverse chain: no matches
            val emptyReverse =
                repository
                    .searchByReverseChain<Patient>(
                        "Patient",
                        "Observation",
                        "subject",
                        "code",
                        "CHOLESTEROL",
                    ).getOrThrow()
            assertTrue(emptyReverse.isEmpty())

            // Encounters with Observations
            val encObsMap = repository.searchEncountersWithObservationsCatching("p-chain").getOrThrow()
            assertEquals(1, encObsMap.size)
            assertEquals(1, encObsMap.values.first().size)
        }

    /**
     * Tests compound multi-criterion search and tag/profile queries.
     */
    @Test
    fun testCompoundAndMetadataSearch() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val patient =
                Patient(
                    id = "p-compound",
                    meta =
                        Meta(
                            profile =
                                listOf(
                                    dev.ohs.fhir.model.r4
                                        .Canonical(value = "http://example.com/TestProfile"),
                                ),
                            tag = listOf(Coding(code = Code(value = "URGENT"), system = Uri(value = "http://tags.com"))),
                        ),
                    name =
                        listOf(
                            dev.ohs.fhir.model.r4
                                .HumanName(family = FhirString(value = "CompoundFamily")),
                        ),
                )
            val patientNoId =
                Patient(
                    id = null,
                    name =
                        listOf(
                            dev.ohs.fhir.model.r4
                                .HumanName(family = FhirString(value = "CompoundFamily")),
                        ),
                )
            repository.savePatient(patient)
            repository.savePatient(patientNoId)

            db.chartCamQueries.insertResource(
                "patient-raw-null-id",
                "Patient",
                "{\"resourceType\":\"Patient\",\"name\":[{\"family\":\"CompoundFamily\"}]}",
                "2026-01-01",
            )
            db.chartCamQueries.insertStringIndex("Patient", "patient-raw-null-id", "family", "CompoundFamily")
            db.chartCamQueries.insertStringIndex("Patient", "patient-raw-null-id", "name", "CompoundFamily")

            // Tag & Profile
            val tagMatches = repository.searchByTag<Patient>("URGENT", "http://tags.com").getOrThrow()
            assertEquals(1, tagMatches.size)

            val profileMatches = repository.searchByProfile<Patient>("http://example.com/TestProfile").getOrThrow()
            assertEquals(1, profileMatches.size)

            // Compound search with matches
            val criteria =
                listOf(
                    SearchCriterion(PatientSearchParams.family, "CompoundFamily"),
                    SearchCriterion(PatientSearchParams.name, "CompoundFamily"),
                )
            val compoundMatches = repository.searchCompoundCatching(criteria).getOrThrow()
            assertEquals(1, compoundMatches.size)

            // Empty criteria
            val compoundEmptyCriteria = repository.searchCompoundCatching<Patient>(emptyList()).getOrThrow()
            assertTrue(compoundEmptyCriteria.isEmpty())

            // Compound search with non-intersecting criterion
            val criteriaNoMatch =
                listOf(
                    SearchCriterion(PatientSearchParams.family, "CompoundFamily"),
                    SearchCriterion(PatientSearchParams.family, "WrongFamily"),
                )
            val compoundEmpty = repository.searchCompoundCatching(criteriaNoMatch).getOrThrow()
            assertTrue(compoundEmpty.isEmpty())

            // Strongly typed searchParam helpers
            assertEquals(3, repository.searchPatientsByParam(PatientSearchParams.family, "CompoundFamily").getOrThrow().size)

            val enc =
                Encounter(
                    id = "enc-date-test",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    period =
                        dev.ohs.fhir.model.r4.Period(
                            start =
                                dev.ohs.fhir.model.r4
                                    .DateTime(value = FhirDateTime.fromString("2026-05-10T10:00:00Z")),
                        ),
                )
            repository.saveEncounter(enc)
            assertEquals(1, repository.searchByParam(EncounterSearchParams.date, "2026-05-10T10:00:00Z").getOrThrow().size)

            val obs =
                Observation(
                    id = "obs-token-test",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                )
            repository.saveObservation(obs)
            assertEquals(1, repository.searchByParam(ObservationSearchParams.code, "bp").getOrThrow().size)
            assertEquals(1, repository.searchByToken<Observation>("code", null, "bp").getOrThrow().size)

            // Number search param
            val numParam =
                dev.ohs.fhir.model.r4.search.SearchParam<Patient, dev.ohs.fhir.model.r4.Integer>(
                    "customNum",
                    dev.ohs.fhir.model.r4.terminologies.SearchParamType.Number,
                    "",
                    emptyList(),
                ) { emptyList() }
            val numRes = repository.searchByParam(numParam, "42").getOrThrow()
            assertTrue(numRes.isEmpty())
        }

    /**
     * Tests encounter status updates across all enum states and notes.
     */
    @Test
    fun testUpdateEncounterStatusBranches() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val enc = Encounter(id = "enc-status-test", status = Enumeration(value = Encounter.EncounterStatus.Planned), `class` = encClass)
            repository.saveEncounter(enc)

            val statuses = listOf("planned", "arrived", "triaged", "in-progress", "onleave", "finished", "cancelled", "unknown-status")
            for (st in statuses) {
                repository.updateEncounterStatus("enc-status-test", st, notes = "Note for $st")
                val updated = repository.getEncounter("enc-status-test")
                assertNotNull(updated)
            }

            // Update with null notes
            repository.updateEncounterStatus("enc-status-test", "finished", notes = null)

            // Update non-existent encounter
            repository.updateEncounterStatus("non-existent-enc", "finished")
            assertNull(repository.getEncounter("non-existent-enc"))
        }

    /**
     * Tests cascading deletion of encounters and patients with attached photos.
     */
    @Test
    fun testCascadeDeletionWithFiles() =
        runTest {
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val patient = createFhirPatient("p-del-test", "Delete", "Me", LocalDate(1980, 1, 1), "MRN-DEL")
            repository.savePatient(patient)

            val enc =
                Encounter(
                    id = "enc-del-test",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-del-test")),
                )
            repository.saveEncounter(enc)

            fileStorage.saveImage("test-photo.jpg", byteArrayOf(1, 2, 3))
            val doc =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-del-test",
                        patientId = "p-del-test",
                        encounterId = "enc-del-test",
                        dateStr = "2026-09-18T10:00:00Z",
                        desc = "Photo",
                        mime = "image/jpeg",
                        urlPath = "test-photo.jpg",
                    ),
                )
            val docEmptyContent =
                DocumentReference(
                    id = "doc-empty-content",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context =
                        DocumentReference.Context(
                            encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-del-test"))),
                        ),
                    content = emptyList(),
                )
            val docNullUrl =
                DocumentReference(
                    id = "doc-null-url",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context =
                        DocumentReference.Context(
                            encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-del-test"))),
                        ),
                    content =
                        listOf(
                            DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4
                                        .Attachment(url = null),
                            ),
                        ),
                )
            repository.saveDocumentReference(doc)
            repository.saveDocumentReference(docEmptyContent)
            repository.saveDocumentReference(docNullUrl)

            val qr =
                QuestionnaireResponse(
                    id = "qr-del-test",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-del-test")),
                    subject = Reference(reference = FhirString(value = "Patient/p-del-test")),
                )
            val qrPatientOnly =
                QuestionnaireResponse(
                    id = "qr-patient-only",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = null,
                    subject = Reference(reference = FhirString(value = "Patient/p-del-test")),
                )
            val encNoId =
                Encounter(
                    id = null,
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = Reference(reference = FhirString(value = "Patient/p-del-test")),
                )
            val qrNoIdPatient =
                QuestionnaireResponse(
                    id = null,
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    subject = Reference(reference = FhirString(value = "Patient/p-del-test")),
                )
            val docNoId =
                DocumentReference(
                    id = null,
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context =
                        DocumentReference.Context(
                            encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-del-test"))),
                        ),
                    content = emptyList(),
                )
            repository.saveEncounter(encNoId)
            repository.saveQuestionnaireResponse(qr)
            repository.saveQuestionnaireResponse(qrPatientOnly)
            repository.saveQuestionnaireResponse(qrNoIdPatient)
            repository.saveDocumentReference(docNoId)

            assertEquals(
                4,
                repository.searchDocumentReferencesByParam(DocumentReferenceSearchParams.encounter, "enc-del-test").getOrThrow().size,
            )
            assertEquals(2, repository.searchEncountersByParam(EncounterSearchParams.subject, "p-del-test").getOrThrow().size)

            // Delete encounter without storage
            val delEncRes = repository.deleteEncounter("enc-del-test", null)
            assertTrue(delEncRes.isSuccess)

            // Delete patient cascades to QR without encounter and patient record
            val delRes = repository.deletePatient("p-del-test", fileStorage)
            assertTrue(delRes.isSuccess)
            assertNull(repository.getPatient("p-del-test"))
            assertNull(repository.getQuestionnaireResponse("qr-patient-only"))
        }

    /**
     * Tests Media resource CRUD operations.
     */
    @Test
    fun testMediaCrud() =
        runTest {
            val media =
                Media(
                    id = "media-1",
                    status = Enumeration(value = Media.EventStatus.Completed),
                    content =
                        dev.ohs.fhir.model.r4
                            .Attachment(
                                url =
                                    dev.ohs.fhir.model.r4
                                        .Url(value = "test.jpg"),
                            ),
                    subject = Reference(reference = FhirString(value = "Patient/p-media")),
                )
            val saveRes = repository.saveMedia(media)
            assertTrue(saveRes.isSuccess)

            val fetchedCatching = repository.getMediaCatching("media-1")
            assertTrue(fetchedCatching.isSuccess)
            assertNotNull(fetchedCatching.getOrThrow())

            val fetched = repository.getMedia("media-1")
            assertNotNull(fetched)
            assertEquals("media-1", fetched.id)

            val allMedia = repository.getAllMedia()
            assertEquals(1, allMedia.size)

            val forPatient = repository.getMediaForPatient("p-media").getOrThrow()
            assertEquals(1, forPatient.size)

            val forPatientWithPrefix = repository.getMediaForPatient("Patient/p-media").getOrThrow()
            assertEquals(1, forPatientWithPrefix.size)

            val delRes = repository.deleteMedia("media-1")
            assertTrue(delRes.isSuccess)
            assertNull(repository.getMedia("media-1"))
        }

    /**
     * Tests Provenance, Device, Questionnaire, and Observation querying helpers.
     */
    @Test
    fun testAdditionalResourceQueries() =
        runTest {
            val device = Device(id = "dev-1")
            repository.saveDevice(device)
            assertEquals(1, repository.getAllDevices().size)
            assertNotNull(repository.getDevice("dev-1"))

            val q = Questionnaire(id = "q-1", status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
            repository.saveQuestionnaire(q, isLocalChange = false)
            assertEquals(1, repository.getAllQuestionnaires().size)
            assertNotNull(repository.getQuestionnaire("q-1"))

            val prov =
                Provenance(
                    id = "prov-1",
                    target = listOf(Reference(reference = FhirString(value = "Patient/p1"))),
                    recorded = Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                    agent = emptyList(),
                )
            repository.saveProvenance(prov, encounterId = null, isLocalChange = false)
            val prov2 =
                Provenance(
                    id = "prov-2",
                    target = listOf(Reference(reference = FhirString(value = "Patient/p1"))),
                    recorded = Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                    agent = emptyList(),
                )
            repository.saveProvenance(prov2, encounterId = "enc-1", isLocalChange = false)
            val provNoId =
                Provenance(
                    id = null,
                    target = emptyList(),
                    recorded = Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                    agent = emptyList(),
                )
            repository.saveProvenance(provNoId, encounterId = "enc-1", isLocalChange = false)
            assertEquals(3, repository.getAllProvenances().size)
            assertEquals(1, repository.getProvenancesForEncounter("enc-1").size)

            val obs =
                Observation(
                    id = "obs-helper",
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "test")))),
                    subject = Reference(reference = FhirString(value = "Patient/p-obs")),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-obs")),
                )
            repository.saveObservation(obs)
            assertNotNull(repository.getObservation("obs-helper"))
            assertEquals(1, repository.getObservationsForPatient("p-obs").size)
            assertEquals(1, repository.getObservationsForPatient("Patient/p-obs").size)
            assertEquals(1, repository.getObservationsForEncounter("enc-obs").size)
            assertEquals(1, repository.getObservationsForEncounter("Encounter/enc-obs").size)
        }

    /**
     * Tests failure and null branches on closed driver and missing identifiers.
     */
    @Test
    fun testClosedDriverAndNullBranches() =
        runTest {
            val closedDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(closedDriver)
            val closedRepo = FhirRepository(closedDriver)
            closedDriver.close()

            assertNull(closedRepo.getPractitioner("p1"))
            assertNull(closedRepo.getPatient("p1"))
            assertNull(closedRepo.getEncounter("e1"))
            assertNull(closedRepo.getDocumentReference("d1"))
            assertNull(closedRepo.getQuestionnaireResponse("q1"))
            assertNull(closedRepo.getDevice("dev1"))
            assertNull(closedRepo.getObservation("obs1"))
            assertNull(closedRepo.getQuestionnaire("q1"))
            assertNull(closedRepo.getMedia("m1"))
            assertNull(closedRepo.decodeResource<Patient>("invalid json"))
            assertNull(closedRepo.getResource("Patient", "p1"))

            // Save provenance on closed driver
            val prov =
                Provenance(
                    id = "prov-fail",
                    target = listOf(Reference(reference = FhirString(value = "Patient/p1"))),
                    recorded = Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                    agent = emptyList(),
                )
            val provFail = closedRepo.saveProvenance(prov)
            assertTrue(provFail.isFailure)

            // Blank MRN
            assertNull(repository.getPatientByMrn("   "))
            assertNull(repository.getPatientByMrn(""))

            // Save resources with null ID
            val encClass = Coding(code = Code(value = "AMB"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
            val pracNullId = Practitioner(id = null, active = FhirBoolean(value = true))
            assertTrue(repository.savePractitioner(pracNullId).isSuccess)

            val devNullId = Device(id = null)
            assertTrue(repository.saveDevice(devNullId).isSuccess)

            val obsNullId =
                Observation(
                    id = null,
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = CodeableConcept(coding = listOf(Coding(code = Code(value = "bp")))),
                )
            assertTrue(repository.saveObservation(obsNullId).isSuccess)

            val qNullId =
                Questionnaire(id = null, status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
            assertTrue(repository.saveQuestionnaire(qNullId).isSuccess)

            val qrNullId =
                QuestionnaireResponse(id = null, status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
            assertTrue(repository.saveQuestionnaireResponse(qrNullId).isSuccess)

            val mediaNullId =
                Media(
                    id = null,
                    status = Enumeration(value = Media.EventStatus.Completed),
                    content =
                        dev.ohs.fhir.model.r4
                            .Attachment(
                                url =
                                    dev.ohs.fhir.model.r4
                                        .Url(value = "test.jpg"),
                            ),
                )
            assertTrue(repository.saveMedia(mediaNullId).isSuccess)

            // Delete encounter with null ID inside photos and responses
            val encForNullChild =
                Encounter(id = "enc-null-child", status = Enumeration(value = Encounter.EncounterStatus.Finished), `class` = encClass)
            repository.saveEncounter(encForNullChild)
            val docNullId =
                DocumentReference(
                    id = null,
                    status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                    context =
                        DocumentReference.Context(
                            encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-null-child"))),
                        ),
                    content = emptyList(),
                )
            val qrChildNullId =
                QuestionnaireResponse(
                    id = null,
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-null-child")),
                )
            repository.saveDocumentReference(docNullId)
            repository.saveQuestionnaireResponse(qrChildNullId)
            val delChildRes = repository.deleteEncounter("enc-null-child", null)
            assertTrue(delChildRes.isSuccess)
        }

    /**
     * Test file storage for tracking deletions.
     */
    class TestStorage : FileStorage {
        val deleted = mutableListOf<String>()

        override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

        override fun readImage(path: String): ByteArray = ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> {
            deleted.add(path)
            return Result.success(Unit)
        }

        override fun clearCache() {}
    }

    /**
     * Non-serializable resource to trigger canonical name resolution failure.
     */
    class UnregisteredCustomResource : dev.ohs.fhir.model.r4.Resource() {
        override val id: String? = null
        override val meta: Meta? = null
        override val implicitRules: Uri? = null
        override val language: Code? = null

        override fun toBuilder(): dev.ohs.fhir.model.r4.Resource.Builder = Patient.Builder()
    }
}
