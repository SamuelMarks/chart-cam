/**
 * @file FhirVersionExpansionTest.kt
 * Tests verifying cross-version conversions between FHIR R4, R5, and R4B for extended resources.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.Url
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests for R4 <-> R5 and R4 <-> R4B conversions.
 */
class FhirVersionExpansionTest {
    /**
     * Tests Questionnaire round-trip conversion including recursive item hierarchy.
     */
    @Test
    fun testQuestionnaireConversion() {
        val subItem =
            Questionnaire.Item(
                linkId = FhirString(value = "sub-q1"),
                text = FhirString(value = "Sub Question"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        val r4Q =
            Questionnaire(
                id = "q-ver-1",
                url = Uri(value = "http://example.com/q1"),
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = "Vitals Assessment"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "group-1"),
                            text = FhirString(value = "General Group"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            item = listOf(subItem),
                        ),
                    ),
            )
        val r5Result = FhirVersionConverter.convertQuestionnaireR4ToR5(r4Q)
        assertTrue(r5Result.isSuccess)
        val r5Q = r5Result.getOrThrow()
        assertEquals("q-ver-1", r5Q.id)
        assertEquals("Vitals Assessment", r5Q.title?.value)
        assertEquals(1, r5Q.item.size)
        assertEquals(
            "group-1",
            r5Q.item
                .first()
                .linkId.value,
        )
        assertEquals(
            1,
            r5Q.item
                .first()
                .item.size,
        )
        assertEquals(
            "sub-q1",
            r5Q.item
                .first()
                .item
                .first()
                .linkId.value,
        )

        val backResult = FhirVersionConverter.convertQuestionnaireR5ToR4(r5Q)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Q.id, r4Back.id)
        assertEquals(r4Q.title?.value, r4Back.title?.value)
        assertEquals(1, r4Back.item.size)
        assertEquals(
            1,
            r4Back.item
                .first()
                .item.size,
        )
    }

    /**
     * Tests QuestionnaireResponse round-trip conversion.
     */
    @Test
    fun testQuestionnaireResponseConversion() {
        val r4Qr =
            QuestionnaireResponse(
                id = "qr-ver-1",
                questionnaire = Canonical(value = "Questionnaire/q-1"),
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                subject = Reference(reference = FhirString(value = "Patient/p-1")),
            )
        val r5Result = FhirVersionConverter.convertQuestionnaireResponseR4ToR5(r4Qr)
        assertTrue(r5Result.isSuccess)
        val r5Qr = r5Result.getOrThrow()
        assertEquals("qr-ver-1", r5Qr.id)
        assertEquals("Questionnaire/q-1", r5Qr.questionnaire.value)

        val backResult = FhirVersionConverter.convertQuestionnaireResponseR5ToR4(r5Qr)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Qr.id, r4Back.id)
        assertEquals(r4Qr.questionnaire?.value, r4Back.questionnaire?.value)
    }

    /**
     * Tests DocumentReference round-trip conversion.
     */
    @Test
    fun testDocumentReferenceConversion() {
        val r4Doc =
            DocumentReference(
                id = "doc-ver-1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content =
                    listOf(
                        DocumentReference.Content(
                            attachment =
                                Attachment(
                                    contentType = Code(value = "image/jpeg"),
                                    url = Url(value = "files/photo.jpg"),
                                ),
                        ),
                    ),
                subject = Reference(reference = FhirString(value = "Patient/p-1")),
            )
        val r5Result = FhirVersionConverter.convertDocumentReferenceR4ToR5(r4Doc)
        assertTrue(r5Result.isSuccess)
        val r5Doc = r5Result.getOrThrow()
        assertEquals("doc-ver-1", r5Doc.id)
        assertEquals(
            "image/jpeg",
            r5Doc.content
                .firstOrNull()
                ?.attachment
                ?.contentType
                ?.value,
        )

        val backResult = FhirVersionConverter.convertDocumentReferenceR5ToR4(r5Doc)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Doc.id, r4Back.id)
        assertEquals(
            "files/photo.jpg",
            r4Back.content
                .firstOrNull()
                ?.attachment
                ?.url
                ?.value,
        )
    }

    /**
     * Tests Provenance round-trip conversion.
     */
    @Test
    fun testProvenanceConversion() {
        val r4Prov =
            Provenance(
                id = "prov-ver-1",
                target = listOf(Reference(reference = FhirString(value = "Encounter/enc-1"))),
                recorded =
                    Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString("2026-09-17T12:00:00Z"),
                    ),
                agent =
                    listOf(
                        Provenance.Agent(
                            who = Reference(reference = FhirString(value = "Practitioner/dr-smith")),
                        ),
                    ),
            )
        val r5Result = FhirVersionConverter.convertProvenanceR4ToR5(r4Prov)
        assertTrue(r5Result.isSuccess)
        val r5Prov = r5Result.getOrThrow()
        assertEquals("prov-ver-1", r5Prov.id)
        assertEquals(
            "Encounter/enc-1",
            r5Prov.target
                .firstOrNull()
                ?.reference
                ?.value,
        )
        assertTrue(r5Prov.agent.isNotEmpty(), "R5 Provenance agent must satisfy 1..* cardinality")
        assertEquals(
            "Practitioner/dr-smith",
            r5Prov.agent
                .first()
                .who.reference
                ?.value,
        )

        val backResult = FhirVersionConverter.convertProvenanceR5ToR4(r5Prov)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Prov.id, r4Back.id)
        assertTrue(r4Back.agent.isNotEmpty(), "R4 Provenance agent must satisfy 1..* cardinality")
        assertEquals(
            "Practitioner/dr-smith",
            r4Back.agent
                .first()
                .who.reference
                ?.value,
        )
    }

    /**
     * Tests Bundle round-trip conversion with entry resources preserved.
     */
    @Test
    fun testBundleConversion() {
        val patient =
            Patient(
                id = "pat-1",
                name = listOf(HumanName(family = FhirString(value = "Doe"))),
            )
        val r4Bundle =
            Bundle(
                id = "b-ver-1",
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(
                            fullUrl = Uri(value = "urn:uuid:123"),
                            resource = patient,
                        ),
                    ),
            )
        val r5Result = FhirVersionConverter.convertBundleR4ToR5(r4Bundle)
        assertTrue(r5Result.isSuccess)
        val r5Bundle = r5Result.getOrThrow()
        assertEquals("b-ver-1", r5Bundle.id)
        assertEquals(
            "urn:uuid:123",
            r5Bundle.entry
                .firstOrNull()
                ?.fullUrl
                ?.value,
        )
        val r5EntryResource = r5Bundle.entry.firstOrNull()?.resource
        assertTrue(r5EntryResource is dev.ohs.fhir.model.r5.Patient, "Bundle entry resource must be converted to R5 Patient")
        assertEquals("pat-1", r5EntryResource.id)

        val backResult = FhirVersionConverter.convertBundleR5ToR4(r5Bundle)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Bundle.id, r4Back.id)
        val r4BackResource = r4Back.entry.firstOrNull()?.resource
        assertTrue(r4BackResource is Patient, "Bundle entry resource must be converted back to R4 Patient")
        assertEquals("pat-1", r4BackResource.id)
    }

    /**
     * Tests Encounter round-trip conversion with class and period preserved.
     */
    @Test
    fun testEncounterConversion() {
        val r4Enc =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-ver-1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress),
                `class` =
                    dev.ohs.fhir.model.r4.Coding(
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                        code = Code(value = "IMP"),
                        display = FhirString(value = "inpatient encounter"),
                    ),
                period =
                    dev.ohs.fhir.model.r4.Period(
                        start =
                            dev.ohs.fhir.model.r4.DateTime(
                                value =
                                    dev.ohs.fhir.model.r4.FhirDateTime
                                        .fromString("2026-09-17T09:00:00Z"),
                            ),
                        end =
                            dev.ohs.fhir.model.r4
                                .DateTime(
                                    value =
                                        dev.ohs.fhir.model.r4.FhirDateTime
                                            .fromString("2026-09-17T11:00:00Z"),
                                ),
                    ),
            )
        val r5Result = FhirVersionConverter.convertEncounterR4ToR5(r4Enc)
        assertTrue(r5Result.isSuccess)
        val r5Enc = r5Result.getOrThrow()
        assertEquals("enc-ver-1", r5Enc.id)
        assertEquals(
            "2026-09-17T09:00:00Z",
            r5Enc.actualPeriod
                ?.start
                ?.value
                .toString(),
        )
        assertEquals(
            "2026-09-17T11:00:00Z",
            r5Enc.actualPeriod
                ?.end
                ?.value
                .toString(),
        )
        assertEquals(
            "IMP",
            r5Enc.`class`
                .firstOrNull()
                ?.coding
                ?.firstOrNull()
                ?.code
                ?.value,
        )

        val backResult = FhirVersionConverter.convertEncounterR5ToR4(r5Enc)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Enc.id, r4Back.id)
        assertEquals("IMP", r4Back.`class`.code?.value)
        assertEquals(
            "2026-09-17T09:00:00Z",
            r4Back.period
                ?.start
                ?.value
                .toString(),
        )
        assertEquals(
            "2026-09-17T11:00:00Z",
            r4Back.period
                ?.end
                ?.value
                .toString(),
        )
    }

    /**
     * Tests Patient R4 <-> R4B conversion.
     */
    @Test
    fun testPatientR4ToR4BConversion() {
        val r4Patient =
            Patient(
                id = "pat-r4b-1",
                name = listOf(HumanName(family = FhirString(value = "R4BFamily"))),
            )
        val r4bResult = FhirVersionConverter.convertPatientR4ToR4B(r4Patient)
        assertTrue(r4bResult.isSuccess)
        val r4bPatient = r4bResult.getOrThrow()
        assertEquals("pat-r4b-1", r4bPatient.id)
        assertEquals(
            "R4BFamily",
            r4bPatient.name
                .firstOrNull()
                ?.family
                ?.value,
        )

        val backResult = FhirVersionConverter.convertPatientR4BToR4(r4bPatient)
        assertTrue(backResult.isSuccess)
        val r4Back = backResult.getOrThrow()
        assertEquals(r4Patient.id, r4Back.id)
        assertEquals(
            "R4BFamily",
            r4Back.name
                .firstOrNull()
                ?.family
                ?.value,
        )
    }

    /**
     * Tests polymorphic convertResourceR4ToR5 and convertResourceR5ToR4 across all supported types.
     */
    @Test
    fun testPolymorphicResourceConversions() {
        val r4Patient = Patient(id = "p-poly")
        val r5PatientRes = FhirVersionConverter.convertResourceR4ToR5(r4Patient)
        assertTrue(r5PatientRes.isSuccess)
        assertTrue(r5PatientRes.getOrThrow() is dev.ohs.fhir.model.r5.Patient)
        val r4PatientBack = FhirVersionConverter.convertResourceR5ToR4(r5PatientRes.getOrThrow())
        assertTrue(r4PatientBack.isSuccess)

        val r4Enc =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-poly",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Planned),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = Code(value = "AMB")),
            )
        val r5EncRes = FhirVersionConverter.convertResourceR4ToR5(r4Enc)
        assertTrue(r5EncRes.isSuccess)
        assertTrue(r5EncRes.getOrThrow() is dev.ohs.fhir.model.r5.Encounter)
        val r4EncBack = FhirVersionConverter.convertResourceR5ToR4(r5EncRes.getOrThrow())
        assertTrue(r4EncBack.isSuccess)

        val r4Obs =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-poly",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r4
                                        .Coding(code = Code(value = "8867-4")),
                                ),
                        ),
            )
        val r5ObsRes = FhirVersionConverter.convertResourceR4ToR5(r4Obs)
        assertTrue(r5ObsRes.isSuccess)
        assertTrue(r5ObsRes.getOrThrow() is dev.ohs.fhir.model.r5.Observation)
        val r4ObsBack = FhirVersionConverter.convertResourceR5ToR4(r5ObsRes.getOrThrow())
        assertTrue(r4ObsBack.isSuccess)

        val r4Q =
            Questionnaire(
                id = "q-poly",
                status = Enumeration(value = PublicationStatus.Active),
            )
        val r5QRes = FhirVersionConverter.convertResourceR4ToR5(r4Q)
        assertTrue(r5QRes.isSuccess)
        assertTrue(r5QRes.getOrThrow() is dev.ohs.fhir.model.r5.Questionnaire)
        val r4QBack = FhirVersionConverter.convertResourceR5ToR4(r5QRes.getOrThrow())
        assertTrue(r4QBack.isSuccess)

        val r4Qr =
            QuestionnaireResponse(
                id = "qr-poly",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )
        val r5QrRes = FhirVersionConverter.convertResourceR4ToR5(r4Qr)
        assertTrue(r5QrRes.isSuccess)
        assertTrue(r5QrRes.getOrThrow() is dev.ohs.fhir.model.r5.QuestionnaireResponse)
        val r4QrBack = FhirVersionConverter.convertResourceR5ToR4(r5QrRes.getOrThrow())
        assertTrue(r4QrBack.isSuccess)

        val r4Doc =
            DocumentReference(
                id = "doc-poly",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content = emptyList(),
            )
        val r5DocRes = FhirVersionConverter.convertResourceR4ToR5(r4Doc)
        assertTrue(r5DocRes.isSuccess)
        assertTrue(r5DocRes.getOrThrow() is dev.ohs.fhir.model.r5.DocumentReference)
        val r4DocBack = FhirVersionConverter.convertResourceR5ToR4(r5DocRes.getOrThrow())
        assertTrue(r4DocBack.isSuccess)

        val r4Prov =
            Provenance(
                id = "prov-poly",
                target = emptyList(),
                recorded =
                    Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString("2026-09-18T10:00:00Z"),
                    ),
                agent = listOf(Provenance.Agent(who = Reference(reference = FhirString(value = "Practitioner/1")))),
            )
        val r5ProvRes = FhirVersionConverter.convertResourceR4ToR5(r4Prov)
        assertTrue(r5ProvRes.isSuccess)
        assertTrue(r5ProvRes.getOrThrow() is dev.ohs.fhir.model.r5.Provenance)
        val r4ProvBack = FhirVersionConverter.convertResourceR5ToR4(r5ProvRes.getOrThrow())
        assertTrue(r4ProvBack.isSuccess)

        // Unsupported resource conversions
        val r4Bundle = Bundle(type = Enumeration(value = Bundle.BundleType.Collection))
        val unsuppR4Res = FhirVersionConverter.convertResourceR4ToR5(r4Bundle)
        assertTrue(unsuppR4Res.isFailure)

        val r5Bundle =
            dev.ohs.fhir.model.r5.Bundle(
                type =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Collection),
            )
        val unsuppR5Res = FhirVersionConverter.convertResourceR5ToR4(r5Bundle)
        assertTrue(unsuppR5Res.isFailure)
    }

    /**
     * Tests partial date variants (Year, YearMonth) and organization reference on Patient resources.
     */
    @Test
    fun testPatientDateVariantsAndOrg() {
        // R4 Patient with Year birthDate and managingOrganization
        val r4PatientYear =
            Patient(
                id = "pat-year",
                birthDate =
                    dev.ohs.fhir.model.r4
                        .Date(
                            value =
                                dev.ohs.fhir.model.r4.FhirDate
                                    .Year(1980),
                        ),
                managingOrganization = Reference(reference = FhirString(value = "Organization/org-1")),
            )
        val r5YearRes = FhirVersionConverter.convertPatientR4ToR5(r4PatientYear)
        assertTrue(r5YearRes.isSuccess)
        val r5Year = r5YearRes.getOrThrow()
        assertTrue(r5Year.birthDate?.value is dev.ohs.fhir.model.r5.FhirDate.Year)
        assertEquals("Organization/org-1", r5Year.managingOrganization?.reference?.value)

        val r4YearBack = FhirVersionConverter.convertPatientR5ToR4(r5Year).getOrThrow()
        assertTrue(r4YearBack.birthDate?.value is dev.ohs.fhir.model.r4.FhirDate.Year)
        assertEquals("Organization/org-1", r4YearBack.managingOrganization?.reference?.value)

        // R4 Patient with YearMonth birthDate
        val r4PatientYearMonth =
            Patient(
                id = "pat-ym",
                birthDate =
                    dev.ohs.fhir.model.r4
                        .Date(
                            value =
                                dev.ohs.fhir.model.r4.FhirDate
                                    .fromString("1992-05"),
                        ),
            )
        val r5YmRes = FhirVersionConverter.convertPatientR4ToR5(r4PatientYearMonth)
        assertTrue(r5YmRes.isSuccess)
        val r5Ym = r5YmRes.getOrThrow()
        assertTrue(r5Ym.birthDate?.value is dev.ohs.fhir.model.r5.FhirDate.YearMonth)

        val r4YmBack = FhirVersionConverter.convertPatientR5ToR4(r5Ym).getOrThrow()
        assertTrue(r4YmBack.birthDate?.value is dev.ohs.fhir.model.r4.FhirDate.YearMonth)

        // R4B Patient with gender and names
        val r4bYearRes = FhirVersionConverter.convertPatientR4ToR4B(r4PatientYear)
        assertTrue(r4bYearRes.isSuccess)
        val r4bYear = r4bYearRes.getOrThrow()
        assertEquals("pat-year", r4bYear.id)
        val r4FromR4bYear = FhirVersionConverter.convertPatientR4BToR4(r4bYear).getOrThrow()
        assertEquals("pat-year", r4FromR4bYear.id)

        val r4bYmRes = FhirVersionConverter.convertPatientR4ToR4B(r4PatientYearMonth)
        assertTrue(r4bYmRes.isSuccess)
        val r4FromR4bYm = FhirVersionConverter.convertPatientR4BToR4(r4bYmRes.getOrThrow()).getOrThrow()
        assertEquals("pat-ym", r4FromR4bYm.id)
    }

    /**
     * Tests Observation value mappings across String, Integer, Quantity, and nulls.
     */
    @Test
    fun testObservationValueVariants() {
        val codeableConcept =
            dev.ohs.fhir.model.r4.CodeableConcept(
                coding =
                    listOf(
                        dev.ohs.fhir.model.r4
                            .Coding(code = Code(value = "obs-c")),
                    ),
            )

        // Observation with String value
        val obsStr =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-str",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code = codeableConcept,
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .String(FhirString(value = "Positive")),
            )
        val r5Str = FhirVersionConverter.convertObservationR4ToR5(obsStr).getOrThrow()
        assertTrue(r5Str.value is dev.ohs.fhir.model.r5.Observation.Value.String)
        val r4StrBack = FhirVersionConverter.convertObservationR5ToR4(r5Str).getOrThrow()
        assertTrue(r4StrBack.value is dev.ohs.fhir.model.r4.Observation.Value.String)

        // Observation with Integer value
        val obsInt =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-int",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code = codeableConcept,
                value =
                    dev.ohs.fhir.model.r4.Observation.Value
                        .Integer(
                            dev.ohs.fhir.model.r4
                                .Integer(value = 42),
                        ),
            )
        val r5Int = FhirVersionConverter.convertObservationR4ToR5(obsInt).getOrThrow()
        assertTrue(r5Int.value is dev.ohs.fhir.model.r5.Observation.Value.Integer)
        val r4IntBack = FhirVersionConverter.convertObservationR5ToR4(r5Int).getOrThrow()
        assertTrue(r4IntBack.value is dev.ohs.fhir.model.r4.Observation.Value.Integer)

        // Observation with Quantity value
        val obsQty =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-qty",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code = codeableConcept,
                value =
                    dev.ohs.fhir.model.r4.Observation.Value.Quantity(
                        dev.ohs.fhir.model.r4.Quantity(
                            value =
                                dev.ohs.fhir.model.r4
                                    .Decimal(
                                        value =
                                            dev.ohs.fhir.model.r4.FhirDecimal
                                                .fromString("120.5"),
                                    ),
                        ),
                    ),
            )
        val r5Qty = FhirVersionConverter.convertObservationR4ToR5(obsQty).getOrThrow()
        assertTrue(r5Qty.value is dev.ohs.fhir.model.r5.Observation.Value.Quantity)
        val r4QtyBack = FhirVersionConverter.convertObservationR5ToR4(r5Qty).getOrThrow()
        assertTrue(r4QtyBack.value is dev.ohs.fhir.model.r4.Observation.Value.Quantity)

        // Observation with null value
        val obsNull =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-null",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Preliminary),
                code = codeableConcept,
            )
        val r5Null = FhirVersionConverter.convertObservationR4ToR5(obsNull).getOrThrow()
        kotlin.test.assertNull(r5Null.value)
        val r4NullBack = FhirVersionConverter.convertObservationR5ToR4(r5Null).getOrThrow()
        kotlin.test.assertNull(r4NullBack.value)
    }

    /**
     * Tests Questionnaire items with AnswerOption Integer, String, and Coding variants.
     */
    @Test
    fun testQuestionnaireAnswerOptionVariants() {
        val itemWithOptions =
            Questionnaire.Item(
                linkId = FhirString(value = "item-opts"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                answerOption =
                    listOf(
                        Questionnaire.Item.AnswerOption(
                            value =
                                Questionnaire.Item.AnswerOption.Value
                                    .String(FhirString(value = "OptA")),
                        ),
                        Questionnaire.Item.AnswerOption(
                            value =
                                Questionnaire.Item.AnswerOption.Value.Coding(
                                    dev.ohs.fhir.model.r4.Coding(
                                        system = Uri(value = "http://example.com"),
                                        code = Code(value = "code-1"),
                                        display = FhirString(value = "Display 1"),
                                    ),
                                ),
                        ),
                        Questionnaire.Item.AnswerOption(
                            value =
                                Questionnaire.Item.AnswerOption.Value
                                    .Integer(
                                        dev.ohs.fhir.model.r4
                                            .Integer(value = 10),
                                    ),
                        ),
                    ),
            )
        val q =
            Questionnaire(
                id = "q-opts",
                status = Enumeration(value = PublicationStatus.Active),
                item = listOf(itemWithOptions),
            )
        val r5Q = FhirVersionConverter.convertQuestionnaireR4ToR5(q).getOrThrow()
        val r5Opts = r5Q.item.first().answerOption
        assertEquals(2, r5Opts.size)
        assertTrue(r5Opts[0].value is dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.String)
        assertTrue(r5Opts[1].value is dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Coding)

        val r4Back = FhirVersionConverter.convertQuestionnaireR5ToR4(r5Q).getOrThrow()
        val r4Opts = r4Back.item.first().answerOption
        assertEquals(2, r4Opts.size)
        assertTrue(r4Opts[0].value is Questionnaire.Item.AnswerOption.Value.String)
        assertTrue(r4Opts[1].value is Questionnaire.Item.AnswerOption.Value.Coding)
    }

    /**
     * Tests Encounter period with start only, end only, or neither.
     */
    @Test
    fun testEncounterPeriodVariants() {
        // Encounter with start only
        val encStartOnly =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-start",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = Code(value = "IMP")),
                period =
                    dev.ohs.fhir.model.r4.Period(
                        start =
                            dev.ohs.fhir.model.r4
                                .DateTime(
                                    value =
                                        dev.ohs.fhir.model.r4.FhirDateTime
                                            .fromString("2026-09-18T10:00:00Z"),
                                ),
                    ),
            )
        val r5Start = FhirVersionConverter.convertEncounterR4ToR5(encStartOnly).getOrThrow()
        assertEquals(
            "2026-09-18T10:00:00Z",
            r5Start.actualPeriod
                ?.start
                ?.value
                .toString(),
        )
        kotlin.test.assertNull(r5Start.actualPeriod?.end)
        val r4StartBack = FhirVersionConverter.convertEncounterR5ToR4(r5Start).getOrThrow()
        assertEquals(
            "2026-09-18T10:00:00Z",
            r4StartBack.period
                ?.start
                ?.value
                .toString(),
        )
        kotlin.test.assertNull(r4StartBack.period?.end)

        // Encounter with end only
        val encEndOnly =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-end",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = Code(value = "IMP")),
                period =
                    dev.ohs.fhir.model.r4.Period(
                        end =
                            dev.ohs.fhir.model.r4
                                .DateTime(
                                    value =
                                        dev.ohs.fhir.model.r4.FhirDateTime
                                            .fromString("2026-09-18T12:00:00Z"),
                                ),
                    ),
            )
        val r5End = FhirVersionConverter.convertEncounterR4ToR5(encEndOnly).getOrThrow()
        kotlin.test.assertNull(r5End.actualPeriod?.start)
        assertEquals(
            "2026-09-18T12:00:00Z",
            r5End.actualPeriod
                ?.end
                ?.value
                .toString(),
        )
        val r4EndBack = FhirVersionConverter.convertEncounterR5ToR4(r5End).getOrThrow()
        kotlin.test.assertNull(r4EndBack.period?.start)
        assertEquals(
            "2026-09-18T12:00:00Z",
            r4EndBack.period
                ?.end
                ?.value
                .toString(),
        )

        // Encounter without period
        val encNoPeriod =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-none",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(code = Code(value = "IMP")),
            )
        val r5NoPeriod = FhirVersionConverter.convertEncounterR4ToR5(encNoPeriod).getOrThrow()
        kotlin.test.assertNull(r5NoPeriod.actualPeriod)
        val r4NoPeriodBack = FhirVersionConverter.convertEncounterR5ToR4(r5NoPeriod).getOrThrow()
        kotlin.test.assertNull(r4NoPeriodBack.period)
    }

    /**
     * Tests all default fallback branches when optional fields are null or empty across resources.
     */
    @Test
    fun testExhaustiveDefaultsAndEdgeCases() {
        // 1. Encounter with null status and empty class coding
        val r4EncNullStatus =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-null-status",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(),
            )
        val r5EncRes = FhirVersionConverter.convertEncounterR4ToR5(r4EncNullStatus)
        assertTrue(r5EncRes.isSuccess)
        val r5Enc = r5EncRes.getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress, r5Enc.status.value)

        // R5 Encounter with null status and empty class
        val r5EncNull =
            dev.ohs.fhir.model.r5.Encounter(
                id = "r5-enc-null",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
                `class` = emptyList(),
            )
        val r4EncBack = FhirVersionConverter.convertEncounterR5ToR4(r5EncNull).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress, r4EncBack.status.value)
        assertEquals("AMB", r4EncBack.`class`.code?.value)

        // R5 Encounter with class coding having null code
        val r5EncNullCode =
            dev.ohs.fhir.model.r5.Encounter(
                id = "r5-enc-null-code",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress),
                `class` =
                    listOf(
                        dev.ohs.fhir.model.r5.CodeableConcept(
                            coding =
                                listOf(
                                    dev.ohs.fhir.model.r5
                                        .Coding(
                                            system =
                                                dev.ohs.fhir.model.r5
                                                    .Uri(value = "http://test"),
                                        ),
                                ),
                        ),
                    ),
            )
        val r4EncNullCodeBack = FhirVersionConverter.convertEncounterR5ToR4(r5EncNullCode).getOrThrow()
        assertEquals("AMB", r4EncNullCodeBack.`class`.code?.value)

        // 2. Observation with null status, subject reference, and performers
        val r4ObsNullStatus =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-null-status",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
                subject =
                    dev.ohs.fhir.model.r4
                        .Reference(reference = FhirString(value = "Patient/subj-1")),
                performer =
                    listOf(
                        dev.ohs.fhir.model.r4
                            .Reference(reference = FhirString(value = "Practitioner/perf-1")),
                    ),
            )
        val r5Obs = FhirVersionConverter.convertObservationR4ToR5(r4ObsNullStatus).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final, r5Obs.status.value)
        assertEquals("Patient/subj-1", r5Obs.subject?.reference?.value)

        val r5ObsNullStatus =
            dev.ohs.fhir.model.r5.Observation(
                id = "r5-obs-null",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
                code =
                    dev.ohs.fhir.model.r5
                        .CodeableConcept(),
                subject =
                    dev.ohs.fhir.model.r5
                        .Reference(
                            reference =
                                dev.ohs.fhir.model.r5
                                    .String(value = "Patient/subj-r5"),
                        ),
            )
        val r4ObsBack = FhirVersionConverter.convertObservationR5ToR4(r5ObsNullStatus).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final, r4ObsBack.status.value)
        assertEquals("Patient/subj-r5", r4ObsBack.subject?.reference?.value)

        // 3. Questionnaire with null status, item with null type, required = true, repeats = true
        val r4QNullStatus =
            Questionnaire(
                id = "q-null-status",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "item-req"),
                            type =
                                dev.ohs.fhir.model.r4
                                    .Enumeration(value = null),
                            required =
                                dev.ohs.fhir.model.r4
                                    .Boolean(value = true),
                            repeats =
                                dev.ohs.fhir.model.r4
                                    .Boolean(value = true),
                        ),
                    ),
            )
        val r5Q = FhirVersionConverter.convertQuestionnaireR4ToR5(r4QNullStatus).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.terminologies.PublicationStatus.Active, r5Q.status.value)
        val r5Item = r5Q.item.first()
        assertEquals(dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.String, r5Item.type.value)
        assertEquals(true, r5Item.required?.value)
        assertEquals(true, r5Item.repeats?.value)

        val r5QNullStatus =
            dev.ohs.fhir.model.r5.Questionnaire(
                id = "r5-q-null",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
                item =
                    listOf(
                        dev.ohs.fhir.model.r5.Questionnaire.Item(
                            linkId =
                                dev.ohs.fhir.model.r5
                                    .String(value = "item-r5-req"),
                            type =
                                dev.ohs.fhir.model.r5
                                    .Enumeration(value = null),
                            required =
                                dev.ohs.fhir.model.r5
                                    .Boolean(value = true),
                            repeats =
                                dev.ohs.fhir.model.r5
                                    .Boolean(value = true),
                            answerOption =
                                listOf(
                                    dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                                        value =
                                            dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Integer(
                                                dev.ohs.fhir.model.r5
                                                    .Integer(value = 10),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val r4QBack = FhirVersionConverter.convertQuestionnaireR5ToR4(r5QNullStatus).getOrThrow()
        assertEquals(PublicationStatus.Active, r4QBack.status.value)
        val r4Item = r4QBack.item.first()
        assertEquals(Questionnaire.QuestionnaireItemType.String, r4Item.type.value)
        assertEquals(true, r4Item.required?.value)
        assertEquals(true, r4Item.repeats?.value)
        assertTrue(r4Item.answerOption.isEmpty()) // Integer option returns null

        // 4. QuestionnaireResponse with null status, null questionnaire canonical
        val r4QrNull =
            QuestionnaireResponse(
                id = "qr-null",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
            )
        val r5Qr = FhirVersionConverter.convertQuestionnaireResponseR4ToR5(r4QrNull).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.QuestionnaireResponse.QuestionnaireResponseStatus.Completed, r5Qr.status.value)
        assertEquals("", r5Qr.questionnaire.value)

        val r5QrNull =
            dev.ohs.fhir.model.r5.QuestionnaireResponse(
                id = "r5-qr-null",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
                questionnaire =
                    dev.ohs.fhir.model.r5
                        .Canonical(value = null),
            )
        val r4QrBack = FhirVersionConverter.convertQuestionnaireResponseR5ToR4(r5QrNull).getOrThrow()
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.Completed, r4QrBack.status.value)
        assertEquals("", r4QrBack.questionnaire?.value)

        // 5. DocumentReference with null status
        val r4DocNull =
            DocumentReference(
                id = "doc-null",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                content = emptyList(),
            )
        val r5Doc = FhirVersionConverter.convertDocumentReferenceR4ToR5(r4DocNull).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Current, r5Doc.status.value)

        val r5DocNull =
            dev.ohs.fhir.model.r5.DocumentReference(
                id = "r5-doc-null",
                status =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
                content = emptyList(),
            )
        val r4DocBack = FhirVersionConverter.convertDocumentReferenceR5ToR4(r5DocNull).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current, r4DocBack.status.value)

        // 6. Provenance with empty agent, agent with null who reference, r5 null recorded
        val r4ProvEmptyAgent =
            Provenance(
                id = "prov-empty-agent",
                target = emptyList(),
                recorded =
                    Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString("2026-09-18T12:00:00Z"),
                    ),
                agent = emptyList(),
            )
        val r5ProvEmpty = FhirVersionConverter.convertProvenanceR4ToR5(r4ProvEmptyAgent).getOrThrow()
        assertEquals(
            "Practitioner/chartcam-device",
            r5ProvEmpty.agent
                .first()
                .who.reference
                ?.value,
        )

        val r4ProvNullWho =
            Provenance(
                id = "prov-null-who",
                target = emptyList(),
                recorded =
                    Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString("2026-09-18T12:00:00Z"),
                    ),
                agent = listOf(Provenance.Agent(who = Reference())),
            )
        val r5ProvFromR4NullWho = FhirVersionConverter.convertProvenanceR4ToR5(r4ProvNullWho).getOrThrow()
        assertEquals(
            "Practitioner/unknown",
            r5ProvFromR4NullWho.agent
                .first()
                .who.reference
                ?.value,
        )

        val r5ProvNullRecorded =
            dev.ohs.fhir.model.r5.Provenance(
                id = "r5-prov-null-rec",
                target = emptyList(),
                agent = emptyList(),
            )
        val r4ProvBack = FhirVersionConverter.convertProvenanceR5ToR4(r5ProvNullRecorded).getOrThrow()
        assertEquals(
            "Practitioner/chartcam-device",
            r4ProvBack.agent
                .first()
                .who.reference
                ?.value,
        )
        assertEquals("2026-09-17T00:00:00Z", r4ProvBack.recorded.value.toString())

        val r5ProvNullWho =
            dev.ohs.fhir.model.r5.Provenance(
                id = "r5-prov-null-who",
                target = emptyList(),
                agent =
                    listOf(
                        dev.ohs.fhir.model.r5.Provenance
                            .Agent(
                                who =
                                    dev.ohs.fhir.model.r5
                                        .Reference(),
                            ),
                    ),
            )
        val r4ProvWhoBack = FhirVersionConverter.convertProvenanceR5ToR4(r5ProvNullWho).getOrThrow()
        assertEquals(
            "Practitioner/unknown",
            r4ProvWhoBack.agent
                .first()
                .who.reference
                ?.value,
        )

        // 7. Bundle with null type
        val r4BundleNull =
            Bundle(
                id = "b-null",
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
            )
        val r5Bundle = FhirVersionConverter.convertBundleR4ToR5(r4BundleNull).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r5.Bundle.BundleType.Collection, r5Bundle.type.value)

        val r5BundleNull =
            dev.ohs.fhir.model.r5.Bundle(
                id = "r5-b-null",
                type =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = null),
            )
        val r4BundleBack = FhirVersionConverter.convertBundleR5ToR4(r5BundleNull).getOrThrow()
        assertEquals(Bundle.BundleType.Collection, r4BundleBack.type.value)

        // 8. Patient R4 <-> R4B with gender and given names
        val r4PatGender =
            Patient(
                id = "pat-gender",
                gender =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Female),
                name = listOf(HumanName(family = FhirString(value = "Smith"), given = listOf(FhirString(value = "Jane")))),
            )
        val r4bPat = FhirVersionConverter.convertPatientR4ToR4B(r4PatGender).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4b.terminologies.AdministrativeGender.Female, r4bPat.gender?.value)
        assertEquals(
            "Jane",
            r4bPat.name
                .first()
                .given
                .first()
                .value,
        )

        val r4PatBack = FhirVersionConverter.convertPatientR4BToR4(r4bPat).getOrThrow()
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Female, r4PatBack.gender?.value)
        assertEquals(
            "Jane",
            r4PatBack.name
                .first()
                .given
                .first()
                .value,
        )
    }
}
