/**
 * @file FhirVersionConverterExhaustiveTest.kt
 * Exhaustive branch test suite for FhirVersionConverter across R4, R4B, and R5 conversions.
 */

package io.healthplatform.chartcam.fhir

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Canonical as R4Canonical
import dev.ohs.fhir.model.r4.Code as R4Code
import dev.ohs.fhir.model.r4.DateTime as R4DateTime
import dev.ohs.fhir.model.r4.Decimal as R4Decimal
import dev.ohs.fhir.model.r4.Enumeration as R4Enumeration
import dev.ohs.fhir.model.r4.FhirDate as R4FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime as R4FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal as R4FhirDecimal
import dev.ohs.fhir.model.r4.Integer as R4Integer
import dev.ohs.fhir.model.r4.Quantity as R4Quantity
import dev.ohs.fhir.model.r4.Reference as R4Reference
import dev.ohs.fhir.model.r4.String as R4String
import dev.ohs.fhir.model.r4.Uri as R4Uri
import dev.ohs.fhir.model.r4.Url as R4Url
import dev.ohs.fhir.model.r5.Canonical as R5Canonical
import dev.ohs.fhir.model.r5.Code as R5Code
import dev.ohs.fhir.model.r5.CodeableConcept as R5CodeableConcept
import dev.ohs.fhir.model.r5.Coding as R5Coding
import dev.ohs.fhir.model.r5.Enumeration as R5Enumeration
import dev.ohs.fhir.model.r5.FhirDate as R5FhirDate
import dev.ohs.fhir.model.r5.Integer as R5Integer
import dev.ohs.fhir.model.r5.Quantity as R5Quantity
import dev.ohs.fhir.model.r5.Reference as R5Reference
import dev.ohs.fhir.model.r5.String as R5String
import dev.ohs.fhir.model.r5.Uri as R5Uri
import dev.ohs.fhir.model.r5.Url as R5Url

/**
 * Exhaustive unit tests for [FhirVersionConverter] covering all enum mappings and optional fields.
 */
class FhirVersionConverterExhaustiveTest {
    /**
     * Exercises all AdministrativeGender variants and null fields for Patient R4, R5, and R4B.
     */
    @Test
    fun testAllPatientGendersAndFields() {
        val genders =
            listOf(
                dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Male,
                dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Female,
                dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Other,
                dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Unknown,
            )

        for (g in genders) {
            val r4Pat =
                dev.ohs.fhir.model.r4.Patient(
                    id = "pat-${g.code}",
                    gender = R4Enumeration(value = g),
                    active =
                        dev.ohs.fhir.model.r4
                            .Boolean(value = true),
                    birthDate =
                        dev.ohs.fhir.model.r4
                            .Date(value = R4FhirDate.fromString("1990-01-01")),
                    managingOrganization = R4Reference(reference = R4String(value = "Organization/org-1")),
                )
            val r5Pat = FhirVersionConverter.convertPatientR4ToR5(r4Pat).getOrThrow()
            assertEquals(g.code, r5Pat.gender?.value?.code)
            assertEquals(true, r5Pat.active?.value)
            assertEquals("Organization/org-1", r5Pat.managingOrganization?.reference?.value)

            val r4RoundTrip = FhirVersionConverter.convertPatientR5ToR4(r5Pat).getOrThrow()
            assertEquals(g, r4RoundTrip.gender?.value)
            assertEquals(true, r4RoundTrip.active?.value)

            val r4bPat = FhirVersionConverter.convertPatientR4ToR4B(r4Pat).getOrThrow()
            assertEquals(g.code, r4bPat.gender?.value?.code)

            val r4FromR4b = FhirVersionConverter.convertPatientR4BToR4(r4bPat).getOrThrow()
            assertEquals(g, r4FromR4b.gender?.value)
        }

        // Test with null gender, null birthDate, false active, null managingOrganization
        val r4NullFields =
            dev.ohs.fhir.model.r4.Patient(
                id = "pat-nulls",
                gender = null,
                active =
                    dev.ohs.fhir.model.r4
                        .Boolean(value = false),
                birthDate = null,
                managingOrganization = null,
            )
        val r5FromNulls = FhirVersionConverter.convertPatientR4ToR5(r4NullFields).getOrThrow()
        assertNull(r5FromNulls.gender)
        assertEquals(false, r5FromNulls.active?.value)
        assertNull(r5FromNulls.birthDate)
        assertNull(r5FromNulls.managingOrganization)

        val r4BackFromNulls = FhirVersionConverter.convertPatientR5ToR4(r5FromNulls).getOrThrow()
        assertNull(r4BackFromNulls.gender)
        assertEquals(false, r4BackFromNulls.active?.value)

        val r4bFromNulls = FhirVersionConverter.convertPatientR4ToR4B(r4NullFields).getOrThrow()
        assertNull(r4bFromNulls.gender)

        val r4FromR4bNulls = FhirVersionConverter.convertPatientR4BToR4(r4bFromNulls).getOrThrow()
        assertNull(r4FromR4bNulls.gender)

        // Test birthDate with Year and YearMonth
        val patYear =
            dev.ohs.fhir.model.r4.Patient(
                id = "pat-yr",
                birthDate =
                    dev.ohs.fhir.model.r4
                        .Date(value = R4FhirDate.fromString("1985")),
            )
        val r5Year = FhirVersionConverter.convertPatientR4ToR5(patYear).getOrThrow()
        assertTrue(r5Year.birthDate?.value is R5FhirDate.Year)
        val r4BackYear = FhirVersionConverter.convertPatientR5ToR4(r5Year).getOrThrow()
        assertTrue(r4BackYear.birthDate?.value is R4FhirDate.Year)

        val patYearMonth =
            dev.ohs.fhir.model.r4.Patient(
                id = "pat-ym",
                birthDate =
                    dev.ohs.fhir.model.r4
                        .Date(value = R4FhirDate.fromString("1985-06")),
            )
        val r5YearMonth = FhirVersionConverter.convertPatientR4ToR5(patYearMonth).getOrThrow()
        assertTrue(r5YearMonth.birthDate?.value is R5FhirDate.YearMonth)
        val r4BackYearMonth = FhirVersionConverter.convertPatientR5ToR4(r5YearMonth).getOrThrow()
        assertTrue(r4BackYearMonth.birthDate?.value is R4FhirDate.YearMonth)
    }

