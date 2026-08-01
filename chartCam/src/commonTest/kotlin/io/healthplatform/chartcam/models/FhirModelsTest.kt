@file:Suppress("UNNECESSARY_SAFE_CALL")

package io.healthplatform.chartcam.models

import com.google.fhir.model.r4.Patient
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FhirModelsTest {
    @Test
    fun testCreateFhirPatient() {
        val dob = LocalDate(1990, 1, 1)
        val patient =
            createFhirPatient(
                id = "pat_123",
                firstName = "John",
                lastName = "Doe",
                dob = dob,
                mrnValue = "MRN-555",
            )
        assertEquals("pat_123", patient.id)
        assertEquals("Doe", patient.name.first().familyName)
        assertEquals("John", patient.name.first().givenName)
        assertEquals("MRN-555", patient.mrn)
        assertEquals("1990-01-01", patient.customBirthDate)
        assertEquals("Doe, John", patient.fullName)
    }

    @Test
    fun testCreateFhirPractitioner() {
        val practitioner =
            createFhirPractitioner(
                id = "prac_456",
                lastName = "Smith",
                firstName = "Jane",
                isActive = true,
            )
        assertEquals("prac_456", practitioner.id)
        assertEquals("Smith", practitioner.name.first().familyName)
        assertEquals("Jane", practitioner.name.first().givenName)
        assertTrue(practitioner.active?.value == true)
        assertEquals("Smith, Jane", practitioner.fullName)
    }

    @Test
    fun testCreateFhirEncounter() {
        val encounter =
            createFhirEncounter(
                id = "enc_789",
                patientId = "Patient/pat_123",
                practitionerId = "Practitioner/prac_456",
                dateStr = "2023-10-27T10:00:00Z",
            )
        assertEquals("enc_789", encounter.id)
        assertEquals("Patient/pat_123", encounter.subject?.reference?.value)
        assertEquals(
            "Practitioner/prac_456",
            encounter.participant
                .first()
                .individual
                ?.reference
                ?.value,
        )
        assertTrue(encounter.encounterDate.contains("2023-10-27"))
    }

    @Test
    fun testCreateFhirDocumentReference() {
        val docRef =
            createFhirDocumentReference(
                DocumentReferenceCreationParams(
                    id = "doc_111",
                    patientId = "Patient/pat_123",
                    encounterId = "Encounter/enc_789",
                    dateStr = "2023-10-27T10:05:00Z",
                    desc = "Front View",
                    mime = "image/jpeg",
                    urlPath = "file:///path/to/image.jpg",
                ),
            )
        assertEquals("doc_111", docRef.id)
        assertEquals("Patient/pat_123", docRef.subject?.reference?.value)
        assertEquals(
            "Encounter/enc_789",
            docRef.context
                ?.encounter
                ?.first()
                ?.reference
                ?.value,
        )
        assertEquals("Front View", docRef.description?.value)
        assertEquals(
            "image/jpeg",
            docRef.content
                .first()
                .attachment
                ?.contentType
                ?.value,
        )
        assertEquals(
            "file:///path/to/image.jpg",
            docRef.content
                .first()
                .attachment
                ?.url
                ?.value,
        )
    }

    @Test
    fun testCreateFhirClinicalNote() {
        val clinicalNote =
            createFhirClinicalNote(
                id = "note_222",
                patientId = "Patient/pat_123",
                encounterId = "Encounter/enc_789",
                dateStr = "2023-10-27T10:10:00Z",
                notesText = "Patient seems fine.",
            )
        assertEquals("note_222", clinicalNote.id)
        assertEquals("Patient/pat_123", clinicalNote.subject?.reference?.value)
        assertEquals(
            "text/plain",
            clinicalNote.content
                .first()
                .attachment
                ?.contentType
                ?.value,
        )
        assertTrue(
            clinicalNote.content
                .first()
                .attachment
                ?.url
                ?.value
                ?.contains("Patient seems fine.") == true,
        )
    }

    @Test
    fun testCreateFhirDevice() {
        val device =
            createFhirDevice(
                id = "dev_333",
                modelName = "Pixel 7",
                manufacturerName = "Google",
            )
        assertEquals("dev_333", device.id)
        assertEquals(
            "Pixel 7",
            device.deviceName
                .first()
                .name
                ?.value,
        )
        assertEquals("Google", device.manufacturer?.value)
    }

    @Test
    fun testCreateFhirProvenance() {
        val provenance =
            createFhirProvenance(
                id = "prov_444",
                targetResourceId = "DocumentReference/doc_111",
                practitionerId = "Practitioner/prac_456",
                dateStr = "2023-10-27T10:06:00Z",
            )
        assertEquals("prov_444", provenance.id)
        assertEquals(
            "DocumentReference/doc_111",
            provenance.target
                .first()
                .reference
                ?.value,
        )
        assertEquals(
            "Practitioner/prac_456",
            provenance.agent
                .first()
                .who
                ?.reference
                ?.value,
        )
    }

    @Test
    fun testCreateFhirBinary() {
        val binary =
            createFhirBinary(
                id = "bin_555",
                contentTypeStr = "image/png",
                base64Data = "iVBORw0KGgo=",
            )
        assertEquals("bin_555", binary.id)
        assertEquals("image/png", binary.contentType?.value)
        assertEquals("iVBORw0KGgo=", binary.data?.value)
    }

    @Test
    fun testEmptyPatientExtensions() {
        val emptyPatient = Patient.Builder().build()
        assertEquals("", emptyPatient.mrn)
        assertEquals("", emptyPatient.customBirthDate)
        assertEquals("Unknown, Unknown", emptyPatient.fullName)
    }
}
