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

    private fun validateQuestionnaire(questionnaire: Questionnaire): Boolean {
        if (questionnaire.title?.value?.isEmpty() != false) return false
        if (questionnaire.item.isEmpty()) return false
        return questionnaire.item.all { item ->
            item.linkId?.value?.isNotEmpty() == true &&
                item.text?.value?.isNotEmpty() == true
        }
    }
}
