/**
 * @file FhirValidator.kt
 * Contains declarations for FhirValidator.kt.
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.Resource

/**
 * Native kotlin-fhir validation engine wrapper.
 * Enforces StructureDefinition rules on FHIR resources.
 */
object FhirValidator {
    /**
     * Validates a FHIR resource against its StructureDefinition.
     *
     * @param resource The FHIR Resource to validate.
     * @return True if valid, false otherwise.
     */
    fun validate(resource: Resource): Boolean {
        // Mock implementation of a validation engine checking structural requirements
        return when (resource) {
            is Patient -> validatePatient(resource)
            is Questionnaire -> validateQuestionnaire(resource)
            else -> true
        }
    }

    private fun validatePatient(patient: Patient): Boolean {
        if (patient.name.isEmpty()) return false
        val hasGiven = patient.name.any { it.given.isNotEmpty() }
        val hasFamily = patient.name.any { it.family?.value?.isNotEmpty() == true }
        val hasIdentifier = patient.identifier.isNotEmpty()
        return hasGiven && hasFamily && hasIdentifier
    }

    /**
     * Validates a Questionnaire against structural rules.
     * Enforces that the title is present, there is at least one item,
     * no duplicate linkIds exist, and that Choice items have at least one answer option.
     *
     * @param questionnaire The Questionnaire to validate.
     * @return True if valid, false otherwise.
     */
    private fun validateQuestionnaire(questionnaire: Questionnaire): Boolean {
        if (questionnaire.title?.value?.isEmpty() != false) return false
        if (questionnaire.item.isEmpty()) return false

        val linkIds = mutableSetOf<String>()

        return questionnaire.item.all { item ->
            val id = item.linkId?.value
            val hasValidLinkAndText =
                id?.isNotEmpty() == true &&
                    item.text?.value?.isNotEmpty() == true

            if (id != null && !linkIds.add(id)) return@all false

            val isChoice = item.type.value == Questionnaire.QuestionnaireItemType.Choice
            val hasValidOptions = if (isChoice) item.answerOption.isNotEmpty() else true

            hasValidLinkAndText && hasValidOptions
        }
    }
}
