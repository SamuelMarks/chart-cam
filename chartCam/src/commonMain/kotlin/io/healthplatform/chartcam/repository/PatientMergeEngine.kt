/**
 * @file PatientMergeEngine.kt
 * Engine for merging conflicting patient records and re-parenting dependent clinical resources.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Engine responsible for executing conflict resolution strategies across Patients and their dependent resources.
 */
class PatientMergeEngine {
    /**
     * Merges non-conflicting demographic fields from incoming patient into local patient.
     * Retains local canonical identifiers while filling in missing demographic data.
     *
     * @param local The existing local patient.
     * @param incoming The incoming patient from external archive.
     * @return Merged [Patient] with local ID and combined demographic details.
     */
    fun mergeDemographics(
        local: Patient,
        incoming: Patient,
    ): Patient =
        local.copy(
            gender = local.gender ?: incoming.gender,
            birthDate = local.birthDate ?: incoming.birthDate,
            telecom = if (local.telecom.isEmpty()) incoming.telecom else local.telecom,
            address = if (local.address.isEmpty()) incoming.address else local.address,
        )

    /**
     * Re-parents an Encounter from an old patient ID to a new target patient ID.
     *
     * @param encounter The Encounter to update.
     * @param targetPatientId The new canonical patient ID.
     * @return Updated [Encounter] referencing the target patient.
     */
    fun reparentEncounter(
        encounter: Encounter,
        targetPatientId: String,
    ): Encounter {
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        return encounter.copy(
            subject = Reference(reference = FhirString(value = "Patient/$cleanTargetId")),
        )
    }

    /**
     * Re-parents a DocumentReference from an old patient ID to a new target patient ID.
     *
     * @param doc The DocumentReference to update.
     * @param targetPatientId The new canonical patient ID.
     * @return Updated [DocumentReference] referencing the target patient.
     */
    fun reparentDocumentReference(
        doc: DocumentReference,
        targetPatientId: String,
    ): DocumentReference {
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        return doc.copy(
            subject = Reference(reference = FhirString(value = "Patient/$cleanTargetId")),
        )
    }

    /**
     * Re-parents a QuestionnaireResponse from an old patient ID to a new target patient ID.
     *
     * @param qr The QuestionnaireResponse to update.
     * @param targetPatientId The new canonical patient ID.
     * @return Updated [QuestionnaireResponse] referencing the target patient.
     */
    fun reparentQuestionnaireResponse(
        qr: QuestionnaireResponse,
        targetPatientId: String,
    ): QuestionnaireResponse {
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        return qr.copy(
            subject = Reference(reference = FhirString(value = "Patient/$cleanTargetId")),
        )
    }
}
