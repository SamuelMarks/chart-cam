/**
 * Contains navigation routes used throughout the application.
 */
package io.healthplatform.chartcam.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An object holding constant string values for static navigation routes.
 */
object Routes {
    /**
     * The route for the login screen.
     */
    const val LOGIN = "/auth/login"

    /**
     * The route for the general capture screen.
     */
    const val CAPTURE = "/capture"

    /**
     * The route for the patient list screen.
     */
    const val PATIENT_LIST = "/patients"

    /**
     * The route for the questionnaire list screen.
     */
    const val QUESTIONNAIRE_LIST = "/questionnaires"
}

/**
 * Represents the route for the triage screen.
 */
@Serializable
@SerialName("/triage")
object TriageRoute

/**
 * Represents the route for a specific patient's details.
 *
 * @property patientId The unique identifier of the patient.
 */
@Serializable
@SerialName("/patients/{patientId}")
data class PatientDetailRoute(
    /**
     * The unique identifier of the patient.
     */
    val patientId: String,
)

/**
 * Represents the route for a specific patient's visits.
 *
 * @property patientId The unique identifier of the patient.
 */
@Serializable
@SerialName("/patients/{patientId}/visits")
data class PatientVisitsRoute(
    /**
     * The unique identifier of the patient.
     */
    val patientId: String,
)

/**
 * Represents the route to capture data for a specific patient.
 *
 * @property patientId The unique identifier of the patient.
 * @property questionnaireId An optional identifier for a specific questionnaire to fill.
 */
@Serializable
@SerialName("/patients/{patientId}/capture/{questionnaireId}")
data class CaptureForPatientRoute(
    /**
     * The unique identifier of the patient.
     */
    val patientId: String,
    /**
     * An optional identifier for a specific questionnaire to fill.
     */
    val questionnaireId: String? = null,
    /**
     * An optional specific item linkId to capture photos for.
     */
    val linkId: String? = null,
)

/**
 * Represents the route for a specific visit of a specific patient.
 *
 * @property patientId The unique identifier of the patient.
 * @property visitId The unique identifier of the visit.
 */
@Serializable
@SerialName("/patients/{patientId}/visits/{visitId}")
data class VisitDetailRoute(
    /**
     * The unique identifier of the patient.
     */
    val patientId: String,
    /**
     * The unique identifier of the visit.
     */
    val visitId: String,
)

/**
 * Represents the route to create a new visit for a specific patient.
 *
 * @property patientId The unique identifier of the patient.
 */
@Serializable
@SerialName("/patients/{patientId}/visit")
data class NewVisitRoute(
    /**
     * The unique identifier of the patient.
     */
    val patientId: String,
)

/**
 * Represents the route to build a new questionnaire.
 *
 * @property duplicateFromId Optional ID of an existing questionnaire to use as a starting point.
 */
@Serializable
@SerialName("/questionnaires/build")
data class QuestionnaireBuilderRoute(
    /**
     * Optional ID of an existing questionnaire to duplicate fields from.
     */
    val duplicateFromId: String? = null,
)
