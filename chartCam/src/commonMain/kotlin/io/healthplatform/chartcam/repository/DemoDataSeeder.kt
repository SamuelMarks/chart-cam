/**
 * @file DemoDataSeeder.kt
 * Contains declarations for DemoDataSeeder.kt.
 *
 * Provides synthetic clinical demonstration datasets and seed/cleanup mechanisms.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.String
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import kotlinx.datetime.LocalDate

/**
 * Manages the seeding and teardown of synthetic clinical demo data.
 * Seeds diverse patient archetypes (pediatric, adult chronic, geriatric)
 * with linked clinical encounters and questionnaire responses.
 */
object DemoDataSeeder {
    /** Patient ID for synthetic pediatric demo record. */
    const val DEMO_PATIENT_PEDIATRIC_ID = "demo_patient_pediatric"

    /** Patient ID for synthetic adult chronic demo record. */
    const val DEMO_PATIENT_ADULT_ID = "demo_patient_adult"

    /** Patient ID for synthetic geriatric demo record. */
    const val DEMO_PATIENT_GERIATRIC_ID = "demo_patient_geriatric"

    /** Encounter ID for synthetic pediatric encounter. */
    const val DEMO_ENCOUNTER_PEDIATRIC_ID = "demo_encounter_pediatric"

    /** Encounter ID for synthetic adult chronic encounter. */
    const val DEMO_ENCOUNTER_ADULT_ID = "demo_encounter_adult"

    /** Encounter ID for synthetic geriatric encounter. */
    const val DEMO_ENCOUNTER_GERIATRIC_ID = "demo_encounter_geriatric"

    /** QuestionnaireResponse ID for synthetic pediatric questionnaire response. */
    const val DEMO_QR_PEDIATRIC_ID = "demo_qr_pediatric"

    /** QuestionnaireResponse ID for synthetic adult questionnaire response. */
    const val DEMO_QR_ADULT_ID = "demo_qr_adult"

    /** QuestionnaireResponse ID for synthetic geriatric questionnaire response. */
    const val DEMO_QR_GERIATRIC_ID = "demo_qr_geriatric"

    /** Date of birth string for pediatric demo patient. */
    private const val DOB_PEDIATRIC = "2021-06-15"

    /** Date of birth string for adult demo patient. */
    private const val DOB_ADULT = "1985-04-12"

    /** Date of birth string for geriatric demo patient. */
    private const val DOB_GERIATRIC = "1948-11-20"

    /** All synthetic demo patient IDs. */
    val ALL_DEMO_PATIENT_IDS =
        listOf(
            DEMO_PATIENT_PEDIATRIC_ID,
            DEMO_PATIENT_ADULT_ID,
            DEMO_PATIENT_GERIATRIC_ID,
        )

    /** All synthetic demo encounter IDs. */
    val ALL_DEMO_ENCOUNTER_IDS =
        listOf(
            DEMO_ENCOUNTER_PEDIATRIC_ID,
            DEMO_ENCOUNTER_ADULT_ID,
            DEMO_ENCOUNTER_GERIATRIC_ID,
        )

    /** All synthetic demo questionnaire response IDs. */
    val ALL_DEMO_QR_IDS =
        listOf(
            DEMO_QR_PEDIATRIC_ID,
            DEMO_QR_ADULT_ID,
            DEMO_QR_GERIATRIC_ID,
        )

    /**
     * Seeds synthetic demo patients, encounters, and questionnaire responses.
     * This operation is idempotent and safe to invoke repeatedly.
     *
     * @param fhirRepository The repository into which demo resources are inserted.
     * @param practitionerId The reference ID of the demo practitioner owning these records.
     */
    suspend fun seedDemoData(
        fhirRepository: FhirRepository,
        practitionerId: kotlin.String = "prac_demo_user",
    ) {
        seedPatients(fhirRepository)
        seedEncounters(fhirRepository, practitionerId)
        seedQuestionnaireResponses(fhirRepository)
    }

    /**
     * Seeds synthetic patient resources across diverse demographic archetypes.
     *
     * @param fhirRepository The repository to save patients into.
     */
    private suspend fun seedPatients(fhirRepository: FhirRepository) {
        val pediatricPatient =
            createFhirPatient(
                id = DEMO_PATIENT_PEDIATRIC_ID,
                firstName = "Leo",
                lastName = "Chen",
                dob = LocalDate.parse(DOB_PEDIATRIC),
                mrnValue = "DEMO-PED-001",
            )
        fhirRepository.savePatient(pediatricPatient)

        val adultPatient =
            createFhirPatient(
                id = DEMO_PATIENT_ADULT_ID,
                firstName = "Sarah",
                lastName = "Jenkins",
                dob = LocalDate.parse(DOB_ADULT),
                mrnValue = "DEMO-ADULT-002",
            )
        fhirRepository.savePatient(adultPatient)

        val geriatricPatient =
            createFhirPatient(
                id = DEMO_PATIENT_GERIATRIC_ID,
                firstName = "Robert",
                lastName = "Miller",
                dob = LocalDate.parse(DOB_GERIATRIC),
                mrnValue = "DEMO-GER-003",
            )
        fhirRepository.savePatient(geriatricPatient)
    }

