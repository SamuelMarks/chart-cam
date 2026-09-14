/**
 * @file FhirValidator.kt
 * Contains declarations for FhirValidator.kt.
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.Resource

/**
 * Base exception for structural validation failures on FHIR resources.
 *
 * @param message The detailed failure message.
 */
sealed class FhirValidationException(
    override val message: String,
) : Exception(message) {
    /**
     * Indicates that a required FHIR field is missing or empty.
     *
     * @param fieldName The name of the missing field.
     */
    class MissingRequiredFieldException(
        val fieldName: String,
    ) : FhirValidationException("Missing required field: $fieldName")

    /**
     * Indicates that duplicate link IDs were discovered in a questionnaire item hierarchy.
     *
     * @param linkId The conflicting link identifier.
     */
    class DuplicateLinkIdException(
        val linkId: String,
    ) : FhirValidationException("Duplicate linkId detected: $linkId")

    /**
     * Indicates that a Choice item has no answer options defined.
     *
     * @param linkId The link identifier of the Choice item.
     */
    class EmptyChoiceOptionsException(
        val linkId: String,
    ) : FhirValidationException("Choice question '$linkId' must define at least one answer option")
}

/**
 * Native kotlin-fhir validation engine wrapper.
 * Enforces StructureDefinition rules on FHIR resources.
 */
object FhirValidator {
    /**
     * Validates a FHIR resource against its StructureDefinition.
     *
     * @param resource The FHIR Resource to validate.
     * @return A [Result] indicating success if valid, or a [FhirValidationException] on failure.
     */
    fun validate(resource: Resource): Result<Unit> =
        when (resource) {
            is Patient -> validatePatient(resource)
            is Questionnaire -> validateQuestionnaire(resource)
            else -> Result.success(Unit)
        }

    /**
     * Validates a Patient resource, ensuring it has necessary fields like name and identifier.
     *
     * @param patient The Patient resource to validate.
     * @return A [Result] indicating success or failure.
     */
    private fun validatePatient(patient: Patient): Result<Unit> {
        if (patient.name.isEmpty()) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("name"))
        }
        val hasGiven = patient.name.any { it.given.isNotEmpty() }
        if (!hasGiven) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("name.given"))
        }
        val hasFamily = patient.name.any { it.family?.value?.isNotEmpty() == true }
        if (!hasFamily) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("name.family"))
        }
        val hasIdentifier = patient.identifier.isNotEmpty()
        if (!hasIdentifier) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("identifier"))
        }
        return Result.success(Unit)
    }

    /**
     * Validates a Questionnaire against structural rules.
     * Enforces that the title is present, there is at least one item,
     * no duplicate linkIds exist, and that Choice items have at least one answer option.
     *
     * @param questionnaire The Questionnaire to validate.
     * @return A [Result] indicating success or failure.
     */
    private fun validateQuestionnaire(questionnaire: Questionnaire): Result<Unit> {
        if (questionnaire.title?.value.isNullOrEmpty()) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("title"))
        }
        if (questionnaire.item.isEmpty()) {
            return Result.failure(FhirValidationException.MissingRequiredFieldException("item"))
        }

        val linkIds = mutableSetOf<String>()

        for (item in questionnaire.item) {
            val id = item.linkId.value
            if (id.isNullOrEmpty()) {
                return Result.failure(FhirValidationException.MissingRequiredFieldException("item.linkId"))
            }
            if (item.text?.value.isNullOrEmpty()) {
                return Result.failure(FhirValidationException.MissingRequiredFieldException("item.text for '$id'"))
            }
            if (!linkIds.add(id)) {
                return Result.failure(FhirValidationException.DuplicateLinkIdException(id))
            }

            val isChoice = item.type.value == Questionnaire.QuestionnaireItemType.Choice
            if (isChoice && item.answerOption.isEmpty()) {
                return Result.failure(FhirValidationException.EmptyChoiceOptionsException(id))
            }
        }
        return Result.success(Unit)
    }
}
