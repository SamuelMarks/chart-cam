/**
 * @file FhirValidatorJvmTest.kt
 * Test verifying clinical and structural FHIR resource validation using direct data class instances.
 */
package io.healthplatform.chartcam.validation

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.String
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
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
            Patient(
                name =
                    listOf(
                        HumanName(
                            family = String(value = "Doe"),
                            given = listOf(String(value = "John")),
                        ),
                    ),
                identifier = listOf(Identifier(value = String(value = "123"))),
            )
        assertTrue(FhirValidator.validate(patient).isSuccess)
    }

    /**
     * Tests invalid patient validation (no name).
     */
    @Test
    fun testInvalidPatientNoName() {
        val patient = Patient(identifier = listOf(Identifier(value = String(value = "123"))))
        assertTrue(FhirValidator.validate(patient).isFailure)
    }

    /**
     * Tests valid questionnaire validation.
     */
    @Test
    fun testValidQuestionnaire() {
        val q =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = String(value = "Test Q"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            text = String(value = "Text"),
                        ),
                    ),
            )
        assertTrue(FhirValidator.validate(q).isSuccess)
    }

    /**
     * Tests invalid questionnaire validation (empty item).
     */
    @Test
    fun testInvalidQuestionnaireEmptyItem() {
        val q =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = String(value = "Test Q"),
            )
        assertTrue(FhirValidator.validate(q).isFailure)
    }

    /**
     * Tests invalid questionnaire validation (choice with no options).
     */
    @Test
    fun testInvalidQuestionnaireChoiceNoOptions() {
        val q =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = String(value = "Test Q"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            text = String(value = "Pick one"),
                        ),
                    ),
            )
        assertTrue(FhirValidator.validate(q).isFailure)
    }
}
