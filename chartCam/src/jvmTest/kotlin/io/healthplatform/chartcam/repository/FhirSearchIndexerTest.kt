/**
 * @file FhirSearchIndexerTest.kt
 * Unit tests for FhirSearchIndexer verifying SearchParam-based indexing into SQLDelight.
 */

package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.search.PatientSearchParams
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite verifying SearchParam indexing into SQLDelight tables via [FhirSearchIndexer].
 */
class FhirSearchIndexerTest {
    private lateinit var db: ChartCamDatabase

    /**
     * Initializes an in-memory SQLDelight database for test isolation.
     */
    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
    }

    /**
     * Verifies that a [Patient] is correctly indexed across string, token, date, and reference tables.
     */
    @Test
    fun testIndexPatient() =
        runTest {
            val patient =
                Patient(
                    id = "p-100",
                    name =
                        listOf(
                            HumanName(
                                family = FhirString(value = "Smith"),
                                given = listOf(FhirString(value = "Alice")),
                            ),
                        ),
                    identifier =
                        listOf(
                            Identifier(
                                system = Uri(value = "http://hospital.org/mrn"),
                                value = FhirString(value = "MRN-100"),
                            ),
                        ),
                    gender = Enumeration(value = AdministrativeGender.Female),
                    birthDate = Date(value = FhirDate.fromString("1992-04-15")),
                    managingOrganization = Reference(reference = FhirString(value = "Organization/org-1")),
                )

            db.chartCamQueries.insertResource("p-100", "Patient", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, patient, "Patient", "p-100")
            assertTrue(result.isSuccess)

            val stringIndices = db.chartCamQueries.searchResourcesByString("Patient", "family", "Smith").awaitAsList()
            assertEquals(1, stringIndices.size)
            assertEquals("p-100", stringIndices.first().resourceId)

            val mrnTokens = db.chartCamQueries.searchResourcesByToken("Patient", "mrn", null, "MRN-100").awaitAsList()
            assertEquals(1, mrnTokens.size)

            val birthDates = db.chartCamQueries.searchResourcesByDate("Patient", "birthdate", "1992-04-15").awaitAsList()
            assertEquals(1, birthDates.size)

            val orgRefs = db.chartCamQueries.searchResourcesByReference("Patient", "organization", "Organization/org-1").awaitAsList()
            assertEquals(1, orgRefs.size)

            val genderTokens =
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "gender", "http://hl7.org/fhir/administrative-gender", "female")
                    .awaitAsList()
            assertEquals(1, genderTokens.size)
        }

    /**
     * Verifies that Bundle metadata is indexed into date and token index tables.
     */
    @Test
    fun testIndexBundleMetadata() =
        runTest {
            val bundle =
                dev.ohs.fhir.model.r4.Bundle(
                    id = "b-1",
                    type = Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Collection),
                    meta =
                        dev.ohs.fhir.model.r4.Meta(
                            lastUpdated =
                                dev.ohs.fhir.model.r4.Instant(
                                    value =
                                        dev.ohs.fhir.model.r4.FhirDateTime
                                            .fromString("2026-09-17T12:00:00Z"),
                                ),
                            tag =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system = Uri(value = "http://chartcam.org/tags"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "offline-export"),
                                    ),
                                ),
                        ),
                )
            db.chartCamQueries.insertResource("b-1", "Bundle", "{}", "2026-09-17T12:00:00Z")
            val res = FhirSearchIndexer.indexResource(db.chartCamQueries, bundle, "Bundle", "b-1")
            assertTrue(res.isSuccess)

            val tagTokens =
                db.chartCamQueries
                    .searchResourcesByToken("Bundle", "_tag", "http://chartcam.org/tags", "offline-export")
                    .awaitAsList()
            assertEquals(1, tagTokens.size)

            val dateTokens =
                db.chartCamQueries
                    .searchResourcesByDate("Bundle", "_lastUpdated", "2026-09-17T12:00:00Z")
                    .awaitAsList()
            assertEquals(1, dateTokens.size)
        }

    /**
     * Verifies that Media resources are indexed via MediaSearchParams and base metadata.
     */
    @Test
    fun testIndexMedia() =
        runTest {
            val media =
                dev.ohs.fhir.model.r4.Media(
                    id = "m-1",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Media.EventStatus.Completed),
                    content =
                        dev.ohs.fhir.model.r4.Attachment(
                            url =
                                dev.ohs.fhir.model.r4
                                    .Url(value = "file:///photos/1.jpg"),
                        ),
                    subject = Reference(reference = FhirString(value = "Patient/p-100")),
                )
            db.chartCamQueries.insertResource("m-1", "Media", "{}", "2026-01-01")
            val res = FhirSearchIndexer.indexResource(db.chartCamQueries, media, "Media", "m-1")
            assertTrue(res.isSuccess)

            val refs = db.chartCamQueries.searchResourcesByReference("Media", "subject", "Patient/p-100").awaitAsList()
            assertEquals(1, refs.size)
        }

    /**
     * Verifies that an [Encounter] is indexed by patient, practitioner, and status.
     */
    @Test
    fun testIndexEncounter() =
        runTest {
            val encounter =
                Encounter(
                    id = "enc-100",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    subject = Reference(reference = FhirString(value = "Patient/p-100")),
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "Practitioner/dr-1")),
                            ),
                        ),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code("AMB"),
                            ),
                )

            db.chartCamQueries.insertResource("enc-100", "Encounter", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, encounter, "Encounter", "enc-100")
            assertTrue(result.isSuccess)

            val patientEncounters = db.chartCamQueries.searchResourcesByReference("Encounter", "patient", "Patient/p-100").awaitAsList()
            assertEquals(1, patientEncounters.size)

            val pracEncounters =
                db.chartCamQueries
                    .searchResourcesByReference(
                        "Encounter",
                        "practitioner",
                        "Practitioner/dr-1",
                    ).awaitAsList()
            assertEquals(1, pracEncounters.size)
        }

    /**
     * Verifies that a [DocumentReference] is indexed by subject, encounter, and status.
     */
    @Test
    fun testIndexDocumentReference() =
        runTest {
            val doc =
                DocumentReference(
                    id = "doc-100",
                    status = Enumeration(value = DocumentReferenceStatus.Current),
                    subject = Reference(reference = FhirString(value = "Patient/p-100")),
                    context =
                        DocumentReference.Context(
                            encounter = listOf(Reference(reference = FhirString(value = "Encounter/enc-100"))),
                        ),
                    content = emptyList(),
                    type =
                        dev.ohs.fhir.model.r4
                            .CodeableConcept(),
                )

            db.chartCamQueries.insertResource("doc-100", "DocumentReference", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, doc, "DocumentReference", "doc-100")
            assertTrue(result.isSuccess)

            val encDocs = db.chartCamQueries.searchResourcesByReference("DocumentReference", "encounter", "Encounter/enc-100").awaitAsList()
            assertEquals(1, encDocs.size)
        }

    /**
     * Verifies that a [QuestionnaireResponse] is indexed by subject, questionnaire, and encounter.
     */
    @Test
    fun testIndexQuestionnaireResponse() =
        runTest {
            val qr =
                QuestionnaireResponse(
                    id = "qr-100",
                    status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                    subject = Reference(reference = FhirString(value = "Patient/p-100")),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-100")),
                    questionnaire = Canonical(value = "Questionnaire/q-100"),
                )

            db.chartCamQueries.insertResource("qr-100", "QuestionnaireResponse", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, qr, "QuestionnaireResponse", "qr-100")
            assertTrue(result.isSuccess)

            val qrList =
                db.chartCamQueries
                    .searchResourcesByReference(
                        "QuestionnaireResponse",
                        "questionnaire",
                        "Questionnaire/q-100",
                    ).awaitAsList()
            assertEquals(1, qrList.size)
        }

    /**
     * Verifies that a [Questionnaire] is indexed by title and status.
     */
    @Test
    fun testIndexQuestionnaire() =
        runTest {
            val q =
                Questionnaire(
                    id = "q-100",
                    status = Enumeration(value = PublicationStatus.Active),
                    title = FhirString(value = "General Health Intake"),
                )

            db.chartCamQueries.insertResource("q-100", "Questionnaire", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, q, "Questionnaire", "q-100")
            assertTrue(result.isSuccess)

            val qList = db.chartCamQueries.searchResourcesByString("Questionnaire", "title", "General Health Intake").awaitAsList()
            assertEquals(1, qList.size)
        }

    /**
     * Verifies that a [Practitioner] is indexed by family, given, and active status.
     */
    @Test
    fun testIndexPractitioner() =
        runTest {
            val prac =
                Practitioner(
                    id = "prac-100",
                    name =
                        listOf(
                            HumanName(
                                family = FhirString(value = "Williams"),
                                given = listOf(FhirString(value = "Clara")),
                            ),
                        ),
                    active =
                        dev.ohs.fhir.model.r4
                            .Boolean(value = true),
                )

            db.chartCamQueries.insertResource("prac-100", "Practitioner", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, prac, "Practitioner", "prac-100")
            assertTrue(result.isSuccess)

            val pracList = db.chartCamQueries.searchResourcesByString("Practitioner", "family", "Williams").awaitAsList()
            assertEquals(1, pracList.size)
        }

    /**
     * Verifies that generic searchByParam operates across resources.
     */
    @Test
    fun testSearchByParamGeneric() =
        runTest {
            val repo = FhirRepository(db)
            val patient =
                io.healthplatform.chartcam.models.createFhirPatient(
                    id = "p-generic",
                    firstName = "Jordan",
                    lastName = "Taylor",
                    dob = kotlinx.datetime.LocalDate(1990, 1, 1),
                    mrnValue = "MRN-GEN",
                )
            repo.savePatient(patient)

            val results = repo.searchByParam<Patient>(PatientSearchParams.family, "Taylor")
            assertTrue(results.isSuccess)
            assertEquals(1, results.getOrNull()?.size)
            assertEquals("p-generic", results.getOrNull()?.first()?.id)
        }

    /**
     * Verifies that searching by quantity SearchParam works correctly via TokenIndex.
     */
    @Test
    fun testSearchByQuantityParam() =
        runTest {
            val repo = FhirRepository(db)
            val observation =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-weight-1",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                    code =
                        dev.ohs.fhir.model.r4.CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system = Uri(value = "http://loinc.org"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "29463-7"),
                                        display = FhirString(value = "Body weight"),
                                    ),
                                ),
                        ),
                    value =
                        dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                            dev.ohs.fhir.model.r4.Quantity(
                                value =
                                    dev.ohs.fhir.model.r4
                                        .Decimal(
                                            value =
                                                dev.ohs.fhir.model.r4.FhirDecimal
                                                    .fromString("75.5"),
                                        ),
                                unit = FhirString(value = "kg"),
                                system = Uri(value = "http://unitsofmeasure.org"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = "kg"),
                            ),
                        ),
                )
            val saveRes = repo.saveObservation(observation)
            assertTrue(saveRes.isSuccess)

            val results =
                repo.searchByParam<dev.ohs.fhir.model.r4.Observation>(
                    dev.ohs.fhir.model.r4.search.ObservationSearchParams.valueQuantity,
                    "75.5",
                )
            assertTrue(results.isSuccess)
            assertEquals(1, results.getOrNull()?.size)
            assertEquals("obs-weight-1", results.getOrNull()?.first()?.id)
        }

    /**
     * Verifies that universal base metadata parameters (_tag, _profile) are indexed and queryable.
     */
    @Test
    fun testBaseResourceParametersIndexed() =
        runTest {
            val repo = FhirRepository(db)
            val patient =
                Patient(
                    id = "p-meta-1",
                    meta =
                        dev.ohs.fhir.model.r4.Meta(
                            tag =
                                listOf(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system = Uri(value = "http://chartcam.org/tags"),
                                        code =
                                            dev.ohs.fhir.model.r4
                                                .Code(value = "vip"),
                                    ),
                                ),
                            profile =
                                listOf(
                                    dev.ohs.fhir.model.r4
                                        .Canonical(value = "http://hl7.org/fhir/StructureDefinition/patient-chartcam"),
                                ),
                        ),
                    name = listOf(HumanName(family = FhirString(value = "VipPerson"))),
                )
            val saveRes = repo.savePatient(patient)
            assertTrue(saveRes.isSuccess)

            val tagResults = repo.searchByTag<Patient>("vip")
            assertTrue(tagResults.isSuccess)
            assertEquals(1, tagResults.getOrNull()?.size)
            assertEquals("p-meta-1", tagResults.getOrNull()?.first()?.id)

            val profileResults = repo.searchByProfile<Patient>("http://hl7.org/fhir/StructureDefinition/patient-chartcam")
            assertTrue(profileResults.isSuccess)
            assertEquals(1, profileResults.getOrNull()?.size)
            assertEquals("p-meta-1", profileResults.getOrNull()?.first()?.id)
        }

    /**
     * Verifies date prefix modifiers (ge, le, gt, lt, ne) evaluate correctly on DateIndexEntity.
     */
    @Test
    fun testDatePrefixSearchModifiers() =
        runTest {
            val repo = FhirRepository(db)
            val p1 =
                Patient(
                    id = "p-1980",
                    birthDate = Date(value = FhirDate.fromString("1980-05-10")),
                )
            val p2 =
                Patient(
                    id = "p-1995",
                    birthDate = Date(value = FhirDate.fromString("1995-08-20")),
                )
            val p3 =
                Patient(
                    id = "p-2010",
                    birthDate = Date(value = FhirDate.fromString("2010-01-01")),
                )
            repo.savePatient(p1)
            repo.savePatient(p2)
            repo.savePatient(p3)

            val geResults = repo.searchByParam(PatientSearchParams.birthdate, "ge1995-01-01").getOrThrow()
            assertEquals(2, geResults.size)
            assertTrue(geResults.any { it.id == "p-1995" })
            assertTrue(geResults.any { it.id == "p-2010" })

            val leResults = repo.searchByParam(PatientSearchParams.birthdate, "le1995-08-20").getOrThrow()
            assertEquals(2, leResults.size)
            assertTrue(leResults.any { it.id == "p-1980" })
            assertTrue(leResults.any { it.id == "p-1995" })

            val gtResults = repo.searchByDatePrefixCatching<Patient>("birthdate", SearchPrefix.GT, "1995-08-20").getOrThrow()
            assertEquals(1, gtResults.size)
            assertEquals("p-2010", gtResults.first().id)

            val ltResults = repo.searchByDatePrefixCatching<Patient>("birthdate", SearchPrefix.LT, "1995-08-20").getOrThrow()
            assertEquals(1, ltResults.size)
            assertEquals("p-1980", ltResults.first().id)
        }

    /**
     * Verifies multi-parameter compound search with set intersection across criteria.
     */
    @Test
    fun testCompoundSearch() =
        runTest {
            val repo = FhirRepository(db)
            val pFemaleOld =
                Patient(
                    id = "p-f-old",
                    gender = Enumeration(value = AdministrativeGender.Female),
                    birthDate = Date(value = FhirDate.fromString("1970-01-01")),
                )
            val pFemaleYoung =
                Patient(
                    id = "p-f-young",
                    gender = Enumeration(value = AdministrativeGender.Female),
                    birthDate = Date(value = FhirDate.fromString("2005-01-01")),
                )
            val pMaleYoung =
                Patient(
                    id = "p-m-young",
                    gender = Enumeration(value = AdministrativeGender.Male),
                    birthDate = Date(value = FhirDate.fromString("2005-01-01")),
                )
            repo.savePatient(pFemaleOld)
            repo.savePatient(pFemaleYoung)
            repo.savePatient(pMaleYoung)

            val criteria =
                listOf(
                    SearchCriterion(PatientSearchParams.gender, "female"),
                    SearchCriterion(PatientSearchParams.birthdate, "ge2000-01-01"),
                )
            val results = repo.searchCompoundCatching(criteria).getOrThrow()
            assertEquals(1, results.size)
            assertEquals("p-f-young", results.first().id)
        }

    /**
     * Verifies local reverse-include resolution for Encounters with Observations.
     */
    @Test
    fun testLocalReverseIncludeEncountersWithObservations() =
        runTest {
            val repo = FhirRepository(db)
            val enc1 =
                Encounter(
                    id = "enc-rev-1",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    subject = Reference(reference = FhirString(value = "Patient/p-rev")),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code("AMB"),
                            ),
                )
            val enc2 =
                Encounter(
                    id = "enc-rev-2",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    subject = Reference(reference = FhirString(value = "Patient/p-rev")),
                    `class` =
                        dev.ohs.fhir.model.r4
                            .Coding(
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code("AMB"),
                            ),
                )
            val obs1 =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-rev-1",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                    subject = Reference(reference = FhirString(value = "Patient/p-rev")),
                    encounter = Reference(reference = FhirString(value = "Encounter/enc-rev-1")),
                    code =
                        dev.ohs.fhir.model.r4
                            .CodeableConcept(text = FhirString(value = "Heart Rate")),
                )
            repo.saveEncounter(enc1)
            repo.saveEncounter(enc2)
            repo.saveObservation(obs1)

            val map = repo.searchEncountersWithObservationsCatching("p-rev").getOrThrow()
            assertEquals(2, map.size)
            val targetEncEntry = map.entries.first { it.key.id == "enc-rev-1" }
            assertEquals(1, targetEncEntry.value.size)
            assertEquals("obs-rev-1", targetEncEntry.value.first().id)

            val emptyEncEntry = map.entries.first { it.key.id == "enc-rev-2" }
            assertEquals(0, emptyEncEntry.value.size)
        }

    /**
     * Verifies that Device resources are indexed using DeviceSearchParams.
     */
    @Test
    fun testIndexDevice() =
        runTest {
            val device =
                dev.ohs.fhir.model.r4.Device(
                    id = "dev-100",
                    status = Enumeration(value = dev.ohs.fhir.model.r4.Device.FHIRDeviceStatus.Active),
                    manufacturer = FhirString(value = "HealthCorp"),
                    modelNumber = FhirString(value = "HC-9000"),
                )
            db.chartCamQueries.insertResource("dev-100", "Device", "{}", "2026-01-01")
            val res = FhirSearchIndexer.indexResource(db.chartCamQueries, device, "Device", "dev-100")
            assertTrue(res.isSuccess)

            val stringIndices = db.chartCamQueries.searchResourcesByString("Device", "manufacturer", "HealthCorp").awaitAsList()
            assertEquals(1, stringIndices.size)
            assertEquals("dev-100", stringIndices.first().resourceId)
        }

    /**
     * Verifies that Provenance resources are indexed and target references (prefixed and unprefixed) are handled.
     */
    @Test
    fun testIndexProvenance() =
        runTest {
            val provenance =
                dev.ohs.fhir.model.r4.Provenance(
                    id = "prov-100",
                    recorded =
                        dev.ohs.fhir.model.r4.Instant(
                            value =
                                dev.ohs.fhir.model.r4.FhirDateTime
                                    .fromString("2026-09-18T10:00:00Z"),
                        ),
                    agent =
                        listOf(
                            dev.ohs.fhir.model.r4.Provenance.Agent(
                                who = Reference(reference = FhirString(value = "Practitioner/dr-1")),
                            ),
                        ),
                    target =
                        listOf(
                            Reference(reference = FhirString(value = "Encounter/enc-100")),
                            Reference(reference = FhirString(value = "enc-raw-200")),
                            Reference(reference = null),
                        ),
                )
            db.chartCamQueries.insertResource("prov-100", "Provenance", "{}", "2026-09-18T10:00:00Z")
            val res = FhirSearchIndexer.indexResource(db.chartCamQueries, provenance, "Provenance", "prov-100")
            assertTrue(res.isSuccess)

            val encRefs = db.chartCamQueries.searchResourcesByReference("Provenance", "encounter", "Encounter/enc-raw-200").awaitAsList()
            assertEquals(2, encRefs.size)
        }

    /**
     * Verifies that Encounter indexing correctly normalizes non-prefixed practitioner references and handles nulls.
     */
    @Test
    fun testIndexEncounterUnprefixedAndNullParticipant() =
        runTest {
            val encounter =
                Encounter(
                    id = "enc-unprefixed",
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    participant =
                        listOf(
                            Encounter.Participant(
                                individual = Reference(reference = FhirString(value = "dr-unprefixed")),
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
            db.chartCamQueries.insertResource("enc-unprefixed", "Encounter", "{}", "2026-01-01")
            val result = FhirSearchIndexer.indexResource(db.chartCamQueries, encounter, "Encounter", "enc-unprefixed")
            assertTrue(result.isSuccess)

            val pracRefs =
                db.chartCamQueries
                    .searchResourcesByReference("Encounter", "practitioner", "Practitioner/dr-unprefixed")
                    .awaitAsList()
            assertEquals(2, pracRefs.size)
        }

    /**
     * Verifies that unhandled custom FHIR resource types fall back to generic indexing branch.
     */
    @Test
    fun testIndexUnhandledResourceFallback() =
        runTest {
            val organization =
                dev.ohs.fhir.model.r4.Organization(
                    id = "org-1",
                    name = FhirString(value = "General Hospital"),
                )
            db.chartCamQueries.insertResource("org-1", "Organization", "{}", "2026-01-01")
            val res = FhirSearchIndexer.indexResource(db.chartCamQueries, organization, "Organization", "org-1")
            assertTrue(res.isSuccess)
        }
}