    /**
     * Seeds synthetic clinical encounters linked to demo patients.
     *
     * @param fhirRepository The repository to save encounters into.
     * @param practitionerId The practitioner identifier.
     */
    private suspend fun seedEncounters(
        fhirRepository: FhirRepository,
        practitionerId: kotlin.String,
    ) {
        val pediatricEncounter =
            createFhirEncounter(
                id = DEMO_ENCOUNTER_PEDIATRIC_ID,
                patientId = "Patient/$DEMO_PATIENT_PEDIATRIC_ID",
                practitionerId = "Practitioner/$practitionerId",
                dateStr = "2024-03-15T09:30:00Z",
            )
        fhirRepository.saveEncounter(pediatricEncounter)

        val adultEncounter =
            createFhirEncounter(
                id = DEMO_ENCOUNTER_ADULT_ID,
                patientId = "Patient/$DEMO_PATIENT_ADULT_ID",
                practitionerId = "Practitioner/$practitionerId",
                dateStr = "2024-03-14T14:15:00Z",
            )
        fhirRepository.saveEncounter(adultEncounter)

        val geriatricEncounter =
            createFhirEncounter(
                id = DEMO_ENCOUNTER_GERIATRIC_ID,
                patientId = "Patient/$DEMO_PATIENT_GERIATRIC_ID",
                practitionerId = "Practitioner/$practitionerId",
                dateStr = "2024-03-12T11:00:00Z",
            )
        fhirRepository.saveEncounter(geriatricEncounter)
    }

    /**
     * Seeds synthetic questionnaire responses linked to demo encounters.
     *
     * @param fhirRepository The repository to save responses into.
     */
    private suspend fun seedQuestionnaireResponses(fhirRepository: FhirRepository) {
        val pediatricQr =
            buildSampleQr(
                qrId = DEMO_QR_PEDIATRIC_ID,
                patientId = DEMO_PATIENT_PEDIATRIC_ID,
                encounterId = DEMO_ENCOUNTER_PEDIATRIC_ID,
                chiefComplaint = "Routine 3-Year Pediatric Well-Child Checkup",
            )
        fhirRepository.saveQuestionnaireResponse(pediatricQr)

        val adultQr =
            buildSampleQr(
                qrId = DEMO_QR_ADULT_ID,
                patientId = DEMO_PATIENT_ADULT_ID,
                encounterId = DEMO_ENCOUNTER_ADULT_ID,
                chiefComplaint = "Adult Hypertension Follow-up & Medication Review",
            )
        fhirRepository.saveQuestionnaireResponse(adultQr)

        val geriatricQr =
            buildSampleQr(
                qrId = DEMO_QR_GERIATRIC_ID,
                patientId = DEMO_PATIENT_GERIATRIC_ID,
                encounterId = DEMO_ENCOUNTER_GERIATRIC_ID,
                chiefComplaint = "Geriatric Fall Risk Assessment & Mobility Evaluation",
            )
        fhirRepository.saveQuestionnaireResponse(geriatricQr)
    }

    /**
     * Clears all synthetic demo resources from the repository to maintain complete isolation.
     *
     * @param fhirRepository The repository from which demo resources are purged.
     */
    suspend fun clearDemoData(fhirRepository: FhirRepository) {
        ALL_DEMO_QR_IDS.forEach { id ->
            fhirRepository.deleteResource("QuestionnaireResponse", id)
        }
        ALL_DEMO_ENCOUNTER_IDS.forEach { id ->
            fhirRepository.deleteEncounter(id)
        }
        ALL_DEMO_PATIENT_IDS.forEach { id ->
            fhirRepository.deletePatient(id)
        }
    }

    /**
     * Helper to construct a standardized mock QuestionnaireResponse.
     *
     * @param qrId Unique identifier for the response.
     * @param patientId Reference identifier of the subject patient.
     * @param encounterId Reference identifier of the parent encounter.
     * @param chiefComplaint Clinical assessment summary answer text.
     * @return A constructed [QuestionnaireResponse].
     */
    private fun buildSampleQr(
        qrId: kotlin.String,
        patientId: kotlin.String,
        encounterId: kotlin.String,
        chiefComplaint: kotlin.String,
    ): QuestionnaireResponse =
        QuestionnaireResponse
            .Builder(status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
            .apply {
                id = qrId
                questionnaire =
                    Canonical
                        .Builder()
                        .apply { value = "Questionnaire/std-form" }
                subject =
                    Reference
                        .Builder()
                        .apply { reference = String.Builder().apply { value = "Patient/$patientId" } }
                encounter =
                    Reference
                        .Builder()
                        .apply { reference = String.Builder().apply { value = "Encounter/$encounterId" } }
                item.add(
                    QuestionnaireResponse.Item
                        .Builder(linkId = String.Builder().apply { value = "q_chief_complaint" })
                        .apply {
                            text = String.Builder().apply { value = "Chief Complaint / Clinical Notes" }
                            answer.add(
                                QuestionnaireResponse.Item.Answer.Builder().apply {
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value.String(
                                            String.Builder().apply { value = chiefComplaint }.build(),
                                        )
                                },
                            )
                        },
                )
            }.build()
}
