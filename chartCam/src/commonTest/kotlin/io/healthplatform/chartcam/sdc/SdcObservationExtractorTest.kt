/**
 * @file SdcObservationExtractorTest.kt
 * Unit tests for SdcObservationExtractor verifying structured FHIR Observation extraction.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Unit tests for [SdcObservationExtractor].
 */
class SdcObservationExtractorTest {
    /**
     * Verifies that pain score and vitals are extracted into structured FHIR Observations.
     */
    @Test
    fun testExtractPainAndVitalsObservations() {
        val questionnaire =
            Questionnaire(
                id = "q-triage",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "pain_score"),
                            text = FhirString(value = "Current Pain Level (0-10)"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                        ),
                        Questionnaire.Item(
                            linkId = FhirString(value = "fitzpatrick_skin"),
                            text = FhirString(value = "Fitzpatrick Skin Phototype"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                        ),
                    ),
            )

        val response =
            QuestionnaireResponse(
                id = "qr-triage-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "pain_score"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Decimal(
                                                Decimal(value = FhirDecimal.fromString("7.5")),
                                            ),
                                    ),
                                ),
                        ),
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "fitzpatrick_skin"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.String(
                                                FhirString(value = "Type II"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-123", "enc-456")
        assertTrue(result.isSuccess)
        val observations = result.getOrNull()
        assertNotNull(observations)
        assertEquals(2, observations.size)

        val painObs = observations.first { it.code.coding.any { c -> c.code?.value == SdcObservationExtractor.LOINC_PAIN_SCORE } }
        assertEquals("Patient/pat-123", painObs.subject?.reference?.value)
        assertEquals("Encounter/enc-456", painObs.encounter?.reference?.value)
        assertEquals(Observation.ObservationStatus.Final, painObs.status.value)
        val painVal = painObs.value as? Observation.Value.Quantity
        assertNotNull(painVal)
        assertEquals(
            "7.5",
            painVal.value.value
                ?.value
                ?.toString(),
        )

        val skinObs = observations.first { it.code.coding.any { c -> c.code?.value == SdcObservationExtractor.LOINC_FITZPATRICK } }
        val skinVal = skinObs.value as? Observation.Value.String
        assertNotNull(skinVal)
        assertEquals("Type II", skinVal.value.value)
    }

    /**
     * Verifies that empty questionnaire responses safely produce an empty observation list.
     */
    @Test
    fun testExtractEmptyResponse() {
        val questionnaire =
            Questionnaire(
                id = "q-empty",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
            )
        val response =
            QuestionnaireResponse(
                id = "qr-empty",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )

        val result = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-empty")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    /**
     * Verifies that question definitions (item.definition) are extracted into Observation codes.
     */
    @Test
    fun testExtractObservationFromDefinition() {
        val questionnaire =
            Questionnaire(
                id = "q-def-1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId =
                                dev.ohs.fhir.model.r4
                                    .String(value = "custom-hr"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                            text =
                                dev.ohs.fhir.model.r4
                                    .String(value = "Heart Rate"),
                            definition =
                                dev.ohs.fhir.model.r4
                                    .Canonical(value = "http://loinc.org/rdf#8867-4"),
                        ),
                    ),
            )
        val response =
            QuestionnaireResponse(
                id = "qr-def-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId =
                                dev.ohs.fhir.model.r4
                                    .String(value = "custom-hr"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Decimal(
                                                dev.ohs.fhir.model.r4.Decimal(
                                                    value =
                                                        dev.ohs.fhir.model.r4.FhirDecimal
                                                            .fromString("72"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-def")
        assertTrue(result.isSuccess)
        val obsList = result.getOrThrow()
        assertEquals(1, obsList.size)
        val obs = obsList.first()
        assertEquals(
            "8867-4",
            obs.code.coding
                .firstOrNull()
                ?.code
                ?.value,
        )
        assertEquals(
            "http://loinc.org",
            obs.code.coding
                .firstOrNull()
                ?.system
                ?.value,
        )
    }

    /**
     * Verifies that the sdc-questionnaire-observationExtract extension triggers Observation generation.
     */
    @Test
    fun testExtractObservationViaObservationExtractExtension() {
        val questionnaire =
            Questionnaire(
                id = "q-extract-ext",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "generic_vital"),
                            text = FhirString(value = "Systolic Blood Pressure"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                            extension =
                                listOf(
                                    dev.ohs.fhir.model.r4.Extension(
                                        url = io.healthplatform.chartcam.fhir.SdcExtensions.OBSERVATION_EXTRACT,
                                        value =
                                            dev.ohs.fhir.model.r4.Extension.Value.Boolean(
                                                dev.ohs.fhir.model.r4
                                                    .Boolean(value = true),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val response =
            QuestionnaireResponse(
                id = "qr-extract-ext-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "generic_vital"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Decimal(
                                                Decimal(value = FhirDecimal.fromString("120")),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-ext")
        assertTrue(result.isSuccess)
        val obsList = result.getOrThrow()
        assertEquals(1, obsList.size)
        assertEquals(
            "Systolic Blood Pressure",
            obsList
                .first()
                .code.text
                ?.value,
        )
    }

    /**
     * Verifies that choice answers with Value.Coding extract into Observation.Value.CodeableConcept.
     */
    @Test
    fun testExtractChoiceCodingObservation() {
        val questionnaire =
            Questionnaire(
                id = "q-choice-obs",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "fitzpatrick_skin"),
                            text = FhirString(value = "Fitzpatrick Skin Phototype"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                        ),
                    ),
            )
        val response =
            QuestionnaireResponse(
                id = "qr-coding-1",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "fitzpatrick_skin"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Coding(
                                                dev.ohs.fhir.model.r4.Coding(
                                                    system =
                                                        dev.ohs.fhir.model.r4
                                                            .Uri(value = "http://loinc.org"),
                                                    code =
                                                        dev.ohs.fhir.model.r4
                                                            .Code(value = "LA28312-3"),
                                                    display = FhirString(value = "Type I"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val observations = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-1", "enc-1").getOrThrow()
        assertEquals(1, observations.size)
        val obs = observations.first()
        assertTrue(obs.value is Observation.Value.CodeableConcept)
        val cc = (obs.value as Observation.Value.CodeableConcept).value
        assertEquals(
            "LA28312-3",
            cc.coding
                .first()
                .code
                ?.value,
        )
    }

    /**
     * Verifies Observation value mappings (Integer, Boolean, Date, DateTime, Quantity, unsupported) and code resolution.
     */
    @Test
    fun testExhaustiveObservationExtractionBranches() {
        val qItemWithCode =
            Questionnaire.Item(
                linkId = FhirString(value = "item_code"),
                text = FhirString(value = "Item with Code"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                code =
                    listOf(
                        dev.ohs.fhir.model.r4.Coding(
                            system =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "http://example.org"),
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code(value = "EX1"),
                        ),
                    ),
            )

        val qItemWithLoincDef =
            Questionnaire.Item(
                linkId = FhirString(value = "item_loinc_def"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                definition =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org/rdf#1234-5"),
            )

        val qItemWithSnomedDef =
            Questionnaire.Item(
                linkId = FhirString(value = "item_snomed_def"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                definition =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://snomed.info/sct/999"),
            )

        // Item with observationExtract extension and null text -> hits line 261 fallback
        val qItemExtractExtNoText =
            Questionnaire.Item(
                linkId = FhirString(value = "item_fallback_text"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                extension =
                    listOf(
                        dev.ohs.fhir.model.r4.Extension(
                            url = io.healthplatform.chartcam.fhir.SdcExtensions.OBSERVATION_EXTRACT,
                            value =
                                dev.ohs.fhir.model.r4.Extension.Value
                                    .Boolean(
                                        dev.ohs.fhir.model.r4
                                            .Boolean(value = true),
                                    ),
                        ),
                    ),
            )

        val qItemDateTime =
            Questionnaire.Item(
                linkId = FhirString(value = "item_datetime"),
                text = FhirString(value = "DateTime Vital"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                definition =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org/111"),
            )

        val qItemQuantity =
            Questionnaire.Item(
                linkId = FhirString(value = "item_qty"),
                text = FhirString(value = "Quantity Vital"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Quantity),
                definition =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org/222"),
            )

        val qItemUnsupportedAnswer =
            Questionnaire.Item(
                linkId = FhirString(value = "item_unsupported"),
                text = FhirString(value = "Unsupported Vital"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                definition =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org/333"),
            )

        val questionnaire =
            Questionnaire(
                id = "q-exhaustive-obs",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        qItemWithCode,
                        qItemWithLoincDef,
                        qItemWithSnomedDef,
                        qItemExtractExtNoText,
                        qItemDateTime,
                        qItemQuantity,
                        qItemUnsupportedAnswer,
                    ),
            )

        val response =
            QuestionnaireResponse(
                id = "qr-exhaustive-obs",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        // Integer answer
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_code"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .Integer(
                                                    dev.ohs.fhir.model.r4
                                                        .Integer(value = 42),
                                                ),
                                    ),
                                ),
                        ),
                        // Boolean answer with definition code
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_loinc_def"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Boolean(
                                                dev.ohs.fhir.model.r4
                                                    .Boolean(value = true),
                                            ),
                                    ),
                                ),
                        ),
                        // Date answer with snomed definition
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_snomed_def"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Date(
                                                dev.ohs.fhir.model.r4
                                                    .Date(
                                                        value =
                                                            dev.ohs.fhir.model.r4.FhirDate
                                                                .fromString("2026-09-18"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                        // Boolean answer with observationExtract extension and fallback text
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_fallback_text"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Boolean(
                                                dev.ohs.fhir.model.r4
                                                    .Boolean(value = true),
                                            ),
                                    ),
                                ),
                        ),
                        // DateTime answer
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_datetime"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.DateTime(
                                                dev.ohs.fhir.model.r4.DateTime(
                                                    value =
                                                        dev.ohs.fhir.model.r4.FhirDateTime
                                                            .fromString("2026-09-18T10:00:00Z"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        // Quantity answer
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_qty"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Quantity(
                                                dev.ohs.fhir.model.r4.Quantity(
                                                    value = Decimal(value = FhirDecimal.fromString("75.5")),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        // Unsupported answer (Attachment) -> mapAnswerToObservationValue returns null, skipped
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "item_unsupported"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Attachment(
                                                dev.ohs.fhir.model.r4
                                                    .Attachment(),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val obsResult = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-obs", null)
        assertTrue(obsResult.isSuccess)
        val obsList = obsResult.getOrThrow()
        assertEquals(6, obsList.size)

        // Integer observation
        val intObs = obsList.first { it.code.coding.any { c -> c.code?.value == "EX1" } }
        assertTrue(intObs.value is Observation.Value.Integer)
        assertEquals(42, (intObs.value as Observation.Value.Integer).value.value)

        // Boolean observation with definition
        val boolObs = obsList.first { it.code.coding.any { c -> c.code?.value == "1234-5" } }
        assertTrue(boolObs.value is Observation.Value.Boolean)
        assertEquals(true, (boolObs.value as Observation.Value.Boolean).value.value)

        // Boolean observation with fallback linkId text
        val fallbackObs = obsList.first { it.code.text?.value == "item_fallback_text" }
        assertTrue(fallbackObs.value is Observation.Value.Boolean)

        // Date observation
        val dateObs = obsList.first { it.code.coding.any { c -> c.system?.value == "http://snomed.info/sct" } }
        assertTrue(dateObs.value is Observation.Value.DateTime)

        // DateTime observation
        val dtObs = obsList.first { it.code.text?.value == "DateTime Vital" }
        assertTrue(dtObs.value is Observation.Value.DateTime)

        // Quantity observation
        val qtyObs = obsList.first { it.code.text?.value == "Quantity Vital" }
        assertTrue(qtyObs.value is Observation.Value.Quantity)
        assertEquals(
            "75.5",
            (qtyObs.value as Observation.Value.Quantity)
                .value.value
                ?.value
                ?.toString(),
        )
    }

    /**
     * Verifies additional branches for skin fallback, unmapped questions, disabled extraction, and linkId fallbacks.
     */
    @Test
    fun testAdditionalObservationExtractionBranches() {
        val questionnaire =
            Questionnaire(
                id = "q-extra",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item =
                    listOf(
                        // Standard metric with "skin" (not fitzpatrick)
                        Questionnaire.Item(
                            linkId = FhirString(value = "skin_score"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // Question with no extract conditions (shouldExtractObservation == false)
                        Questionnaire.Item(
                            linkId = FhirString(value = "random_no_extract"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // Question with observationExtract extension = false
                        Questionnaire.Item(
                            linkId = FhirString(value = "extract_disabled"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            extension =
                                listOf(
                                    dev.ohs.fhir.model.r4.Extension(
                                        url = io.healthplatform.chartcam.fhir.SdcExtensions.OBSERVATION_EXTRACT,
                                        value =
                                            dev.ohs.fhir.model.r4.Extension.Value
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = false),
                                                ),
                                    ),
                                ),
                        ),
                        // Question with observationExtract extension having non-boolean value
                        Questionnaire.Item(
                            linkId = FhirString(value = "extract_non_bool"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            extension =
                                listOf(
                                    dev.ohs.fhir.model.r4.Extension(
                                        url = io.healthplatform.chartcam.fhir.SdcExtensions.OBSERVATION_EXTRACT,
                                        value =
                                            dev.ohs.fhir.model.r4.Extension.Value
                                                .String(FhirString(value = "true")),
                                    ),
                                ),
                        ),
                        // Question with unrelated extension url
                        Questionnaire.Item(
                            linkId = FhirString(value = "unrelated_ext"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            extension =
                                listOf(
                                    dev.ohs.fhir.model.r4.Extension(
                                        url = "http://example.org/unrelated",
                                        value =
                                            dev.ohs.fhir.model.r4.Extension.Value
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = true),
                                                ),
                                    ),
                                ),
                        ),
                        // Question with null value but id set
                        Questionnaire.Item(
                            linkId = FhirString(id = "q_fallback_id", value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            definition =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "http://loinc.org/999"),
                        ),
                        // Question with null value and null id
                        Questionnaire.Item(
                            linkId = FhirString(id = null, value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // Question with blank value
                        Questionnaire.Item(
                            linkId = FhirString(value = "   "),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )

        val response =
            QuestionnaireResponse(
                id = "qr-extra",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        // Skin metric -> extracts LOINC_FITZPATRICK
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "skin_score"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Type III")),
                                    ),
                                ),
                        ),
                        // Random question -> shouldExtractObservation is false, skipped
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "random_no_extract"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Ignored")),
                                    ),
                                ),
                        ),
                        // Extract disabled -> shouldExtractObservation is false, skipped
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "extract_disabled"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Ignored")),
                                    ),
                                ),
                        ),
                        // Unrelated extension -> shouldExtractObservation is false, skipped
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "unrelated_ext"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Ignored")),
                                    ),
                                ),
                        ),
                        // Extract non-bool -> extracts
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "extract_non_bool"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Extracted")),
                                    ),
                                ),
                        ),
                        // Unmapped question (qItem == null) -> extracted using fallback linkId text
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "unmapped_question"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "FallbackValue")),
                                    ),
                                ),
                        ),
                        // Response item with null value but id set
                        QuestionnaireResponse.Item(
                            linkId = FhirString(id = "q_fallback_id", value = null),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "ValFallback")),
                                    ),
                                ),
                        ),
                        // Response item with null value and null id -> linkId defaults to ""
                        QuestionnaireResponse.Item(
                            linkId = FhirString(id = null, value = null),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "EmptyLinkIdVal")),
                                    ),
                                ),
                        ),
                        // Date answer with null Date value
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "unmapped_date"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Date(
                                                dev.ohs.fhir.model.r4
                                                    .Date(value = null),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = SdcObservationExtractor.extractObservations(questionnaire, response, "pat-extra").getOrThrow()
        assertTrue(result.isNotEmpty())
        val skinObs = result.first { it.code.coding.any { c -> c.code?.value == SdcObservationExtractor.LOINC_FITZPATRICK } }
        assertNotNull(skinObs)
    }
}
