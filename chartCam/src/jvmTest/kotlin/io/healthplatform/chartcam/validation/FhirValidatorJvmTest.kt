/**
 * @file FhirValidatorJvmTest.kt
 * Contains declarations for FhirValidatorJvmTest.kt.
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.PublicationStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test class for FhirValidator on JVM.
 */
class FhirValidatorJvmTest {
    /**
     * Tests valid patient validation.
     */
    @Test
    fun testValidPatient() {
        val patient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName.Builder().apply {
                            family = String.Builder().apply { value = "Doe" }
                            given.add(String.Builder().apply { value = "John" })
                        },
                    )
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "123" }
                        },
                    )
                }.build()
        assertTrue(FhirValidator.validate(patient))
    }

    /**
     * Tests invalid patient validation (no name).
     */
    @Test
    fun testInvalidPatientNoName() {
        val patient =
            Patient
                .Builder()
                .apply {
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "123" }
                        },
                    )
                }.build()
        assertFalse(FhirValidator.validate(patient))
    }

    /**
     * Tests valid questionnaire validation.
     */
    @Test
    fun testValidQuestionnaire() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "Test Q" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                text = String.Builder().apply { value = "Text" }
                            },
                    )
                }.build()
        assertTrue(FhirValidator.validate(q))
    }

    /**
     * Tests invalid questionnaire validation (empty item).
     */
    @Test
    fun testInvalidQuestionnaireEmptyItem() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "Test Q" }
                }.build()
        assertFalse(FhirValidator.validate(q))
    }

    /**
     * Tests invalid questionnaire validation (choice with no options).
     */
    @Test
    fun testInvalidQuestionnaireChoiceNoOptions() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "Test Q" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            ).apply {
                                text = String.Builder().apply { value = "Pick one" }
                                // No answerOptions added!
                            },
                    )
                }.build()
        assertFalse(FhirValidator.validate(q))
    }
}
