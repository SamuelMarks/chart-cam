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
 * @param language The BCP-47 language tag to format the date with.
 * @return The localized date string, or the original [fhirDate] if parsing fails.
 */
actual fun formatLocalizedDate(
    fhirDate: String,
    language: String,
): String {
    if (fhirDate.isBlank()) return fhirDate
    return try {
        val date = Date(fhirDate)
        if (date.toString() == "Invalid Date") {
            fhirDate
        } else if (fhirDate.contains("T")) {
            date.asDynamic().toLocaleString(language).unsafeCast<String>()
        } else {
            date.asDynamic().toLocaleDateString(language).unsafeCast<String>()
        }
    } catch (e: IllegalStateException) {
        println(e.message)
        fhirDate
    }
}
