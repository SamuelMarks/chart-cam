/**
 * @file PatientMergeEngine.kt
 * Engine for merging conflicting patient records and re-parenting dependent clinical resources.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.String as FhirString

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
    ): Patient {
        val builder = local.toBuilder()
        if (local.gender == null && incoming.gender != null) {
            builder.gender = incoming.gender
        }
        if (local.birthDate == null && incoming.birthDate != null) {
            builder.birthDate = incoming.birthDate?.toBuilder()
        }
        if (local.telecom.isEmpty() && incoming.telecom.isNotEmpty()) {
            incoming.telecom.forEach { builder.telecom.add(it.toBuilder()) }
        }
        if (local.address.isEmpty() && incoming.address.isNotEmpty()) {
            incoming.address.forEach { builder.address.add(it.toBuilder()) }
        }
        return builder.build()
    }

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
        val builder = encounter.toBuilder()
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        builder.subject =
            Reference.Builder().apply {
                reference = FhirString.Builder().apply { value = "Patient/$cleanTargetId" }
            }
        return builder.build()
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
        val builder = doc.toBuilder()
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        builder.subject =
            Reference.Builder().apply {
                reference = FhirString.Builder().apply { value = "Patient/$cleanTargetId" }
            }
        return builder.build()
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
        val builder = qr.toBuilder()
        val cleanTargetId = targetPatientId.removePrefix("Patient/")
        builder.subject =
            Reference.Builder().apply {
                reference = FhirString.Builder().apply { value = "Patient/$cleanTargetId" }
            }
        return builder.build()
    }
}