    /**
     * Exercises all Encounter status variants, period combinations, and class coding options.
     */
    @Test
    fun testAllEncounterStatusesAndFields() {
        val statuses =
            listOf(
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Planned,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Onleave,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Cancelled,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Entered_In_Error,
                dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Unknown,
            )

        for (status in statuses) {
            val r4Enc =
                dev.ohs.fhir.model.r4.Encounter(
                    id = "enc-${status.code}",
                    status = R4Enumeration(value = status),
                    `class` =
                        dev.ohs.fhir.model.r4.Coding(
                            system = R4Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = R4Code(value = "AMB"),
                            display = R4String(value = "ambulatory"),
                        ),
                    period =
                        dev.ohs.fhir.model.r4.Period(
                            start = R4DateTime(value = R4FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                            end = R4DateTime(value = R4FhirDateTime.fromString("2026-09-18T11:00:00Z")),
                        ),
                    subject = R4Reference(reference = R4String(value = "Patient/p-1")),
                    serviceProvider = R4Reference(reference = R4String(value = "Organization/o-1")),
                )
            val r5Enc = FhirVersionConverter.convertEncounterR4ToR5(r4Enc).getOrThrow()
            val expectedR5Code =
                when (status) {
                    dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Onleave -> "in-progress"
                    dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished -> "completed"
                    else -> status.code
                }
            assertEquals(expectedR5Code, r5Enc.status.value?.code)
            assertNotNull(r5Enc.actualPeriod)

            val r4Back = FhirVersionConverter.convertEncounterR5ToR4(r5Enc).getOrThrow()
            val expectedStatus =
                if (status == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Onleave) {
                    dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress
                } else {
                    status
                }
            assertEquals(expectedStatus, r4Back.status.value)
        }

        // Test with null status, null class coding fields, and null period
        val r4EncMinimal =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-min",
                status = R4Enumeration(value = null),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(system = null, code = null, display = null),
                period = null,
                subject = null,
                serviceProvider = null,
            )
        val r5Minimal = FhirVersionConverter.convertEncounterR4ToR5(r4EncMinimal).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress, r5Minimal.status.value)
        assertNull(r5Minimal.actualPeriod)
        assertNull(r5Minimal.subject)
        assertNull(r5Minimal.serviceProvider)

        val r4FromMin = FhirVersionConverter.convertEncounterR5ToR4(r5Minimal).getOrThrow()
        assertEquals("AMB", r4FromMin.`class`.code?.value)

