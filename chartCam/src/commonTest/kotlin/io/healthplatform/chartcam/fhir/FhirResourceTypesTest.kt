/**
 * @file FhirResourceTypesTest.kt
 * Unit tests for resolveCanonicalResourceType and resolveResourceTypeName.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.AuditEvent
import dev.ohs.fhir.model.r4.Basic
import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Location
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests verifying canonical FHIR resourceType string resolution without reflection.
 */
class FhirResourceTypesTest {
    /**
     * Custom mock resource used for testing dynamic and fallback resolution.
     */
    private class UnknownCustomResource : Resource() {
        override val id: String? = null
        override val meta: dev.ohs.fhir.model.r4.Meta? = null
        override val implicitRules: dev.ohs.fhir.model.r4.Uri? = null
        override val language: Code? = null

        override fun toBuilder(): Resource.Builder = error("Stub")
    }

    /**
     * Verifies that all standard ChartCam FHIR clinical entities resolve to their exact canonical type strings.
     */
    @Test
    fun testStandardClinicalEntitiesResolveCorrectly() {
        assertEquals("Patient", resolveCanonicalResourceType<Patient>().getOrThrow())
        assertEquals("Encounter", resolveCanonicalResourceType<Encounter>().getOrThrow())
        assertEquals("Observation", resolveCanonicalResourceType<Observation>().getOrThrow())
        assertEquals("DocumentReference", resolveCanonicalResourceType<DocumentReference>().getOrThrow())
        assertEquals("Questionnaire", resolveCanonicalResourceType<Questionnaire>().getOrThrow())
        assertEquals("QuestionnaireResponse", resolveCanonicalResourceType<QuestionnaireResponse>().getOrThrow())
        assertEquals("Practitioner", resolveCanonicalResourceType<Practitioner>().getOrThrow())
        assertEquals("Device", resolveCanonicalResourceType<Device>().getOrThrow())
        assertEquals("Provenance", resolveCanonicalResourceType<Provenance>().getOrThrow())
        assertEquals("Bundle", resolveCanonicalResourceType<Bundle>().getOrThrow())
        assertEquals("AuditEvent", resolveCanonicalResourceType<AuditEvent>().getOrThrow())
        assertEquals("Media", resolveCanonicalResourceType<Media>().getOrThrow())
    }

    /**
     * Verifies that resolveResourceTypeName handles all branches cleanly.
     */
    @Test
    fun testResolveResourceTypeNameAllBranches() {
        val patient = Patient(id = "p1")
        assertEquals("Patient", resolveResourceTypeName(patient))

        val encounter =
            Encounter(
                id = "e1",
                status = Enumeration(value = Encounter.EncounterStatus.Planned),
                `class` = Coding(),
            )
        assertEquals("Encounter", resolveResourceTypeName(encounter))

        val observation =
            Observation(
                id = "o1",
                status = Enumeration(value = Observation.ObservationStatus.Final),
                code = CodeableConcept(),
            )
        assertEquals("Observation", resolveResourceTypeName(observation))

        val docRef =
            DocumentReference(
                id = "d1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content = emptyList(),
            )
        assertEquals("DocumentReference", resolveResourceTypeName(docRef))

        val questionnaire =
            Questionnaire(
                id = "q1",
                status = Enumeration(value = PublicationStatus.Active),
            )
        assertEquals("Questionnaire", resolveResourceTypeName(questionnaire))

        val questionnaireResponse =
            QuestionnaireResponse(
                id = "qr1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )
        assertEquals("QuestionnaireResponse", resolveResourceTypeName(questionnaireResponse))

        val practitioner = Practitioner(id = "pr1")
        assertEquals("Practitioner", resolveResourceTypeName(practitioner))

        val device = Device(id = "dev1")
        assertEquals("Device", resolveResourceTypeName(device))

        val provenance =
            Provenance(
                id = "prov1",
                target = emptyList(),
                recorded = Instant(value = FhirDateTime.fromString("2026-01-01T00:00:00Z")),
                agent = emptyList(),
            )
        assertEquals("Provenance", resolveResourceTypeName(provenance))

        val media =
            Media(
                id = "m1",
                status = Enumeration(value = Media.EventStatus.Completed),
                content = Attachment(),
            )
        assertEquals("Media", resolveResourceTypeName(media))

        val auditEvent =
            AuditEvent(
                id = "a1",
                type = Coding(),
                recorded = Instant(value = FhirDateTime.fromString("2026-01-01T00:00:00Z")),
                agent =
                    listOf(
                        AuditEvent.Agent(
                            requestor =
                                dev.ohs.fhir.model.r4
                                    .Boolean(value = true),
                        ),
                    ),
                source = AuditEvent.Source(observer = Reference()),
            )
        assertEquals("AuditEvent", resolveResourceTypeName(auditEvent))

        val bundle =
            Bundle(
                id = "b1",
                type = Enumeration(value = Bundle.BundleType.Collection),
            )
        assertEquals("Bundle", resolveResourceTypeName(bundle))

        // Dynamic fallback for known FHIR resource not in explicit when
        val basic = Basic(code = CodeableConcept())
        assertEquals("Basic", resolveResourceTypeName(basic))

        val binary = Binary(contentType = Code(value = "application/octet-stream"))
        assertEquals("Binary", resolveResourceTypeName(binary))

        val location = Location(id = "loc-1")
        assertEquals("Location", resolveResourceTypeName(location))

        // Dynamic fallback for unknown custom resource class
        val custom = UnknownCustomResource()
        assertEquals("Resource", resolveResourceTypeName(custom))
        assertEquals("Resource", resolveDynamicResourceTypeName(custom))

        // Dynamic fallback for anonymous object where simpleName is null
        val anonymousResource =
            object : Resource() {
                override val id: String? = null
                override val meta: dev.ohs.fhir.model.r4.Meta? = null
                override val implicitRules: dev.ohs.fhir.model.r4.Uri? = null
                override val language: Code? = null

                override fun toBuilder(): Resource.Builder = error("Stub")
            }
        assertEquals("Resource", resolveResourceTypeName(anonymousResource))
        assertEquals("Resource", resolveDynamicResourceTypeName(anonymousResource))
    }

    /**
     * Verifies that resolution returns success on valid resources.
     */
    @Test
    fun testResolutionSuccess() {
        val res = resolveCanonicalResourceType<Patient>()
        assertTrue(res.isSuccess)
    }
}
