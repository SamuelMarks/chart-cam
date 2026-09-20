/**
 * @file OfflineValueSetValidatorTest.kt
 * Unit tests for OfflineValueSetValidator verifying Coding, CodeableConcept, and ExtensibleEnumeration validation.
 */
package io.healthplatform.chartcam.validation

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.OperationOutcome
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite for [OfflineValueSetValidator].
 */
class OfflineValueSetValidatorTest {
    /**
     * Verifies validation of a standard FHIR Coding against a permitted code set.
     */
    @Test
    fun testValidateCoding() {
        val validCoding =
            Coding(
                system = Uri(value = "http://loinc.org"),
                code = Code(value = "87474-3"),
                display = FhirString(value = "Fitzpatrick skin type"),
            )
        val outcome = OfflineValueSetValidator.validateCoding(validCoding, setOf("87474-3", "72514-3")).getOrThrow()
        assertNull(outcome)

        val invalidCoding =
            Coding(
                system = Uri(value = "http://loinc.org"),
                code = Code(value = "99999-9"),
            )
        val invalidOutcome = OfflineValueSetValidator.validateCoding(invalidCoding, setOf("87474-3")).getOrThrow()
        assertNotNull(invalidOutcome)
        assertEquals(1, invalidOutcome.issue.size)

        // Null code element
        val nullCode = Coding(system = Uri(value = "http://loinc.org"), code = null)
        val nullCodeOutcome = OfflineValueSetValidator.validateCoding(nullCode, setOf("87474-3")).getOrThrow()
        assertNotNull(nullCodeOutcome)
        assertEquals(
            OperationOutcome.IssueType.Required,
            nullCodeOutcome.issue
                .first()
                .code.value,
        )

        // Blank code element
        val blankCode = Coding(system = Uri(value = "http://loinc.org"), code = Code(value = "   "))
        val blankCodeOutcome = OfflineValueSetValidator.validateCoding(blankCode, setOf("87474-3")).getOrThrow()
        assertNotNull(blankCodeOutcome)
        assertEquals(
            OperationOutcome.IssueType.Required,
            blankCodeOutcome.issue
                .first()
                .code.value,
        )

        // Expected system mismatch
        val systemMismatch = Coding(system = Uri(value = "http://snomed.info/sct"), code = Code(value = "87474-3"))
        val sysOutcome = OfflineValueSetValidator.validateCoding(systemMismatch, setOf("87474-3"), "http://loinc.org").getOrThrow()
        assertNotNull(sysOutcome)
        assertEquals(
            OperationOutcome.IssueType.Code_Invalid,
            sysOutcome.issue
                .first()
                .code.value,
        )

        // System is null when expectedSystem is provided
        val nullSysCoding = Coding(system = null, code = Code(value = "87474-3"))
        val nullSysOutcome = OfflineValueSetValidator.validateCoding(nullSysCoding, setOf("87474-3"), "http://loinc.org").getOrThrow()
        assertNotNull(nullSysOutcome)

        // Empty allowedCodes set means any code is permitted as long as system matches
        val emptyAllowedOutcome = OfflineValueSetValidator.validateCoding(validCoding, emptySet(), "http://loinc.org").getOrThrow()
        assertNull(emptyAllowedOutcome)
    }

    /**
     * Verifies validation of [ExtensibleEnumeration] for predefined and custom codes.
     */
    @Test
    fun testValidateExtensibleEnumeration() {
        // 1. Predefined standard enum constant
        val predefined = ExtensibleEnumeration.of(AdministrativeGender.Female)
        val predefinedRes = OfflineValueSetValidator.validateExtensibleEnumeration(predefined).getOrThrow()
        assertNull(predefinedRes)

        // 2. Custom code in permitted extension set
        val customAllowed = ExtensibleEnumeration.of("non-binary")
        val customAllowedRes =
            OfflineValueSetValidator
                .validateExtensibleEnumeration(
                    customAllowed,
                    allowedCustomCodes = setOf("non-binary", "two-spirit"),
                ).getOrThrow()
        assertNull(customAllowedRes)

        // 3. Custom code not in permitted extension set
        val customDisallowed = ExtensibleEnumeration.of("unrecognized")
        val customDisallowedRes =
            OfflineValueSetValidator
                .validateExtensibleEnumeration(
                    customDisallowed,
                    allowedCustomCodes = setOf("non-binary"),
                ).getOrThrow()
        assertNotNull(customDisallowedRes)
        assertEquals(1, customDisallowedRes.issue.size)

        // 4. Blank custom code is rejected as an error
        val blankCustom = ExtensibleEnumeration.of("   ")
        val blankCustomRes = OfflineValueSetValidator.validateExtensibleEnumeration(blankCustom).getOrThrow()
        assertNotNull(blankCustomRes)

        // 5. Custom code when allowedCustomCodes is null (unrestricted custom codes)
        val customUnrestricted = ExtensibleEnumeration.of("custom-unrestricted")
        val customUnrestrictedRes = OfflineValueSetValidator.validateExtensibleEnumeration(customUnrestricted, null).getOrThrow()
        assertNull(customUnrestrictedRes)
    }

