/**
 * @file FhirValidator.kt
 * Validates FHIR R4 resources against core structure and ChartCam clinical rules.
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.Resource

/**
 * Base sealed class for FHIR validation exceptions.
 *
 * @param message Explanation of the validation violation.
 */
sealed class FhirValidationException(
    message: String,
) : Exception(message) {
    /**
     * Exception thrown when a mandatory field is missing in the FHIR resource.
     *
     * @param field The name or path of the missing field.
     */
    class MissingRequiredFieldException(
        val field: String,
    ) : FhirValidationException("Missing required field: $field")

    /**
     * Exception thrown when duplicate linkIds are detected across questionnaire items.
     *
     * @param linkId The duplicated item identifier.
     */
    class DuplicateLinkIdException(
        val linkId: String,
    ) : FhirValidationException("Duplicate linkId found in Questionnaire: $linkId")

    /**
     * Exception thrown when a choice or open-choice question provides no answer options.
     *
     * @param linkId The question linkId lacking options.
     */
    class EmptyChoiceOptionsException(
        val linkId: String,
    ) : FhirValidationException("Choice question '$linkId' must define at least one answer option")

    /**
     * Exception thrown when an enableWhen condition refers to a non-existent question linkId.
     *
     * @param sourceLinkId The question containing the dangling clause.
     * @param targetLinkId The missing target question identifier.
     */
    class DanglingEnableWhenReferenceException(
        val sourceLinkId: String,
        val targetLinkId: String,
    ) : FhirValidationException(
            "Question '$sourceLinkId' has enableWhen condition pointing to non-existent question '$targetLinkId'",
        )
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
        val err = checkPatientErrors(patient)
        return if (err != null) Result.failure(err) else Result.success(Unit)
    }

    /**
     * Evaluates structural patient errors.
     *
     * @param patient The patient resource.
     * @return A validation exception if an error is found, or null.
     */
    private fun checkPatientErrors(patient: Patient): FhirValidationException? =
        when {
            patient.name.isEmpty() -> FhirValidationException.MissingRequiredFieldException("name")
            !patient.name.any { it.given.isNotEmpty() } ->
                FhirValidationException.MissingRequiredFieldException("name.given")
            !patient.name.any { it.family?.value?.isNotEmpty() == true } ->
                FhirValidationException.MissingRequiredFieldException("name.family")
            patient.identifier.isEmpty() -> FhirValidationException.MissingRequiredFieldException("identifier")
            else -> null
        }

    /**
     * Validates a Questionnaire against structural rules.
     *
     * @param questionnaire The Questionnaire to validate.
     * @return A [Result] indicating success or failure.
     */
    private fun validateQuestionnaire(questionnaire: Questionnaire): Result<Unit> {
        val err = checkQuestionnaireErrors(questionnaire)
        return if (err != null) Result.failure(err) else Result.success(Unit)
    }

    /**
     * Evaluates structural questionnaire errors.
     *
     * @param questionnaire The questionnaire resource.
     * @return A validation exception if an error is found, or null.
     */
    private fun checkQuestionnaireErrors(questionnaire: Questionnaire): FhirValidationException? {
        val headerError =
            when {
                questionnaire.title?.value.isNullOrEmpty() ->
                    FhirValidationException.MissingRequiredFieldException("title")
                questionnaire.item.isEmpty() -> FhirValidationException.MissingRequiredFieldException("item")
                else -> null
            }
        if (headerError != null) return headerError

        val linkIds = mutableSetOf<String>()
        return validateItems(questionnaire.item, linkIds) ?: validateEnableWhenClauses(questionnaire.item, linkIds)
    }

    /**
     * Validates individual questionnaire items.
     *
     * @param items List of questionnaire items.
     * @param linkIds Set of encountered linkIds.
     * @return A validation exception if an error is found, or null.
     */
    private fun validateItems(
        items: List<Questionnaire.Item>,
        linkIds: MutableSet<String>,
    ): FhirValidationException? {
        for (item in items) {
            val err = validateSingleItem(item, linkIds)
            if (err != null) return err
        }
        return null
    }

    /**
     * Validates an individual questionnaire item.
     *
     * @param item The item to validate.
     * @param linkIds The set of seen linkIds.
     * @return An exception if invalid, or null.
     */
    private fun validateSingleItem(
        item: Questionnaire.Item,
        linkIds: MutableSet<String>,
    ): FhirValidationException? {
        val id = item.linkId.value
        val isChoice = item.type.value == Questionnaire.QuestionnaireItemType.Choice
        return when {
            id.isNullOrEmpty() -> FhirValidationException.MissingRequiredFieldException("item.linkId")
            item.text?.value.isNullOrEmpty() ->
                FhirValidationException.MissingRequiredFieldException("item.text for '$id'")
            !linkIds.add(id) -> FhirValidationException.DuplicateLinkIdException(id)
            isChoice && item.answerOption.isEmpty() -> FhirValidationException.EmptyChoiceOptionsException(id)
            else -> null
        }
    }

    /**
     * Validates that all enableWhen clauses point to existing linkIds.
     *
     * @param items List of questionnaire items.
     * @param linkIds Set of valid linkIds in the questionnaire.
     * @return A validation exception if a dangling clause is found, or null.
     */
    private fun validateEnableWhenClauses(
        items: List<Questionnaire.Item>,
        linkIds: Set<String>,
    ): FhirValidationException? {
        for (item in items) {
            val sourceId = item.linkId.value ?: ""
            for (ew in item.enableWhen) {
                val target = ew.question.value
                if (target != null && !linkIds.contains(target)) {
                    return FhirValidationException.DanglingEnableWhenReferenceException(sourceId, target)
                }
            }
        }
        return null
    }
}
