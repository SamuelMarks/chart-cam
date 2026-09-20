/**
 * @file QuestionnaireResponseGeneratorTest.kt
 * Contains declarations for QuestionnaireResponseGeneratorTest.kt.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests for [QuestionnaireResponseGenerator].
 */
class QuestionnaireResponseGeneratorTest {
    /**
     * Validates generation of a basic response.
     */
    @Test
    fun testBasicResponseGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q1"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "name" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                text = FhirString.Builder().apply { value = "Patient Name" }
                            },
                    )
                }.build()

        val answers = mapOf("name" to "Alice Smith")
        val result = QuestionnaireResponseGenerator.generateResult(q, answers)
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        assertEquals(1, qr.item.size)
        assertEquals("name", qr.item[0].linkId.value)
    }

    /**
     * Validates generation of repeated question groups with hierarchical answers.
     */
    @Test
    fun testRepeatingGroupResponseGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_repeating"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "lesion_group" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            ).apply {
                                repeats = FhirBoolean.Builder().apply { value = true }
                                text = FhirString.Builder().apply { value = "Lesions" }
                                item.add(
                                    Questionnaire.Item.Builder(
                                        linkId = FhirString.Builder().apply { value = "location" },
                                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                                    ),
                                )
                            },
                    )
                }.build()

        val answers =
            mapOf(
                "lesion_group#0.location" to "Arm",
                "lesion_group#1.location" to "Leg",
            )

        val result = QuestionnaireResponseGenerator.generateResult(q, answers)
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        assertEquals(2, qr.item.size)
        assertEquals("lesion_group", qr.item[0].linkId.value)
        assertEquals("lesion_group", qr.item[1].linkId.value)
    }

    /**
     * Validates safe handling of dates and datetimes with blank, partial, and malformed inputs.
     */
    @Test
    fun testDateAndDateTimeSafeParsing() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_dates"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "dob" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "recorded" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                            ),
                    )
                }.build()

        // Test with empty/blank strings - should not crash
        val blankAnswers = mapOf("dob" to "   ", "recorded" to "")
        val blankRes = QuestionnaireResponseGenerator.generateResult(q, blankAnswers)
        assertTrue(blankRes.isSuccess)
        val blankQr = blankRes.getOrNull()
        assertNotNull(blankQr)
        assertTrue(blankQr.item.all { it.answer.isEmpty() })

        // Test with malformed string - should not crash
        val malformedAnswers = mapOf("dob" to "not-a-date", "recorded" to "invalid-time")
        val malformedRes = QuestionnaireResponseGenerator.generateResult(q, malformedAnswers)
        assertTrue(malformedRes.isSuccess)
        val malformedQr = malformedRes.getOrNull()
        assertNotNull(malformedQr)
        assertTrue(malformedQr.item.all { it.answer.isEmpty() })

        // Test with valid partial date and valid ISO datetime
        val validAnswers = mapOf("dob" to "2026-09", "recorded" to "2026-09-17T14:30:00Z")
        val validRes = QuestionnaireResponseGenerator.generateResult(q, validAnswers)
        assertTrue(validRes.isSuccess)
        val validQr = validRes.getOrNull()
        assertNotNull(validQr)
        assertEquals(2, validQr.item.size)
        val dateAnswer =
            validQr.item
                .first { it.linkId.value == "dob" }
                .answer
                .firstOrNull()
        assertNotNull(dateAnswer)
        val dateTimeAnswer =
            validQr.item
                .first { it.linkId.value == "recorded" }
                .answer
                .firstOrNull()
        assertNotNull(dateTimeAnswer)
    }

    /**
     * Validates that null questionnaire ID and null item text produce null rather than empty string primitives.
     */
    @Test
    fun testNullQuestionnaireIdAndNullItemText() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = null
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "unlabeled_q" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                text = null
                            },
                    )
                }.build()

        val answers = mapOf("unlabeled_q" to "value1")
        val result = QuestionnaireResponseGenerator.generateResult(q, answers)
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        kotlin.test.assertNull(qr.questionnaire)
        assertEquals(1, qr.item.size)
        kotlin.test.assertNull(qr.item[0].text)
    }

    /**
     * Validates that choice questions with declared answer options generate Value.Coding answers.
     */
    @Test
    fun testChoiceQuestionValueCodingGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_choice"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "skin_type" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            ).apply {
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.Coding(
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
                                )
                            },
                    )
                }.build()

        val answers = mapOf("skin_type" to "Type I")
        val qr = QuestionnaireResponseGenerator.generateResult(q, answers).getOrThrow()
        val answerValue =
            qr.item
                .first()
                .answer
                .first()
                .value
        assertTrue(answerValue is dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Coding)
        val coding = (answerValue as dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Coding).value
        assertEquals("LA28312-3", coding.code?.value)
        assertEquals("http://loinc.org", coding.system?.value)
    }

    /**
     * Validates that attachment questions correctly produce Value.Attachment answers.
     */
    @Test
    fun testAttachmentAnswerGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_att"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "photo" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                            ),
                    )
                }.build()

        val answers = mapOf("photo" to "file:///photos/lesion.jpg")
        val qr = QuestionnaireResponseGenerator.generateResult(q, answers).getOrThrow()
        val answerValue =
            qr.item
                .first()
                .answer
                .first()
                .value
        assertTrue(answerValue is dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment)
        val att = (answerValue as dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Attachment).value
        assertEquals("file:///photos/lesion.jpg", att.url?.value)
    }

    /**
     * Validates choice, non-choice primitives, numbers, attachments, and repeating groups.
     */
    @Test
    fun testExhaustiveQuestionnaireResponseGeneratorBranches() {
        fun fhirStr(s: String) = FhirString.Builder().apply { value = s }
        val coding1 =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = "C1"),
                display = FhirString(value = "Option One"),
            )
        val coding2 =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = "C2"),
                display = FhirString(value = "Option Two"),
            )
        val codingNoDisplay =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = "C3"),
            )
        val codingNoCode =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                display = FhirString(value = "Option Four"),
            )

        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_exhaustive"
                    // Group with repeats = true and malformed hash key
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("repeat_group"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            ).apply {
                                repeats = FhirBoolean.Builder().apply { value = true }
                                item.add(
                                    Questionnaire.Item
                                        .Builder(
                                            linkId = fhirStr("inner_q"),
                                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                                        ),
                                )
                                item.add(
                                    Questionnaire.Item
                                        .Builder(
                                            linkId = fhirStr("repeat_group#0.prefixed_q"),
                                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                                        ),
                                )
                            },
                    )
                    // Group with repeats = false, no answers, but child has answer
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("static_group"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            ).apply {
                                item.add(
                                    Questionnaire.Item
                                        .Builder(
                                            linkId = fhirStr("child_text"),
                                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Text),
                                        ),
                                )
                            },
                    )
                    // Choice with options
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("choice_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            ).apply {
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Coding(coding1),
                                    ),
                                )
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Coding(coding2),
                                    ),
                                )
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Coding(codingNoDisplay),
                                    ),
                                )
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Coding(codingNoCode),
                                    ),
                                )
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Integer(
                                                    dev.ohs.fhir.model.r4
                                                        .Integer(value = 5),
                                                ),
                                    ),
                                )
                                answerOption.add(
                                    Questionnaire.Item.AnswerOption.Builder(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.Coding(
                                                dev.ohs.fhir.model.r4.Coding(
                                                    system =
                                                        dev.ohs.fhir.model.r4
                                                            .Uri(value = "http://loinc.org"),
                                                    code =
                                                        dev.ohs.fhir.model.r4
                                                            .Code(value = null),
                                                    display = FhirString(value = null),
                                                ),
                                            ),
                                    ),
                                )
                            },
                    )
                    // Primitive questions
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("bool_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("dec_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("int_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("att_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("body_map_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("non_string_text_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("temporal_date_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                            ),
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("temporal_datetime_q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                            ),
                    )
                    // Question with null linkId value
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = null },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Display),
                            ),
                    )
                    // Question with null type value
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = fhirStr("null_type_q"),
                                type = Enumeration(value = null),
                            ),
                    )
                }.build()

        val answers: Map<kotlin.String, Any> =
            mapOf(
                "repeat_group#invalid.inner_q" to "ignored",
                "repeat_group#0.inner_q" to "valid_repeat",
                "repeat_group#0.prefixed_q" to "prefixed_val",
                "child_text" to "Nested text content",
                "choice_q" to "Option One",
                "bool_q" to true,
                "dec_q" to
                    dev.ohs.fhir.model.r4.FhirDecimal
                        .fromString("12.34"),
                "int_q" to 99,
                "att_q" to
                    dev.ohs.fhir.model.r4
                        .Attachment(
                            url =
                                dev.ohs.fhir.model.r4
                                    .Url(value = "http://file.test"),
                        ),
                "body_map_q" to
                    io.healthplatform.chartcam.models.BodyMapLocation(
                        regionId = "left_arm",
                        displayName = "Left Arm",
                        xPercent = 25.0f,
                        yPercent = 50.0f,
                    ),
                "non_string_text_q" to 9999, // Hits line 318 (else -> answerValue.toString())
                "null_type_q" to "untyped_val",
                "temporal_date_q" to "2024-01-15",
                "temporal_datetime_q" to "2024-01-15T12:00:00Z",
            )

        val qr = QuestionnaireResponseGenerator.generateResult(q, answers).getOrThrow()
        assertEquals(12, qr.item.size)

        // Non-string date and datetime
        val nonStrDateQr =
            QuestionnaireResponseGenerator
                .generateResult(
                    q,
                    mapOf<kotlin.String, Any>("temporal_date_q" to 12345, "temporal_datetime_q" to true),
                ).getOrThrow()
        assertTrue(nonStrDateQr.item.all { it.answer.isEmpty() })

        // Matching code without display
        val codeMatchQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("choice_q" to "C3")).getOrThrow()
        assertEquals(1, codeMatchQr.item.size)

        // Matching display without code
        val displayMatchQr =
            QuestionnaireResponseGenerator
                .generateResult(
                    q,
                    mapOf<kotlin.String, Any>("choice_q" to "Option Four"),
                ).getOrThrow()
        assertEquals(1, displayMatchQr.item.size)

        // Scoped key prefix when linkId starts with prefix
        val scopedKeyAnswers = mapOf<kotlin.String, Any>("repeat_group#0.repeat_group#0" to "already_prefixed")
        val scopedQr = QuestionnaireResponseGenerator.generateResult(q, scopedKeyAnswers).getOrThrow()
        assertNotNull(scopedQr)

        // Choice answers with Fitzpatrick, Coding, List of Codings, List of Strings, and unsupported choice
        val fitzType = io.healthplatform.chartcam.models.FitzpatrickScaleDefaults.ALL_TYPES[2]
        val fitzAnswers = mapOf<kotlin.String, Any>("choice_q" to fitzType)
        val fitzQr = QuestionnaireResponseGenerator.generateResult(q, fitzAnswers).getOrThrow()
        assertTrue(
            fitzQr.item
                .first()
                .answer
                .first()
                .value is dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String,
        )

        val directCodingAnswers = mapOf<kotlin.String, Any>("choice_q" to coding2)
        val directCodingQr = QuestionnaireResponseGenerator.generateResult(q, directCodingAnswers).getOrThrow()
        assertTrue(
            directCodingQr.item
                .first()
                .answer
                .first()
                .value is dev.ohs.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Coding,
        )

        val unhandledChoiceAnswers = mapOf<kotlin.String, Any>("choice_q" to 999)
        val unhandledChoiceQr = QuestionnaireResponseGenerator.generateResult(q, unhandledChoiceAnswers).getOrThrow()
        assertTrue(
            unhandledChoiceQr.item
                .first()
                .answer
                .isEmpty(),
        )

        val listChoiceAnswers: Map<kotlin.String, Any> =
            mapOf(
                "choice_q" to listOf(coding1, "Option Two", "Unknown Option", 123),
            )
        val listChoiceQr = QuestionnaireResponseGenerator.generateResult(q, listChoiceAnswers).getOrThrow()
        assertEquals(
            3,
            listChoiceQr.item
                .first()
                .answer.size,
        )

        // Decimal with Number, String, and Invalid types
        val decNumQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("dec_q" to 42.5)).getOrThrow()
        assertEquals(1, decNumQr.item.size)
        val decStrQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("dec_q" to "99.9")).getOrThrow()
        assertEquals(1, decStrQr.item.size)
        val decInvQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("dec_q" to "invalid-dec")).getOrThrow()
        assertTrue(
            decInvQr.item
                .first()
                .answer
                .isEmpty(),
        )
        val decObjQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("dec_q" to listOf(1))).getOrThrow()
        assertTrue(
            decObjQr.item
                .first()
                .answer
                .isEmpty(),
        )

        // Integer with valid String, invalid String, and invalid Object
        val intStrQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("int_q" to "123")).getOrThrow()
        assertEquals(1, intStrQr.item.size)
        val intInvQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("int_q" to "not-int")).getOrThrow()
        assertTrue(
            intInvQr.item
                .first()
                .answer
                .isEmpty(),
        )
        val intObjQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("int_q" to true)).getOrThrow()
        assertTrue(
            intObjQr.item
                .first()
                .answer
                .isEmpty(),
        )

        // Boolean with false and non-boolean
        val boolFalseQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("bool_q" to false)).getOrThrow()
        assertEquals(1, boolFalseQr.item.size)
        val boolInvalidQr =
            QuestionnaireResponseGenerator
                .generateResult(
                    q,
                    mapOf<kotlin.String, Any>("bool_q" to "not-a-bool"),
                ).getOrThrow()
        assertEquals(1, boolInvalidQr.item.size)

        // Attachment with invalid type
        val attInvQr = QuestionnaireResponseGenerator.generateResult(q, mapOf<kotlin.String, Any>("att_q" to 12345)).getOrThrow()
        assertTrue(
            attInvQr.item
                .first()
                .answer
                .isEmpty(),
        )

        // Repeating group with no keys matching repeat prefix - falls back to index 0
        val repeatEmptyKeys = mapOf<kotlin.String, Any>("repeat_group.inner_q" to "fallback_value")
        val repeatEmptyQr = QuestionnaireResponseGenerator.generateResult(q, repeatEmptyKeys).getOrThrow()
        assertEquals(0, repeatEmptyQr.item.size)

        // Empty string and empty list answers
        val emptyAnswers = mapOf<kotlin.String, Any>("child_text" to "   ", "choice_q" to emptyList<Any>())
        val emptyQr = QuestionnaireResponseGenerator.generateResult(q, emptyAnswers).getOrThrow()
        assertTrue(emptyQr.item.isEmpty())
    }
}