    /**
     * Verifies validation of [CodeableConcept] with empty, valid, and invalid codings.
     */
    @Test
    fun testValidateCodeableConcept() {
        // Empty coding list
        val emptyConcept = CodeableConcept(coding = emptyList())
        val emptyRes = OfflineValueSetValidator.validateCodeableConcept(emptyConcept, setOf("87474-3")).getOrThrow()
        assertNotNull(emptyRes)
        assertEquals(
            OperationOutcome.IssueType.Required,
            emptyRes.issue
                .first()
                .code.value,
        )

        // Valid coding inside concept
        val validConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = "http://loinc.org"),
                            code = Code(value = "87474-3"),
                        ),
                    ),
            )
        val validRes = OfflineValueSetValidator.validateCodeableConcept(validConcept, setOf("87474-3"), "http://loinc.org").getOrThrow()
        assertNull(validRes)

        // Invalid coding inside concept
        val invalidConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = "http://loinc.org"),
                            code = Code(value = "invalid-code"),
                        ),
                    ),
            )
        val invalidRes = OfflineValueSetValidator.validateCodeableConcept(invalidConcept, setOf("87474-3"), "http://loinc.org").getOrThrow()
        assertNotNull(invalidRes)
        assertEquals(1, invalidRes.issue.size)

        // Mixed concept with both valid and invalid codings
        val mixedConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = "http://loinc.org"),
                            code = Code(value = "87474-3"),
                        ),
                        Coding(
                            system = Uri(value = "http://loinc.org"),
                            code = Code(value = "invalid-code"),
                        ),
                    ),
            )
        val mixedRes = OfflineValueSetValidator.validateCodeableConcept(mixedConcept, setOf("87474-3"), "http://loinc.org").getOrThrow()
        assertNotNull(mixedRes)
        assertEquals(1, mixedRes.issue.size)
    }

    /**
     * Verifies response options validation covering String and Coding answer options and responses.
     */
    @Test
    fun testValidateResponseOptionsComprehensive() {
        val questionnaire =
            Questionnaire(
                id = "q-options-test",
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        // 1. Choice item with String options
                        Questionnaire.Item(
                            linkId = FhirString(value = "string-choice"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            answerOption =
                                listOf(
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .String(FhirString(value = "A")),
                                    ),
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .String(FhirString(value = "B")),
                                    ),
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .Integer(Integer(value = 42)),
                                    ),
                                ),
                        ),
                        // 2. Choice item with Coding options
                        Questionnaire.Item(
                            linkId = FhirString(value = "coding-choice"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            answerOption =
                                listOf(
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.Coding(
                                                Coding(
                                                    system = Uri(value = "http://example.com"),
                                                    code = Code(value = "code-1"),
                                                ),
                                            ),
                                    ),
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.Coding(
                                                Coding(code = null),
                                            ),
                                    ),
                                ),
                        ),
                        // 3. Choice item with no answerOption
                        Questionnaire.Item(
                            linkId = FhirString(value = "empty-choice"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            answerOption = emptyList(),
                        ),
                        // 4. Non-choice item
                        Questionnaire.Item(
                            linkId = FhirString(value = "text-item"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // 5. Item identified only by linkId.id
                        Questionnaire.Item(
                            linkId = FhirString(id = "id-only-link"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            answerOption =
                                listOf(
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value
                                                .String(FhirString(value = "Opt1")),
                                    ),
                                ),
                        ),
                        // 6. Nested item structure
                        Questionnaire.Item(
                            linkId = FhirString(value = "group-item"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            item =
                                listOf(
                                    Questionnaire.Item(
                                        linkId = FhirString(value = "nested-choice"),
                                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                                        answerOption =
                                            listOf(
                                                Questionnaire.Item.AnswerOption(
                                                    value =
                                                        Questionnaire.Item.AnswerOption.Value
                                                            .String(FhirString(value = "NestedVal")),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        // 7. Item with blank linkId (ignored in index)
                        Questionnaire.Item(
                            linkId = FhirString(value = "   "),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // 8. Item with null linkId value and null id (ignored in index)
                        Questionnaire.Item(
                            linkId = FhirString(),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )

        val response =
            QuestionnaireResponse(
                id = "qr-options-test",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "string-choice"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "A")),
                                    ),
                                    // Unknown answer
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "C")),
                                    ),
                                    // Other value type (e.g. Integer) ignored in choice check
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .Integer(Integer(value = 1)),
                                    ),
                                ),
                        ),
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "coding-choice"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Coding(
                                                Coding(code = Code(value = "code-1")),
                                            ),
                                    ),
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Coding(
                                                Coding(code = Code(value = "code-unknown")),
                                            ),
                                    ),
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Coding(
                                                Coding(code = null),
                                            ),
                                    ),
                                ),
                        ),
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "empty-choice"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Any")),
                                    ),
                                ),
                        ),
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "text-item"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Hello")),
                                    ),
                                ),
                        ),
                        QuestionnaireResponse.Item(
                            linkId = FhirString(id = "id-only-link"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(FhirString(value = "Opt1")),
                                    ),
                                ),
                        ),
                        // Response with null linkId value but id present
                        QuestionnaireResponse.Item(
                            linkId = FhirString(id = "unknown-id"),
                        ),
                        // Response with null linkId value and null id
                        QuestionnaireResponse.Item(
                            linkId = FhirString(),
                        ),
                        // Nested response structure
                        QuestionnaireResponse.Item(
                            linkId = FhirString(value = "group-item"),
                            item =
                                listOf(
                                    QuestionnaireResponse.Item(
                                        linkId = FhirString(value = "nested-choice"),
                                        answer =
                                            listOf(
                                                QuestionnaireResponse.Item.Answer(
                                                    value =
                                                        QuestionnaireResponse.Item.Answer.Value
                                                            .String(FhirString(value = "NestedVal")),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val outcome = OfflineValueSetValidator.validateResponseOptions(questionnaire, response).getOrThrow()
        assertNotNull(outcome)
        // Should have 2 issues: "C" in string-choice, "code-unknown" in coding-choice
        assertEquals(2, outcome.issue.size)
        assertTrue(outcome.issue.any { it.diagnostics?.value?.contains("'C'") == true })
        assertTrue(outcome.issue.any { it.diagnostics?.value?.contains("'code-unknown'") == true })
    }
}
