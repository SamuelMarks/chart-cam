package io.healthplatform.chartcam.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object Routes {
    const val LOGIN = "/auth/login"
    const val CAPTURE = "/capture"
    const val PATIENT_LIST = "/patients"
}

@Serializable 
@SerialName("/triage")
object TriageRoute

@Serializable 
@SerialName("/patients/{patientId}")
data class PatientDetailRoute(val patientId: String)

@Serializable 
@SerialName("/patients/{patientId}/visits")
data class PatientVisitsRoute(val patientId: String)

@Serializable 
@SerialName("/patients/{patientId}/capture/{questionnaireId}")
data class CaptureForPatientRoute(val patientId: String, val questionnaireId: String? = null)

@Serializable 
@SerialName("/patients/{patientId}/visits/{visitId}")
data class VisitDetailRoute(val patientId: String, val visitId: String)

@Serializable
@SerialName("/patients/{patientId}/visit")
data class NewVisitRoute(val patientId: String)