        // R5 Encounter with empty class and period with null start/end
        val r5EncEmptyClass =
            dev.ohs.fhir.model.r5.Encounter(
                id = "r5-enc-ec",
                status = R5Enumeration(value = null),
                `class` = emptyList(),
                actualPeriod =
                    dev.ohs.fhir.model.r5
                        .Period(start = null, end = null),
            )
        val r4FromEmptyClass = FhirVersionConverter.convertEncounterR5ToR4(r5EncEmptyClass).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress, r4FromEmptyClass.status.value)
        assertEquals("AMB", r4FromEmptyClass.`class`.code?.value)
        assertNull(r4FromEmptyClass.period?.start)
        assertNull(r4FromEmptyClass.period?.end)

        // R5 Encounter with class concept having coding with null fields
        val r5EncConceptNullCoding =
            dev.ohs.fhir.model.r5.Encounter(
                id = "r5-enc-cnc",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Completed),
                `class` = listOf(R5CodeableConcept(coding = listOf(R5Coding(system = null, code = null, display = null)))),
            )
        val r4FromConceptNull = FhirVersionConverter.convertEncounterR5ToR4(r5EncConceptNullCoding).getOrThrow()
        assertEquals("AMB", r4FromConceptNull.`class`.code?.value)
    }

    /**
     * Exercises all Observation status variants, value mapping branches, and optional metadata fields.
     */
    @Test
    fun testAllObservationStatusesAndValues() {
        val statuses =
            listOf(
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Registered,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Preliminary,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Amended,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Corrected,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Cancelled,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Entered_In_Error,
                dev.ohs.fhir.model.r4.Observation.ObservationStatus.Unknown,
            )

        for (status in statuses) {
            val r4Obs =
                dev.ohs.fhir.model.r4.Observation(
                    id = "obs-${status.code}",
                    status = R4Enumeration(value = status),
                    code =
                        dev.ohs.fhir.model.r4
                            .CodeableConcept(text = R4String(value = "Heart rate")),
                    value =
                        dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                            R4Quantity(value = R4Decimal(value = R4FhirDecimal.fromString("72.0"))),
                        ),
                    effective =
                        dev.ohs.fhir.model.r4.Observation.Effective.DateTime(
                            R4DateTime(value = R4FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                        ),
                    issued =
                        dev.ohs.fhir.model.r4
                            .Instant(value = R4FhirDateTime.fromString("2026-09-18T10:05:00Z")),
                    subject = R4Reference(reference = R4String(value = "Patient/p-1")),
                    encounter = R4Reference(reference = R4String(value = "Encounter/e-1")),
                )
            val r5Obs = FhirVersionConverter.convertObservationR4ToR5(r4Obs).getOrThrow()
            assertEquals(status.code, r5Obs.status.value?.code)
            assertNotNull(r5Obs.value)

            val r4Back = FhirVersionConverter.convertObservationR5ToR4(r5Obs).getOrThrow()
            assertEquals(status, r4Back.status.value)
        }

        // Test observation value variants: String, Integer, Quantity with null value, Boolean (unsupported), and null
        val r4StringVal =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-str",
                status = R4Enumeration(value = null),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .String(R4String(value = "Normal")),
            )
        val r5StringVal = FhirVersionConverter.convertObservationR4ToR5(r4StringVal).getOrThrow()
        assertTrue(r5StringVal.value is dev.ohs.fhir.model.r5.Observation.Value.String)

        val r4BackStr = FhirVersionConverter.convertObservationR5ToR4(r5StringVal).getOrThrow()
        assertTrue(r4BackStr.value is dev.ohs.fhir.model.r4.Observation.Value.String)

        val r4IntVal =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-int",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .Integer(R4Integer(value = 42)),
            )
        val r5IntVal = FhirVersionConverter.convertObservationR4ToR5(r4IntVal).getOrThrow()
        assertTrue(r5IntVal.value is dev.ohs.fhir.model.r5.Observation.Value.Integer)

        val r4BackInt = FhirVersionConverter.convertObservationR5ToR4(r5IntVal).getOrThrow()
        assertTrue(r4BackInt.value is dev.ohs.fhir.model.r4.Observation.Value.Integer)

        val r4NullQuantity =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-null-q",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .Quantity(R4Quantity(value = null)),
            )
        val r5NullQ = FhirVersionConverter.convertObservationR4ToR5(r4NullQuantity).getOrThrow()
        assertNull(r5NullQ.value)

        val r5NullQuantity =
            dev.ohs.fhir.model.r5.Observation(
                id = "r5-null-q",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r5.Observation.Value
                        .Quantity(R5Quantity(value = null)),
            )
        val r4FromNullQ = FhirVersionConverter.convertObservationR5ToR4(r5NullQuantity).getOrThrow()
        assertNull(r4FromNullQ.value)

        val r4NullStringVal =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-null-str",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .String(R4String(value = null)),
            )
        assertNull(FhirVersionConverter.convertObservationR4ToR5(r4NullStringVal).getOrThrow().value)

        val r5NullStringVal =
            dev.ohs.fhir.model.r5.Observation(
                id = "r5-null-str",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r5.Observation.Value
                        .String(R5String(value = null)),
            )
        assertNull(FhirVersionConverter.convertObservationR5ToR4(r5NullStringVal).getOrThrow().value)

        val r4NullIntVal =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-null-int",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .Integer(R4Integer(value = null)),
            )
        assertNull(FhirVersionConverter.convertObservationR4ToR5(r4NullIntVal).getOrThrow().value)

        val r5NullIntVal =
            dev.ohs.fhir.model.r5.Observation(
                id = "r5-null-int",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r5.Observation.Value
                        .Integer(R5Integer(value = null)),
            )
        assertNull(FhirVersionConverter.convertObservationR5ToR4(r5NullIntVal).getOrThrow().value)

        val r4BoolVal =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-bool",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .Boolean(
                            dev.ohs.fhir.model.r4
                                .Boolean(value = true),
                        ),
            )
        assertNull(FhirVersionConverter.convertObservationR4ToR5(r4BoolVal).getOrThrow().value)

        val r5BoolVal =
            dev.ohs.fhir.model.r5.Observation(
                id = "r5-bool",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
                value =
                    dev.ohs.fhir.model.r5.Observation.Value
                        .Boolean(
                            dev.ohs.fhir.model.r5
                                .Boolean(value = true),
                        ),
            )
        assertNull(FhirVersionConverter.convertObservationR5ToR4(r5BoolVal).getOrThrow().value)
    }

    /**
     * Exercises all Questionnaire PublicationStatus variants and AnswerOption types.
     */
    @Test
    fun testAllQuestionnaireStatusesAndAnswerOptions() {
        val pubStatuses =
            listOf(
                dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Draft,
                dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active,
                dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Retired,
                dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Unknown,
            )

        for (pub in pubStatuses) {
            val r4Q =
                dev.ohs.fhir.model.r4.Questionnaire(
                    id = "q-${pub.code}",
                    status = R4Enumeration(value = pub),
                    url = R4Uri(value = "http://example.org/q-${pub.code}"),
                    title = R4String(value = "Title ${pub.code}"),
                )
            val r5Q = FhirVersionConverter.convertQuestionnaireR4ToR5(r4Q).getOrThrow()
            assertEquals(pub.code, r5Q.status.value?.code)
            assertEquals("http://example.org/q-${pub.code}", r5Q.url?.value)
            assertEquals("Title ${pub.code}", r5Q.title?.value)

            val r4Back = FhirVersionConverter.convertQuestionnaireR5ToR4(r5Q).getOrThrow()
            assertEquals(pub, r4Back.status.value)
            assertEquals("http://example.org/q-${pub.code}", r4Back.url?.value)
            assertEquals("Title ${pub.code}", r4Back.title?.value)
        }

        // Test AnswerOption variants: String (with non-null and null value), Coding (with non-null and null fields)
        val r4ItemWithOptions =
            dev.ohs.fhir.model.r4.Questionnaire.Item(
                linkId = R4String(value = "item-options"),
                type = R4Enumeration(value = dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.Choice),
                answerOption =
                    listOf(
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.String(
                                    R4String(value = "opt-str"),
                                ),
                        ),
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.String(
                                    R4String(value = null),
                                ),
                        ),
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.Coding(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system = R4Uri(value = "sys"),
                                        code = R4Code(value = "cod"),
                                        display = R4String(value = "disp"),
                                    ),
                                ),
                        ),
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.Coding(
                                    dev.ohs.fhir.model.r4
                                        .Coding(system = null, code = null, display = null),
                                ),
                        ),
                    ),
            )
        val r5Item = FhirVersionConverter.convertQuestionnaireItemR4ToR5(r4ItemWithOptions).getOrThrow()
        assertEquals(3, r5Item.answerOption.size)

        val r5ItemWithOptions =
            dev.ohs.fhir.model.r5.Questionnaire.Item(
                linkId = R5String(value = "r5-item-opt"),
                type = R5Enumeration(value = dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.String),
                answerOption =
                    listOf(
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.String(
                                    R5String(value = "opt-str"),
                                ),
                        ),
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.String(
                                    R5String(value = null),
                                ),
                        ),
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Coding(
                                    R5Coding(
                                        system = R5Uri(value = "sys"),
                                        code = R5Code(value = "cod"),
                                        display = R5String(value = "disp"),
                                    ),
                                ),
                        ),
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Coding(
                                    R5Coding(system = null, code = null, display = null),
                                ),
                        ),
                    ),
            )
        val r4FromR5Item = FhirVersionConverter.convertQuestionnaireItemR5ToR4(r5ItemWithOptions).getOrThrow()
        assertEquals(3, r4FromR5Item.answerOption.size)
    }

    /**
     * Exercises all QuestionnaireResponse statuses and DocumentReference statuses.
     */
    @Test
    fun testAllQrAndDocRefStatuses() {
        val qrStatuses =
            listOf(
                dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.In_Progress,
                dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed,
                dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Amended,
                dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Entered_In_Error,
                dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Stopped,
            )

        for (status in qrStatuses) {
            val r4Qr =
                dev.ohs.fhir.model.r4.QuestionnaireResponse(
                    id = "qr-${status.code}",
                    status = R4Enumeration(value = status),
                    questionnaire = R4Canonical(value = "Questionnaire/q-1"),
                    subject = R4Reference(reference = R4String(value = "Patient/p-1")),
                )
            val r5Qr = FhirVersionConverter.convertQuestionnaireResponseR4ToR5(r4Qr).getOrThrow()
            assertEquals(status.code, r5Qr.status.value?.code)

            val r4Back = FhirVersionConverter.convertQuestionnaireResponseR5ToR4(r5Qr).getOrThrow()
            assertEquals(status, r4Back.status.value)
        }

        val docStatuses =
            listOf(
                dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current,
                dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Superseded,
                dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Entered_In_Error,
            )

        for (docStatus in docStatuses) {
            val r4Doc =
                dev.ohs.fhir.model.r4.DocumentReference(
                    id = "doc-${docStatus.code}",
                    status = R4Enumeration(value = docStatus),
                    content =
                        listOf(
                            dev.ohs.fhir.model.r4.DocumentReference.Content(
                                attachment =
                                    dev.ohs.fhir.model.r4.Attachment(
                                        contentType = R4Code(value = "image/jpeg"),
                                        url = R4Url(value = "http://example.org/photo.jpg"),
                                    ),
                            ),
                        ),
                    subject = R4Reference(reference = R4String(value = "Patient/p-1")),
                )
            val r5Doc = FhirVersionConverter.convertDocumentReferenceR4ToR5(r4Doc).getOrThrow()
            assertEquals(docStatus.code, r5Doc.status.value?.code)

            val r4Back = FhirVersionConverter.convertDocumentReferenceR5ToR4(r5Doc).getOrThrow()
            assertEquals(docStatus, r4Back.status.value)
        }
    }

    /**
     * Exercises all Bundle types and entry combinations across R4 and R5.
     */
    @Test
    fun testAllBundleTypesAndEntries() {
        val bundleTypes =
            listOf(
                dev.ohs.fhir.model.r4.Bundle.BundleType.Document,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Message,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction_Response,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Batch,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Batch_Response,
                dev.ohs.fhir.model.r4.Bundle.BundleType.History,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Searchset,
                dev.ohs.fhir.model.r4.Bundle.BundleType.Collection,
            )

        for (bt in bundleTypes) {
            val r4Bundle =
                dev.ohs.fhir.model.r4.Bundle(
                    id = "bundle-${bt.code}",
                    type = R4Enumeration(value = bt),
                    entry =
                        listOf(
                            dev.ohs.fhir.model.r4.Bundle.Entry(
                                fullUrl = R4Uri(value = "urn:uuid:p1"),
                                resource =
                                    dev.ohs.fhir.model.r4
                                        .Patient(id = "p1"),
                            ),
                            dev.ohs.fhir.model.r4.Bundle.Entry(
                                fullUrl = null,
                                resource = null,
                            ),
                            dev.ohs.fhir.model.r4.Bundle.Entry(
                                fullUrl = R4Uri(value = "urn:uuid:org1"),
                                resource =
                                    dev.ohs.fhir.model.r4
                                        .Organization(id = "org1"),
                            ),
                        ),
                )
            val r5Bundle = FhirVersionConverter.convertBundleR4ToR5(r4Bundle).getOrThrow()
            assertEquals(bt.code, r5Bundle.type.value?.code)
            assertEquals(3, r5Bundle.entry.size)
            assertNotNull(r5Bundle.entry[0].resource)
            assertNull(r5Bundle.entry[1].resource)
            assertNull(r5Bundle.entry[2].resource) // Unsupported Organization returns null resource in entry

            val r4Back = FhirVersionConverter.convertBundleR5ToR4(r5Bundle).getOrThrow()
            assertEquals(bt, r4Back.type.value)
            assertEquals(3, r4Back.entry.size)
            assertNotNull(r4Back.entry[0].resource)
            assertNull(r4Back.entry[1].resource)
            assertNull(r4Back.entry[2].resource)
        }
    }

    /**
     * Tests polymorphic convertResourceR4ToR5 and convertResourceR5ToR4 dispatch.
     */
    @Test
    fun testPolymorphicResourceConversion() {
        val r4Patient =
            dev.ohs.fhir.model.r4
                .Patient(id = "poly-pat")
        val r4Encounter =
            dev.ohs.fhir.model.r4.Encounter(
                id = "poly-enc",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = R4Code(value = "AMB")),
            )
        val r4Observation =
            dev.ohs.fhir.model.r4.Observation(
                id = "poly-obs",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
            )
        val r4Questionnaire =
            dev.ohs.fhir.model.r4.Questionnaire(
                id = "poly-q",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
            )
        val r4Qr =
            dev.ohs.fhir.model.r4.QuestionnaireResponse(
                id = "poly-qr",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )
        val r4DocRef =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "poly-doc",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content = emptyList(),
            )
        val r4Provenance =
            dev.ohs.fhir.model.r4.Provenance(
                id = "poly-prov",
                target = emptyList(),
                recorded =
                    dev.ohs.fhir.model.r4
                        .Instant(value = R4FhirDateTime.fromString("2026-09-18T12:00:00Z")),
                agent =
                    listOf(
                        dev.ohs.fhir.model.r4.Provenance
                            .Agent(who = R4Reference(reference = R4String(value = "Practitioner/p1"))),
                    ),
            )

        val r4Resources: List<dev.ohs.fhir.model.r4.Resource> =
            listOf(
                r4Patient,
                r4Encounter,
                r4Observation,
                r4Questionnaire,
                r4Qr,
                r4DocRef,
                r4Provenance,
            )

        for (res in r4Resources) {
            val r5Res = FhirVersionConverter.convertResourceR4ToR5(res).getOrThrow()
            assertNotNull(r5Res)
            val r4Back = FhirVersionConverter.convertResourceR5ToR4(r5Res).getOrThrow()
            assertNotNull(r4Back)
            assertEquals(res.id, r4Back.id)
        }

        // Test unsupported resource types
        val unsupportedR4 =
            dev.ohs.fhir.model.r4
                .Organization(id = "unsupp-org")
        val failR4 = FhirVersionConverter.convertResourceR4ToR5(unsupportedR4)
        assertTrue(failR4.isFailure)

        val unsupportedR5 =
            dev.ohs.fhir.model.r5
                .Organization(id = "unsupp-r5-org")
        val failR5 = FhirVersionConverter.convertResourceR5ToR4(unsupportedR5)
        assertTrue(failR5.isFailure)
    }

    /**
     * Exercises all null-wrapper fields and fallback branches across resources.
     */
    @Test
    fun testFhirVersionConverterEdgeBranches() {
        // 1. Patient with null-wrapper fields in name, identifier, managingOrganization across R4, R5, R4B
        val r4PatEdge =
            dev.ohs.fhir.model.r4.Patient(
                id = "pat-edge",
                name =
                    listOf(
                        dev.ohs.fhir.model.r4.HumanName(
                            family = null,
                            given = listOf(R4String(value = null), R4String(value = "Alice")),
                        ),
                    ),
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            system = R4Uri(value = null),
                            value = R4String(value = null),
                        ),
                    ),
                managingOrganization = R4Reference(reference = R4String(value = null)),
            )
        val r5PatEdge = FhirVersionConverter.convertPatientR4ToR5(r4PatEdge).getOrThrow()
        assertNull(r5PatEdge.managingOrganization)
        assertNull(r5PatEdge.name.first().family)
        assertEquals(
            1,
            r5PatEdge.name
                .first()
                .given.size,
        )
        assertNull(r5PatEdge.identifier.first().system)
        assertNull(r5PatEdge.identifier.first().value)

        val r4BackEdge = FhirVersionConverter.convertPatientR5ToR4(r5PatEdge).getOrThrow()
        assertNull(r4BackEdge.managingOrganization)
        assertNull(r4BackEdge.name.first().family)
        assertEquals(
            1,
            r4BackEdge.name
                .first()
                .given.size,
        )

        val r4bPatEdge = FhirVersionConverter.convertPatientR4ToR4B(r4PatEdge).getOrThrow()
        assertNull(r4bPatEdge.name.first().family)
        assertEquals(
            1,
            r4bPatEdge.name
                .first()
                .given.size,
        )

        val r4FromR4bEdge = FhirVersionConverter.convertPatientR4BToR4(r4bPatEdge).getOrThrow()
        assertNull(r4FromR4bEdge.name.first().family)
        assertEquals(
            1,
            r4FromR4bEdge.name
                .first()
                .given.size,
        )

        // 2. Encounter with null-wrapper fields in class coding, period, subject, serviceProvider
        val r4EncEdge =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-edge",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4.Coding(
                        system = R4Uri(value = null),
                        code = R4Code(value = null),
                        display = R4String(value = null),
                    ),
                period =
                    dev.ohs.fhir.model.r4
                        .Period(start = R4DateTime(value = null), end = R4DateTime(value = null)),
                subject = R4Reference(reference = R4String(value = null)),
                serviceProvider = R4Reference(reference = R4String(value = null)),
            )
        val r5EncEdge = FhirVersionConverter.convertEncounterR4ToR5(r4EncEdge).getOrThrow()
        assertNull(r5EncEdge.subject)
        assertNull(r5EncEdge.serviceProvider)
        assertNull(r5EncEdge.actualPeriod?.start)
        assertNull(r5EncEdge.actualPeriod?.end)

        val r4EncBack = FhirVersionConverter.convertEncounterR5ToR4(r5EncEdge).getOrThrow()
        assertNull(r4EncBack.subject)
        assertNull(r4EncBack.serviceProvider)
        assertNull(r4EncBack.period?.start)
        assertNull(r4EncBack.period?.end)

        // 3. Observation with null coding fields, null text, and quantity with unit having null value
        val r4ObsEdge =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-edge",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4.CodeableConcept(
                        coding =
                            listOf(
                                dev.ohs.fhir.model.r4.Coding(
                                    system = R4Uri(value = null),
                                    code = R4Code(value = null),
                                    display = R4String(value = null),
                                ),
                            ),
                        text = R4String(value = null),
                    ),
                subject = R4Reference(reference = R4String(value = null)),
                encounter = R4Reference(reference = R4String(value = null)),
                value =
                    dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                        R4Quantity(
                            value = R4Decimal(value = R4FhirDecimal.fromString("98.6")),
                            unit = R4String(value = null),
                        ),
                    ),
            )
        val r5ObsEdge = FhirVersionConverter.convertObservationR4ToR5(r4ObsEdge).getOrThrow()
        assertNull(r5ObsEdge.subject)
        assertNull(r5ObsEdge.encounter)
        assertNull(r5ObsEdge.code.text)

        val r4ObsBack = FhirVersionConverter.convertObservationR5ToR4(r5ObsEdge).getOrThrow()
        assertNull(r4ObsBack.subject)
        assertNull(r4ObsBack.encounter)
        assertNull(r4ObsBack.code.text)

        // 4. Questionnaire with null url, null title, null item text, null item repeats/required
        val r4QEdge =
            dev.ohs.fhir.model.r4.Questionnaire(
                id = "q-edge",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                url = R4Uri(value = null),
                title = R4String(value = null),
                item =
                    listOf(
                        dev.ohs.fhir.model.r4.Questionnaire.Item(
                            linkId = R4String(value = null),
                            type = R4Enumeration(value = dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.String),
                            text = R4String(value = null),
                            required =
                                dev.ohs.fhir.model.r4
                                    .Boolean(value = null),
                            repeats =
                                dev.ohs.fhir.model.r4
                                    .Boolean(value = null),
                        ),
                    ),
            )
        val r5QEdge = FhirVersionConverter.convertQuestionnaireR4ToR5(r4QEdge).getOrThrow()
        assertNull(r5QEdge.url)
        assertNull(r5QEdge.title)
        assertNull(r5QEdge.item.first().text)
        assertNull(r5QEdge.item.first().required)
        assertNull(r5QEdge.item.first().repeats)

        val r4QBack = FhirVersionConverter.convertQuestionnaireR5ToR4(r5QEdge).getOrThrow()
        assertNull(r4QBack.url)
        assertNull(r4QBack.title)

        // 5. QuestionnaireResponse with null canonical questionnaire, null encounter, null author
        val r4QrEdge =
            dev.ohs.fhir.model.r4.QuestionnaireResponse(
                id = "qr-edge",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                questionnaire = R4Canonical(value = null),
                encounter = R4Reference(reference = R4String(value = null)),
                author = R4Reference(reference = R4String(value = null)),
            )
        val r5QrEdge = FhirVersionConverter.convertQuestionnaireResponseR4ToR5(r4QrEdge).getOrThrow()
        assertEquals("", r5QrEdge.questionnaire.value)
        assertNull(r5QrEdge.encounter)
        assertNull(r5QrEdge.author)

        val r4QrBack = FhirVersionConverter.convertQuestionnaireResponseR5ToR4(r5QrEdge).getOrThrow()
        assertEquals("", r4QrBack.questionnaire?.value)
        assertNull(r4QrBack.encounter)
        assertNull(r4QrBack.author)

        // 6. DocumentReference with attachment having null contentType and null url
        val r4DocEdge =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "doc-edge",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content =
                    listOf(
                        dev.ohs.fhir.model.r4.DocumentReference.Content(
                            attachment =
                                dev.ohs.fhir.model.r4.Attachment(
                                    contentType = R4Code(value = null),
                                    url = R4Url(value = null),
                                ),
                        ),
                    ),
            )
        val r5DocEdge = FhirVersionConverter.convertDocumentReferenceR4ToR5(r4DocEdge).getOrThrow()
        assertNull(
            r5DocEdge.content
                .first()
                .attachment.contentType,
        )
        assertNull(
            r5DocEdge.content
                .first()
                .attachment.url,
        )

        val r4DocBack = FhirVersionConverter.convertDocumentReferenceR5ToR4(r5DocEdge).getOrThrow()
        assertNull(
            r4DocBack.content
                .first()
                .attachment.contentType,
        )
        assertNull(
            r4DocBack.content
                .first()
                .attachment.url,
        )

        // 7. Provenance with null target references, null recorded date, empty agents, and agent with null who
        val r4ProvEdge =
            dev.ohs.fhir.model.r4.Provenance(
                id = "prov-edge",
                target = listOf(R4Reference(reference = null), R4Reference(reference = R4String(value = null))),
                recorded =
                    dev.ohs.fhir.model.r4
                        .Instant(value = null),
                agent =
                    listOf(
                        dev.ohs.fhir.model.r4.Provenance
                            .Agent(who = R4Reference(reference = null)),
                    ),
            )
        val r5ProvEdge = FhirVersionConverter.convertProvenanceR4ToR5(r4ProvEdge).getOrThrow()
        assertTrue(r5ProvEdge.target.isEmpty())
        assertNull(r5ProvEdge.recorded)
        assertEquals(
            "Practitioner/unknown",
            r5ProvEdge.agent
                .first()
                .who.reference
                ?.value,
        )

        // Test Provenance with empty agent list triggering fallback default agent
        val r4ProvEmptyAgent =
            dev.ohs.fhir.model.r4.Provenance(
                id = "prov-empty-agent",
                target = emptyList(),
                recorded =
                    dev.ohs.fhir.model.r4
                        .Instant(value = null),
                agent = emptyList(),
            )
        val r5ProvFromEmpty = FhirVersionConverter.convertProvenanceR4ToR5(r4ProvEmptyAgent).getOrThrow()
        assertEquals(
            "Practitioner/chartcam-device",
            r5ProvFromEmpty.agent
                .first()
                .who.reference
                ?.value,
        )

        val r5ProvEmptyAgent =
            dev.ohs.fhir.model.r5.Provenance(
                id = "r5-prov-ea",
                target = listOf(R5Reference(reference = null), R5Reference(reference = R5String(value = null))),
                recorded = null,
                agent = emptyList(),
            )
        val r4ProvFromR5 = FhirVersionConverter.convertProvenanceR5ToR4(r5ProvEmptyAgent).getOrThrow()
        assertEquals(
            "Practitioner/chartcam-device",
            r4ProvFromR5.agent
                .first()
                .who.reference
                ?.value,
        )
        assertTrue(r4ProvFromR5.target.isEmpty())

        // 8. Bundle with entry having null fullUrl value
        val r4BundleEdge =
            dev.ohs.fhir.model.r4.Bundle(
                id = "b-edge",
                type = R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Collection),
                entry =
                    listOf(
                        dev.ohs.fhir.model.r4.Bundle.Entry(
                            fullUrl = R4Uri(value = null),
                            resource =
                                dev.ohs.fhir.model.r4
                                    .Patient(id = "pat-in-b"),
                        ),
                    ),
            )
        val r5BundleEdge = FhirVersionConverter.convertBundleR4ToR5(r4BundleEdge).getOrThrow()
        assertNull(r5BundleEdge.entry.first().fullUrl)

        val r4BundleBack = FhirVersionConverter.convertBundleR5ToR4(r5BundleEdge).getOrThrow()
        assertNull(r4BundleBack.entry.first().fullUrl)

        // 9. Arrived and Triaged encounter statuses
        val encArrived =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-arr",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Arrived),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = R4Code(value = "AMB")),
            )
        assertEquals(
            dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress,
            FhirVersionConverter
                .convertEncounterR4ToR5(encArrived)
                .getOrThrow()
                .status.value,
        )

        val encTriaged =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-tri",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Triaged),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = R4Code(value = "AMB")),
            )
        assertEquals(
            dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress,
            FhirVersionConverter
                .convertEncounterR4ToR5(encTriaged)
                .getOrThrow()
                .status.value,
        )

        // 10. Null-gender wrappers for Patient
        val r4PatNullGender =
            dev.ohs.fhir.model.r4
                .Patient(id = "png-4", gender = R4Enumeration(value = null))
        assertNull(FhirVersionConverter.convertPatientR4ToR5(r4PatNullGender).getOrThrow().gender)
        assertNull(FhirVersionConverter.convertPatientR4ToR4B(r4PatNullGender).getOrThrow().gender)

        val r5PatNullGender =
            dev.ohs.fhir.model.r5
                .Patient(id = "png-5", gender = R5Enumeration(value = null))
        assertNull(FhirVersionConverter.convertPatientR5ToR4(r5PatNullGender).getOrThrow().gender)

        val r4bPatNullGender =
            dev.ohs.fhir.model.r4b
                .Patient(
                    id = "png-4b",
                    gender =
                        dev.ohs.fhir.model.r4b
                            .Enumeration(value = null),
                )
        assertNull(FhirVersionConverter.convertPatientR4BToR4(r4bPatNullGender).getOrThrow().gender)

        // 11. R5 Patient with null-wrapped primitives
        val r5PatWithNullWrappers =
            dev.ohs.fhir.model.r5.Patient(
                id = "p5-nw",
                name =
                    listOf(
                        dev.ohs.fhir.model.r5
                            .HumanName(family = R5String(value = null), given = listOf(R5String(value = null))),
                    ),
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r5
                            .Identifier(system = R5Uri(value = null), value = R5String(value = null)),
                    ),
                active =
                    dev.ohs.fhir.model.r5
                        .Boolean(value = null),
                managingOrganization = R5Reference(reference = R5String(value = null)),
            )
        val r4FromR5Nw = FhirVersionConverter.convertPatientR5ToR4(r5PatWithNullWrappers).getOrThrow()
        assertNull(r4FromR5Nw.name.first().family)
        assertTrue(
            r4FromR5Nw.name
                .first()
                .given
                .isEmpty(),
        )
        assertNull(r4FromR5Nw.identifier.first().system)
        assertNull(r4FromR5Nw.identifier.first().value)
        assertNull(r4FromR5Nw.active)
        assertNull(r4FromR5Nw.managingOrganization)

        // 12. Null statuses across resources
        val obsNullStatR4 =
            dev.ohs.fhir.model.r4.Observation(
                id = "o-ns4",
                status = R4Enumeration(value = null),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
            )
        assertEquals(
            dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final,
            FhirVersionConverter
                .convertObservationR4ToR5(obsNullStatR4)
                .getOrThrow()
                .status.value,
        )
        val obsNullStatR5 =
            dev.ohs.fhir.model.r5.Observation(
                id = "o-ns5",
                status = R5Enumeration(value = null),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
            )
        assertEquals(
            dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final,
            FhirVersionConverter
                .convertObservationR5ToR4(obsNullStatR5)
                .getOrThrow()
                .status.value,
        )

        val qNullStatR4 =
            dev.ohs.fhir.model.r4
                .Questionnaire(id = "q-ns4", status = R4Enumeration(value = null))
        assertEquals(
            dev.ohs.fhir.model.r5.terminologies.PublicationStatus.Active,
            FhirVersionConverter
                .convertQuestionnaireR4ToR5(qNullStatR4)
                .getOrThrow()
                .status.value,
        )
        val qNullStatR5 =
            dev.ohs.fhir.model.r5
                .Questionnaire(id = "q-ns5", status = R5Enumeration(value = null))
        assertEquals(
            dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active,
            FhirVersionConverter
                .convertQuestionnaireR5ToR4(qNullStatR5)
                .getOrThrow()
                .status.value,
        )

        val qrNullStatR4 =
            dev.ohs.fhir.model.r4
                .QuestionnaireResponse(id = "qr-ns4", status = R4Enumeration(value = null))
        assertEquals(
            dev.ohs.fhir.model.r5.QuestionnaireResponse.QuestionnaireResponseStatus.Completed,
            FhirVersionConverter
                .convertQuestionnaireResponseR4ToR5(qrNullStatR4)
                .getOrThrow()
                .status.value,
        )
        val qrNullStatR5 =
            dev.ohs.fhir.model.r5.QuestionnaireResponse(
                id = "qr-ns5",
                status = R5Enumeration(value = null),
                questionnaire = R5Canonical(value = ""),
            )
        assertEquals(
            dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed,
            FhirVersionConverter
                .convertQuestionnaireResponseR5ToR4(qrNullStatR5)
                .getOrThrow()
                .status.value,
        )

        val docNullStatR4 =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "d-ns4",
                status = R4Enumeration(value = null),
                content = emptyList(),
            )
        assertEquals(
            dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Current,
            FhirVersionConverter
                .convertDocumentReferenceR4ToR5(docNullStatR4)
                .getOrThrow()
                .status.value,
        )
        val docNullStatR5 =
            dev.ohs.fhir.model.r5.DocumentReference(
                id = "d-ns5",
                status = R5Enumeration(value = null),
                content = emptyList(),
            )
        assertEquals(
            dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current,
            FhirVersionConverter
                .convertDocumentReferenceR5ToR4(docNullStatR5)
                .getOrThrow()
                .status.value,
        )

        val bNullTypeR4 =
            dev.ohs.fhir.model.r4
                .Bundle(id = "b-nt4", type = R4Enumeration(value = null))
        assertEquals(
            dev.ohs.fhir.model.r5.Bundle.BundleType.Collection,
            FhirVersionConverter
                .convertBundleR4ToR5(bNullTypeR4)
                .getOrThrow()
                .type.value,
        )
        val bNullTypeR5 =
            dev.ohs.fhir.model.r5
                .Bundle(id = "b-nt5", type = R5Enumeration(value = null))
        assertEquals(
            dev.ohs.fhir.model.r4.Bundle.BundleType.Collection,
            FhirVersionConverter
                .convertBundleR5ToR4(bNullTypeR5)
                .getOrThrow()
                .type.value,
        )

        // 13. Bundle Subscription_Notification and unsupported R5 resource in R5 bundle
        val bSubNotif =
            dev.ohs.fhir.model.r5.Bundle(
                id = "b-sub",
                type = R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Subscription_Notification),
                entry =
                    listOf(
                        dev.ohs.fhir.model.r5.Bundle
                            .Entry(
                                resource =
                                    dev.ohs.fhir.model.r5
                                        .Organization(id = "unsupp-in-r5-b"),
                            ),
                    ),
            )
        val r4FromSub = FhirVersionConverter.convertBundleR5ToR4(bSubNotif).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.Bundle.BundleType.Collection, r4FromSub.type.value)
        assertNull(r4FromSub.entry.first().resource)

        // 14. Provenance with blank who reference
        val provBlankWhoR4 =
            dev.ohs.fhir.model.r4.Provenance(
                id = "p-bw4",
                target = emptyList(),
                recorded =
                    dev.ohs.fhir.model.r4
                        .Instant(value = R4FhirDateTime.fromString("2026-09-17T00:00:00Z")),
                agent =
                    listOf(
                        dev.ohs.fhir.model.r4.Provenance
                            .Agent(who = R4Reference(reference = R4String(value = "   "))),
                    ),
            )
        assertEquals(
            "Practitioner/unknown",
            FhirVersionConverter
                .convertProvenanceR4ToR5(provBlankWhoR4)
                .getOrThrow()
                .agent
                .first()
                .who.reference
                ?.value,
        )

        val provBlankWhoR5 =
            dev.ohs.fhir.model.r5.Provenance(
                id = "p-bw5",
                target = emptyList(),
                agent =
                    listOf(
                        dev.ohs.fhir.model.r5.Provenance
                            .Agent(who = R5Reference(reference = R5String(value = "   "))),
                    ),
            )
        assertEquals(
            "Practitioner/unknown",
            FhirVersionConverter
                .convertProvenanceR5ToR4(provBlankWhoR5)
                .getOrThrow()
                .agent
                .first()
                .who.reference
                ?.value,
        )

        // 15. Questionnaire answerOption with integer (unsupported type)
        val qItemIntOptR4 =
            dev.ohs.fhir.model.r4.Questionnaire.Item(
                linkId = R4String(value = "i-io"),
                type = R4Enumeration(value = dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.Choice),
                answerOption =
                    listOf(
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value
                                    .Integer(R4Integer(value = 99)),
                        ),
                    ),
            )
        assertTrue(
            FhirVersionConverter
                .convertQuestionnaireItemR4ToR5(qItemIntOptR4)
                .getOrThrow()
                .answerOption
                .isEmpty(),
        )

        val qItemIntOptR5 =
            dev.ohs.fhir.model.r5.Questionnaire.Item(
                linkId = R5String(value = "i-io5"),
                type = R5Enumeration(value = dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.String),
                answerOption =
                    listOf(
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value
                                    .Integer(R5Integer(value = 99)),
                        ),
                    ),
            )
        assertTrue(
            FhirVersionConverter
                .convertQuestionnaireItemR5ToR4(qItemIntOptR5)
                .getOrThrow()
                .answerOption
                .isEmpty(),
        )

        // 16. DocumentReference with attachment having null-wrapped code and url, and attachment with completely null url
        val docR5NullWrappers =
            dev.ohs.fhir.model.r5.DocumentReference(
                id = "d5-nw",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Current),
                content =
                    listOf(
                        dev.ohs.fhir.model.r5.DocumentReference.Content(
                            attachment =
                                dev.ohs.fhir.model.r5
                                    .Attachment(contentType = R5Code(value = null), url = R5Url(value = null)),
                        ),
                        dev.ohs.fhir.model.r5.DocumentReference.Content(
                            attachment =
                                dev.ohs.fhir.model.r5
                                    .Attachment(contentType = null, url = null),
                        ),
                    ),
            )
        val docR4FromNw = FhirVersionConverter.convertDocumentReferenceR5ToR4(docR5NullWrappers).getOrThrow()
        assertNull(
            docR4FromNw.content
                .first()
                .attachment.contentType,
        )
        assertNull(
            docR4FromNw.content
                .first()
                .attachment.url,
        )
        assertNull(docR4FromNw.content[1].attachment.url)

        val docR4NullUrl =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "d4-nu",
                status = R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content =
                    listOf(
                        dev.ohs.fhir.model.r4.DocumentReference.Content(
                            attachment =
                                dev.ohs.fhir.model.r4
                                    .Attachment(contentType = null, url = null),
                        ),
                    ),
            )
        val docR5FromNu = FhirVersionConverter.convertDocumentReferenceR4ToR5(docR4NullUrl).getOrThrow()
        assertNull(
            docR5FromNu.content
                .first()
                .attachment.url,
        )

        // 17. Encounter R5 with concept having empty coding list
        val encR5EmptyCoding =
            dev.ohs.fhir.model.r5.Encounter(
                id = "e5-ec",
                status = R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress),
                `class` = listOf(R5CodeableConcept(coding = emptyList())),
            )
        val encR4FromEc = FhirVersionConverter.convertEncounterR5ToR4(encR5EmptyCoding).getOrThrow()
        assertEquals("AMB", encR4FromEc.`class`.code?.value)

        // 18. Questionnaire item R5 with Coding type (non-R4) and null linkId
        val qItemR5CodingType =
            dev.ohs.fhir.model.r5.Questionnaire.Item(
                linkId = R5String(value = null),
                type = R5Enumeration(value = dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.Coding),
            )
        val qItemR4FromCoding = FhirVersionConverter.convertQuestionnaireItemR5ToR4(qItemR5CodingType).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.String, qItemR4FromCoding.type.value)
        assertEquals("", qItemR4FromCoding.linkId.value)
    }
}
