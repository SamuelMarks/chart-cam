package io.healthplatform.chartcam.navigation

import kotlinx.serialization.Serializable

object Routes {
    const val LOGIN = "/auth/login"
    const val CAPTURE = "capture"
    const val PATIENT_LIST = "/patients"
}

@Serializable object TriageRoute
@Serializable data class PatientDetailRoute(val patientId: String)
@Serializable data class PatientVisitsRoute(val patientId: String)
@Serializable data class CaptureForPatientRoute(val patientId: String, val questionnaireId: String? = null)
@Serializable data class VisitDetailRoute(val patientId: String, val visitId: String)
