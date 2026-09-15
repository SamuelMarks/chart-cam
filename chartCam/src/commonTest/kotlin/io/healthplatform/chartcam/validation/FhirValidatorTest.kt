/**
 * @file FhirValidatorTest.kt
 * Contains tests for [FhirValidator].
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for [FhirValidator] profiles including structural FHIR rules.
 */
class FhirValidatorTest {
    /**
     * Verifies that Patient validation correctly rejects patients missing required profile fields.
     */
    @Test
    fun testPatientValidationProfile() {
        val validPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName.Builder().apply {
                            given.add(String.Builder().apply { value = "John" })
                            family = String.Builder().apply { value = "Doe" }
                        },
                    )
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "MRN-1234" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(validPatient).isSuccess, "Patient with given, family, and identifier should be valid")

        val missingIdentifierPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName.Builder().apply {
                            given.add(String.Builder().apply { value = "John" })
                            family = String.Builder().apply { value = "Doe" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingIdentifierPatient).isFailure, "Patient missing identifier should be invalid")

        val missingNamePatient =
            Patient
                .Builder()
                .apply {
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "MRN-1234" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingNamePatient).isFailure, "Patient missing name should be invalid")
    }

    /**
     * Verifies that Questionnaire validation correctly rejects questionnaires missing required profile fields.
     */
    @Test
    fun testQuestionnaireValidationProfile() {
        val validQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "What is your age?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(validQuestionnaire).isSuccess, "Questionnaire with title and valid items should be valid")

        val missingTitleQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "What is your age?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingTitleQuestionnaire).isFailure, "Questionnaire missing title should be invalid")

        val missingItemTextQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item.Builder(
                            linkId = String.Builder().apply { value = "item-1" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                        ),
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingItemTextQuestionnaire).isFailure, "Questionnaire item missing text should be invalid")

        val duplicateLinkIdQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "First item?" }
                            },
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "Second item?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(duplicateLinkIdQuestionnaire).isFailure, "Questionnaire with duplicate linkIds should be invalid")
    }

    /**
     * Tests that a Questionnaire with an enableWhen condition pointing to a non-existent linkId fails validation.
     */
    @Test
    fun testDanglingEnableWhenReference() {
        val ewAnswer =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                com.google.fhir.model.r4.Boolean
                    .Builder()
                    .apply { value = true }
                    .build(),
            )
        val qWithDangling =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "Dangling Condition Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            ).apply {
                                text = String.Builder().apply { value = "Do you have symptoms?" }
                                enableWhen.add(
                                    Questionnaire.Item.EnableWhen.Builder(
                                        answer = ewAnswer,
                                        operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                        question = String.Builder().apply { value = "non-existent-item" },
                                    ),
                                )
                            },
                    )
                }.build()

        val result = FhirValidator.validate(qWithDangling)
        assertTrue(result.isFailure, "Questionnaire with dangling enableWhen condition must fail validation")
        assertTrue(result.exceptionOrNull() is FhirValidationException.DanglingEnableWhenReferenceException)
    }
}
