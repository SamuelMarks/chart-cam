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
import kotlin.test.assertFalse
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

        assertTrue(FhirValidator.validate(validPatient), "Patient with given, family, and identifier should be valid")

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

        assertFalse(FhirValidator.validate(missingIdentifierPatient), "Patient missing identifier should be invalid")

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

        assertFalse(FhirValidator.validate(missingNamePatient), "Patient missing name should be invalid")
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

        assertTrue(FhirValidator.validate(validQuestionnaire), "Questionnaire with title and valid items should be valid")

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

        assertFalse(FhirValidator.validate(missingTitleQuestionnaire), "Questionnaire missing title should be invalid")

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

        assertFalse(FhirValidator.validate(missingItemTextQuestionnaire), "Questionnaire item missing text should be invalid")

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

        assertFalse(FhirValidator.validate(duplicateLinkIdQuestionnaire), "Questionnaire with duplicate linkIds should be invalid")
    }
}
