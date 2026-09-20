/**
 * @file FhirVersionConverterTest.kt
 * Unit tests for FhirVersionConverter verifying offline R4 to R5 resource conversions.
 */
package io.healthplatform.chartcam.fhir

import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Decimal as R4Decimal
import dev.ohs.fhir.model.r4.Enumeration as R4Enumeration
import dev.ohs.fhir.model.r4.FhirDecimal as R4FhirDecimal
import dev.ohs.fhir.model.r4.Observation as R4Observation
import dev.ohs.fhir.model.r4.Quantity as R4Quantity
import dev.ohs.fhir.model.r4.String as R4String

/**
 * Unit tests for [FhirVersionConverter].
 */
class FhirVersionConverterTest {
    /**
     * Verifies that Patient resources can be converted between R4 and R5 in memory without loss.
     */
    @Test
    fun testPatientConversionRoundTrip() {
        val r4Patient =
            createFhirPatient(
                id = "pat-r4",
                firstName = "Grace",
                lastName = "Hopper",
                dob = LocalDate(1906, 12, 9),
                mrnValue = "MRN-GH-01",
                gender = "female",
            )

        val r5Result = FhirVersionConverter.convertPatientR4ToR5(r4Patient)
        assertTrue(r5Result.isSuccess)
        val r5Patient = r5Result.getOrNull()
        assertNotNull(r5Patient)
        assertEquals("pat-r4", r5Patient.id)
        assertEquals(
            "Hopper",
            r5Patient.name
                .firstOrNull()
                ?.family
                ?.value,
        )
        assertEquals(
            "Grace",
            r5Patient.name
                .firstOrNull()
                ?.given
                ?.firstOrNull()
                ?.value,
        )
        assertEquals("female", r5Patient.gender?.value?.code)

        val roundTripResult = FhirVersionConverter.convertPatientR5ToR4(r5Patient)
        assertTrue(roundTripResult.isSuccess)
        val roundTrip = roundTripResult.getOrNull()
        assertNotNull(roundTrip)
        assertEquals(r4Patient.id, roundTrip.id)
        assertEquals(r4Patient.gender?.value?.code, roundTrip.gender?.value?.code)
        assertEquals(
            r4Patient.name
                .firstOrNull()
                ?.family
                ?.value,
            roundTrip.name
                .firstOrNull()
                ?.family
                ?.value,
        )
    }

    /**
     * Verifies that Encounter resources can be converted between R4 and R5 in memory.
     */
    @Test
    fun testEncounterConversionRoundTrip() {
        val r4Encounter =
            createFhirEncounter(
                id = "enc-r4",
                patientId = "Patient/pat-1",
                practitionerId = "Practitioner/prac-1",
                dateStr = "2026-09-17T10:00:00Z",
            )

        val r5Result = FhirVersionConverter.convertEncounterR4ToR5(r4Encounter)
        assertTrue(r5Result.isSuccess)
        val r5Encounter = r5Result.getOrNull()
        assertNotNull(r5Encounter)
        assertEquals("enc-r4", r5Encounter.id)
        assertEquals("Patient/pat-1", r5Encounter.subject?.reference?.value)

        val roundTripResult = FhirVersionConverter.convertEncounterR5ToR4(r5Encounter)
        assertTrue(roundTripResult.isSuccess)
        val roundTrip = roundTripResult.getOrNull()
        assertNotNull(roundTrip)
        assertEquals(r4Encounter.id, roundTrip.id)
        assertEquals(r4Encounter.subject?.reference?.value, roundTrip.subject?.reference?.value)
    }

    /**
     * Verifies that Observation resources can be converted between R4 and R5 in memory.
     */
    @Test
    fun testObservationConversionRoundTrip() {
        val r4Obs =
            R4Observation(
                id = "obs-r4",
                status = R4Enumeration(value = R4Observation.ObservationStatus.Final),
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
                                            .Code(value = "72514-3"),
                                    display = R4String(value = "Pain severity"),
                                ),
                            ),
                    ),
                value =
                    R4Observation.Value.Quantity(
                        R4Quantity(value = R4Decimal(value = R4FhirDecimal.fromString("4.5"))),
                    ),
            )

        val r5Result = FhirVersionConverter.convertObservationR4ToR5(r4Obs)
        assertTrue(r5Result.isSuccess)
        val r5Obs = r5Result.getOrNull()
        assertNotNull(r5Obs)
        assertEquals("obs-r4", r5Obs.id)
        assertEquals(
            "72514-3",
            r5Obs.code.coding
                .firstOrNull()
                ?.code
                ?.value,
        )

        val roundTripResult = FhirVersionConverter.convertObservationR5ToR4(r5Obs)
        assertTrue(roundTripResult.isSuccess)
        val roundTrip = roundTripResult.getOrNull()
        assertNotNull(roundTrip)
        assertEquals(r4Obs.id, roundTrip.id)
        assertEquals(
            "72514-3",
            roundTrip.code.coding
                .firstOrNull()
                ?.code
                ?.value,
        )
    }
}
