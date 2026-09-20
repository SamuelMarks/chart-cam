/**
 * @file FhirContainedResourcesTest.kt
 * Unit tests for FhirContainedResources verifying contained resource inspection and manipulation.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.AuditEvent
import dev.ohs.fhir.model.r4.Basic
import dev.ohs.fhir.model.r4.CarePlan
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DiagnosticReport
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Location
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Organization
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite for [FhirContainedResources].
 */
class FhirContainedResourcesTest {
    /**
     * Verifies getContainedResource, resolveAllContained, createLocalReference, and addContainedResource.
     */
    @Test
    fun testContainedResourceSuite() {
        val practitioner =
            Practitioner(
                id = "prac-c1",
                name = listOf(HumanName(family = FhirString(value = "Doctor"))),
            )
        val patient =
            Patient(
                id = "pat-parent",
                contained = listOf(practitioner),
            )

        // 1. Resolve by reference with #
        val foundWithHash = patient.getContainedResource<Practitioner>("#prac-c1").getOrThrow()
        assertNotNull(foundWithHash)
        assertEquals("prac-c1", foundWithHash.id)

        // 2. Resolve by reference without #
        val foundNoHash = patient.getContainedResource<Practitioner>("prac-c1").getOrThrow()
        assertNotNull(foundNoHash)
        assertEquals("prac-c1", foundNoHash.id)

        // 3. Resolve non-existent
        val notFound = patient.getContainedResource<Practitioner>("nonexistent").getOrThrow()
        assertNull(notFound)

        // 4. resolveAllContained
        val allPracs = patient.resolveAllContained<Practitioner>()
        assertEquals(1, allPracs.size)
        assertEquals("prac-c1", allPracs.first().id)

        val allObs = patient.resolveAllContained<Observation>()
        assertEquals(0, allObs.size)

        // 5. createLocalReference
        val localRef = createLocalReference(practitioner)
        assertEquals("#prac-c1", localRef.reference?.value)

        val localRefNoId = createLocalReference(Practitioner(id = null))
        assertEquals("#", localRefNoId.reference?.value)

        val localRefPrefixedId = createLocalReference(Practitioner(id = "#already-has-hash"))
        assertEquals("#already-has-hash", localRefPrefixedId.reference?.value)

        // 6. addContainedResource
        val newObs =
            Observation(
                id = "obs-c2",
                status = Enumeration(value = Observation.ObservationStatus.Final),
                code =
                    CodeableConcept(text = FhirString(value = "Vitals")),
            )
        val updatedPatient = addContainedResource(patient, newObs).getOrThrow()
        assertEquals(2, updatedPatient.contained.size)
        val resolvedObs = updatedPatient.getContainedResource<Observation>("#obs-c2").getOrThrow()
        assertNotNull(resolvedObs)
        assertEquals("obs-c2", resolvedObs.id)

        // 7. addContainedResource on Media
        val media =
            Media(
                id = "med-1",
                status = Enumeration(value = Media.EventStatus.Completed),
                content = Attachment(),
            )
        val updatedMedia = addContainedResource(media, newObs).getOrThrow()
        assertEquals(1, updatedMedia.contained.size)
        assertEquals("obs-c2", updatedMedia.contained.first().id)
    }

    /**
     * Verifies addContainedResource on all supported clinical and administrative DomainResource subtypes.
     */
    @Test
    fun testAddContainedResourceAllSubtypes() {
        val child = Practitioner(id = "ch-1")

        // Encounter
        val encounter =
            Encounter(
                id = "enc-1",
                status = Enumeration(value = Encounter.EncounterStatus.Planned),
                `class` = Coding(),
            )
        assertEquals(1, addContainedResource(encounter, child).getOrThrow().contained.size)

        // DocumentReference
        val docRef =
            DocumentReference(
                id = "doc-1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content = emptyList(),
            )
        assertEquals(1, addContainedResource(docRef, child).getOrThrow().contained.size)

        // QuestionnaireResponse
        val qr =
            QuestionnaireResponse(
                id = "qr-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )
        assertEquals(1, addContainedResource(qr, child).getOrThrow().contained.size)

        // Questionnaire
        val q =
            Questionnaire(
                id = "q-1",
                status = Enumeration(value = PublicationStatus.Active),
            )
        assertEquals(1, addContainedResource(q, child).getOrThrow().contained.size)

        // Observation
        val obs =
            Observation(
                id = "obs-1",
                status = Enumeration(value = Observation.ObservationStatus.Final),
                code = CodeableConcept(),
            )
        assertEquals(1, addContainedResource(obs, child).getOrThrow().contained.size)

        // Condition
        val cond =
            Condition(
                id = "cond-1",
                subject = Reference(),
            )
        assertEquals(1, addContainedResource(cond, child).getOrThrow().contained.size)

        // CarePlan
        val cp =
            CarePlan(
                id = "cp-1",
                status = Enumeration(value = CarePlan.RequestStatus.Active),
                intent = Enumeration(value = CarePlan.CarePlanIntent.Plan),
                subject = Reference(),
            )
        assertEquals(1, addContainedResource(cp, child).getOrThrow().contained.size)

        // DiagnosticReport
        val dr =
            DiagnosticReport(
                id = "dr-1",
                status = Enumeration(value = DiagnosticReport.DiagnosticReportStatus.Final),
                code = CodeableConcept(),
            )
        assertEquals(1, addContainedResource(dr, child).getOrThrow().contained.size)

        // Provenance
        val prov =
            Provenance(
                id = "prov-1",
                target = emptyList(),
                recorded = Instant(value = FhirDateTime.fromString("2026-01-01T00:00:00Z")),
                agent = emptyList(),
            )
        assertEquals(1, addContainedResource(prov, child).getOrThrow().contained.size)

        // AuditEvent
        val aud =
            AuditEvent(
                id = "aud-1",
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
        assertEquals(1, addContainedResource(aud, child).getOrThrow().contained.size)

        // Practitioner
        val prac = Practitioner(id = "prac-1")
        assertEquals(1, addContainedResource(prac, child).getOrThrow().contained.size)

        // Device
        val dev = Device(id = "dev-1")
        assertEquals(1, addContainedResource(dev, child).getOrThrow().contained.size)

        // Organization
        val org = Organization(id = "org-1")
        assertEquals(1, addContainedResource(org, child).getOrThrow().contained.size)

        // Basic
        val basic = Basic(id = "basic-1", code = CodeableConcept())
        assertEquals(1, addContainedResource(basic, child).getOrThrow().contained.size)

        // Unsupported DomainResource type (Location) triggers failure Result
        val unsupported = Location(id = "loc-1")
        val failureResult = addContainedResource(unsupported, child)
        assertTrue(failureResult.isFailure)
    }
}
