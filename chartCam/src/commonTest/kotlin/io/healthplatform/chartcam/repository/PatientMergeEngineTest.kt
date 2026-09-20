/**
 * @file PatientMergeEngineTest.kt
 * Unit tests for PatientMergeEngine verifying conflict resolution and demographic merging.
 */

package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Address
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.ContactPoint
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite verifying [PatientMergeEngine] demographic merging and resource reparenting.
 */
class PatientMergeEngineTest {
    /**
     * Verifies mergeDemographics across both present and absent demographic fields.
     */
    @Test
    fun testMergeDemographics() {
        val engine = PatientMergeEngine()

        val telecom1 = listOf(ContactPoint(value = FhirString(value = "555-1111")))
        val telecom2 = listOf(ContactPoint(value = FhirString(value = "555-2222")))
        val address1 = listOf(Address(city = FhirString(value = "Metropolis")))
        val address2 = listOf(Address(city = FhirString(value = "Gotham")))

        // Case 1: Local has null/empty fields, incoming fills them
        val localSparse =
            Patient(
                id = "p-local",
                gender = null,
                birthDate = null,
                telecom = emptyList(),
                address = emptyList(),
            )
        val incomingFull =
            Patient(
                id = "p-incoming",
                gender = Enumeration(value = AdministrativeGender.Female),
                birthDate =
                    Date(
                        value =
                            dev.ohs.fhir.model.r4.FhirDate
                                .fromString("1995-05-15"),
                    ),
                telecom = telecom2,
                address = address2,
            )

        val merged1 = engine.mergeDemographics(localSparse, incomingFull)
        assertEquals("p-local", merged1.id)
        assertEquals<AdministrativeGender?>(AdministrativeGender.Female, merged1.gender?.value)
        assertEquals("1995-05-15", merged1.birthDate?.value?.toString())
        assertEquals(telecom2, merged1.telecom)
        assertEquals(address2, merged1.address)

        // Case 2: Local already has non-empty fields, local values take precedence
        val localFull =
            Patient(
                id = "p-local-2",
                gender = Enumeration(value = AdministrativeGender.Male),
                birthDate =
                    Date(
                        value =
                            dev.ohs.fhir.model.r4.FhirDate
                                .fromString("1990-01-01"),
                    ),
                telecom = telecom1,
                address = address1,
            )

        val merged2 = engine.mergeDemographics(localFull, incomingFull)
        assertEquals("p-local-2", merged2.id)
        assertEquals<AdministrativeGender?>(AdministrativeGender.Male, merged2.gender?.value)
        assertEquals("1990-01-01", merged2.birthDate?.value?.toString())
        assertEquals(telecom1, merged2.telecom)
        assertEquals(address1, merged2.address)
    }

    /**
     * Verifies reparentEncounter with and without "Patient/" prefix.
     */
    @Test
    fun testReparentEncounter() {
        val engine = PatientMergeEngine()
        val encounter =
            Encounter(
                id = "enc-1",
                status = Enumeration(value = Encounter.EncounterStatus.Planned),
                `class` = Coding(),
            )

        val reparented1 = engine.reparentEncounter(encounter, "target-123")
        assertEquals("Patient/target-123", reparented1.subject?.reference?.value)

        val reparented2 = engine.reparentEncounter(encounter, "Patient/target-456")
        assertEquals("Patient/target-456", reparented2.subject?.reference?.value)
    }

    /**
     * Verifies reparentDocumentReference with and without "Patient/" prefix.
     */
    @Test
    fun testReparentDocumentReference() {
        val engine = PatientMergeEngine()
        val doc =
            DocumentReference(
                id = "doc-1",
                status = Enumeration(value = DocumentReferenceStatus.Current),
                content = emptyList(),
            )

        val reparented1 = engine.reparentDocumentReference(doc, "target-123")
        assertEquals("Patient/target-123", reparented1.subject?.reference?.value)

        val reparented2 = engine.reparentDocumentReference(doc, "Patient/target-456")
        assertEquals("Patient/target-456", reparented2.subject?.reference?.value)
    }

    /**
     * Verifies reparentQuestionnaireResponse with and without "Patient/" prefix.
     */
    @Test
    fun testReparentQuestionnaireResponse() {
        val engine = PatientMergeEngine()
        val qr =
            QuestionnaireResponse(
                id = "qr-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )

        val reparented1 = engine.reparentQuestionnaireResponse(qr, "target-123")
        assertEquals("Patient/target-123", reparented1.subject?.reference?.value)

        val reparented2 = engine.reparentQuestionnaireResponse(qr, "Patient/target-456")
        assertEquals("Patient/target-456", reparented2.subject?.reference?.value)
    }
}
