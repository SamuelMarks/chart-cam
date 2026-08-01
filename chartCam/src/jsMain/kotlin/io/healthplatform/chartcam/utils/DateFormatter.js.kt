/**
 * @file DateFormatter.js.kt
 * @file DateFormatter.js.kt
 * Contains declarations for DateFormatter.js.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.js.Date

/**
 * Formats a FHIR date or datetime string into a localized, human-readable format on the JS platform.
 *
 * @param fhirDate The date string in FHIR standard format (e.g., ISO 8601).
 * @return The localized date string, or the original [fhirDate] if parsing fails.
 */
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
    } catch (e: IllegalStateException) {
        println(e.message)
        fhirDate
    }
}
