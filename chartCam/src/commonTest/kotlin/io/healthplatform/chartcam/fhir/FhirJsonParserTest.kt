/**
 * @file FhirJsonParserTest.kt
 * Unit tests for FhirJsonParser verifying round-trip serialization and error handling.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite for verifying multiplatform FHIR JSON serialization via [FhirJsonParser].
 */
class FhirJsonParserTest {
    /**
     * Verifies that a [Patient] resource round-trips through JSON encoding and decoding.
     */
    @Test
    fun testPatientRoundTrip() {
        val patient =
            Patient(
                id = "p-123",
                name =
                    listOf(
                        HumanName(
                            family = FhirString(value = "Doe"),
                            given = listOf(FhirString(value = "John")),
                        ),
                    ),
            )
        val encodeResult = FhirJsonParser.encodeResource(patient)
        assertTrue(encodeResult.isSuccess)
        val json = encodeResult.getOrNull()
        assertNotNull(json)
        assertTrue(json.contains("p-123"))

        val decodeResult = FhirJsonParser.decodeTypedResource(Patient.serializer(), json)
        assertTrue(decodeResult.isSuccess)
        val decoded = decodeResult.getOrNull()
        assertNotNull(decoded)
        assertEquals("p-123", decoded.id)
        assertEquals(
            "Doe",
            decoded.name
                .firstOrNull()
                ?.family
                ?.value,
        )
    }

    /**
     * Verifies that an [Encounter] resource round-trips successfully.
     */
    @Test
    fun testEncounterRoundTrip() {
        val encounter =
            Encounter(
                id = "enc-456",
                status = Enumeration(value = Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4.Coding(
                        code =
                            dev.ohs.fhir.model.r4
                                .Code("AMB"),
                        system =
                            dev.ohs.fhir.model.r4
                                .Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                    ),
            )
        val json = FhirJsonParser.encodeResource(encounter).getOrNull()
        assertNotNull(json)

        val decoded = FhirJsonParser.decodeTypedResource(Encounter.serializer(), json).getOrNull()
        assertNotNull(decoded)
        assertEquals("enc-456", decoded.id)
        assertEquals(Encounter.EncounterStatus.Finished, decoded.status.value)
    }

    /**
     * Verifies that a [Questionnaire] resource round-trips successfully.
     */
    @Test
    fun testQuestionnaireRoundTrip() {
        val questionnaire =
            Questionnaire(
                id = "q-789",
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = "Triage Intake"),
            )
        val json = FhirJsonParser.encodeResource(questionnaire).getOrNull()
        assertNotNull(json)

        val decoded = FhirJsonParser.decodeTypedResource(Questionnaire.serializer(), json).getOrNull()
        assertNotNull(decoded)
        assertEquals("q-789", decoded.id)
        assertEquals("Triage Intake", decoded.title?.value)
    }

    /**
     * Verifies polymorphic decoding of different resources via [FhirJsonParser.decodeAnyResource].
     */
    @Test
    fun testPolymorphicDecoding() {
        val patientJson = """{"resourceType":"Patient","id":"poly-p1"}"""
        val patientResult = FhirJsonParser.decodeAnyResource(patientJson)
        assertTrue(patientResult.isSuccess)
        val patient = patientResult.getOrNull() as? Patient
        assertNotNull(patient)
        assertEquals("poly-p1", patient.id)

        val docJson = """{"resourceType":"DocumentReference","id":"doc-01","status":"current"}"""
        val docResult = FhirJsonParser.decodeAnyResource(docJson)
        assertTrue(docResult.isSuccess)
        val doc = docResult.getOrNull() as? DocumentReference
        assertNotNull(doc)
        assertEquals("doc-01", doc.id)
    }

    /**
     * Verifies that a [Bundle] containing multiple polymorphic entries round-trips properly.
     */
    @Test
    fun testBundleRoundTrip() {
        val patient = Patient(id = "b-p1")
        val encounter =
            Encounter(
                id = "b-e1",
                status = Enumeration(value = Encounter.EncounterStatus.Planned),
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
                id = "bundle-01",
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(resource = patient),
                        Bundle.Entry(resource = encounter),
                    ),
            )

        val jsonResult = FhirJsonParser.encodeTypedResource(Bundle.serializer(), bundle)
        assertTrue(jsonResult.isSuccess)
        val json = jsonResult.getOrNull()
        assertNotNull(json)

