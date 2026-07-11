package io.healthplatform.chartcam.utils

import kotlin.js.Date

actual fun formatLocalizedDate(fhirDate: String): String {
    if (fhirDate.isBlank()) return fhirDate
    return try {
        val date = Date(fhirDate)
        if (date.toString() == "Invalid Date") {
            fhirDate
        } else if (fhirDate.contains("T")) {
            date.toLocaleString()
        } else {
            date.toLocaleDateString()
        }
    } catch (e: Exception) {
        fhirDate
    }
}