        val decodedResult = FhirJsonParser.decodeTypedResource(Bundle.serializer(), json)
        assertTrue(decodedResult.isSuccess)
        val decodedBundle = decodedResult.getOrNull()
        assertNotNull(decodedBundle)
        assertEquals(2, decodedBundle.entry.size)
        assertTrue(decodedBundle.entry[0].resource is Patient)
        assertTrue(decodedBundle.entry[1].resource is Encounter)
    }

    /**
     * Verifies that malformed JSON strings return a failure result instead of throwing.
     */
    @Test
    fun testMalformedJsonReturnsFailure() {
        val malformedJson = "{ not valid fhir json ... "
        val result = FhirJsonParser.decodeAnyResource(malformedJson)
        assertTrue(result.isFailure)

        val typedResult = FhirJsonParser.decodeTypedResource(Patient.serializer(), malformedJson)
        assertTrue(typedResult.isFailure)
    }

    /**
     * Verifies that compact JSON omits whitespace and indentation while pretty JSON includes them.
     */
    @Test
    fun testCompactVsPrettyJson() {
        val patient =
            Patient(
                id = "p-compact-test",
                name =
                    listOf(
                        HumanName(
                            family = FhirString(value = "Smith"),
                            given = listOf(FhirString(value = "Jane")),
                        ),
                    ),
            )

        val compactResult = FhirJsonParser.encodeResource(patient)
        val prettyResult = FhirJsonParser.encodeResourcePretty(patient)

        assertTrue(compactResult.isSuccess)
        assertTrue(prettyResult.isSuccess)

        val compactJson = compactResult.getOrThrow()
        val prettyJson = prettyResult.getOrThrow()

        assertTrue(prettyJson.contains("\n"))
        assertTrue(!compactJson.contains("\n"))
        assertTrue(compactJson.length < prettyJson.length)

        val decodedFromCompact = FhirJsonParser.decodeTypedResource(Patient.serializer(), compactJson).getOrThrow()
        val decodedFromPretty = FhirJsonParser.decodeTypedResource(Patient.serializer(), prettyJson).getOrThrow()

        assertEquals(patient.id, decodedFromCompact.id)
        assertEquals(patient.id, decodedFromPretty.id)
        assertEquals(
            "Smith",
            decodedFromCompact.name
                .firstOrNull()
                ?.family
                ?.value,
        )
        assertEquals(
            "Smith",
            decodedFromPretty.name
                .firstOrNull()
                ?.family
                ?.value,
        )
    }

    /**
     * Verifies typed pretty and inline pretty serialization.
     */
    @Test
    fun testTypedPrettySerialization() {
        val patient = Patient(id = "p-pretty-typed")
        val typedPretty = FhirJsonParser.encodeTypedResourcePretty(Patient.serializer(), patient).getOrThrow()
        assertTrue(typedPretty.contains("\n"))
        assertTrue(typedPretty.contains("p-pretty-typed"))

        val inlinePretty = FhirJsonParser.encodeToStringPrettyCatching(patient).getOrThrow()
        assertTrue(inlinePretty.contains("\n"))
        assertEquals(typedPretty, inlinePretty)
    }

    /**
     * Verifies multi-version decoding for R4, R4B, and R5 resources.
     */
    @Test
    fun testMultiVersionDecoding() {
        val r4Json = """{"resourceType":"Patient","id":"r4-pat"}"""
        val r4Res = FhirJsonParser.decodeR4Resource(r4Json)
        assertTrue(r4Res.isSuccess)
        assertEquals("r4-pat", r4Res.getOrNull()?.id)

        val r4bJson = """{"resourceType":"Patient","id":"r4b-pat"}"""
        val r4bRes = FhirJsonParser.decodeR4BResource(r4bJson)
        assertTrue(r4bRes.isSuccess)
        assertEquals("r4b-pat", r4bRes.getOrNull()?.id)

        val r5Json = """{"resourceType":"Patient","id":"r5-pat"}"""
        val r5Res = FhirJsonParser.decodeR5Resource(r5Json)
        assertTrue(r5Res.isSuccess)
        assertEquals("r5-pat", r5Res.getOrNull()?.id)
    }

    /**
     * Verifies canonical resource type resolution is reflection-free and stable.
     */
    @Test
    fun testCanonicalResourceTypeResolution() {
        assertEquals("Patient", resolveCanonicalResourceType<Patient>().getOrNull())
        assertEquals("Encounter", resolveCanonicalResourceType<Encounter>().getOrNull())
        assertEquals("Observation", resolveCanonicalResourceType<dev.ohs.fhir.model.r4.Observation>().getOrNull())
        assertEquals("DocumentReference", resolveCanonicalResourceType<DocumentReference>().getOrNull())
        assertEquals("Questionnaire", resolveCanonicalResourceType<Questionnaire>().getOrNull())
        assertEquals("Bundle", resolveCanonicalResourceType<Bundle>().getOrNull())

        val pat = Patient(id = "pat-inst")
        assertEquals("Patient", resolveResourceTypeName(pat))
        val enc =
            Encounter(
                id = "enc-inst",
                status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code(value = "AMB"),
                        ),
            )
        assertEquals("Encounter", resolveResourceTypeName(enc))
    }

    /**
     * Verifies that polymorphic encoding produces a single resourceType discriminator.
     */
    @Test
    fun testPolymorphicEncodingPreservesSingleResourceType() {
        val patient: dev.ohs.fhir.model.r4.Resource = Patient(id = "poly-1")
        val json = FhirJsonParser.encodeResource(patient).getOrThrow()
        val count = Regex("\"resourceType\"\\s*:\\s*\"Patient\"").findAll(json).count()
        assertEquals(1, count)

        val decoded = FhirJsonParser.decodeAnyResource(json).getOrThrow()
        assertTrue(decoded is Patient)
        assertEquals("poly-1", decoded.id)

        assertNotNull(FhirJsonParser.json)
    }
}
