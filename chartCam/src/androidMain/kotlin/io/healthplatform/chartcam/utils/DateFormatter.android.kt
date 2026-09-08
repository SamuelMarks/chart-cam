/**
 * @file DateFormatter.android.kt
 * Contains declarations for DateFormatter.android.kt.
 */
package io.healthplatform.chartcam.utils

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats a FHIR date or datetime string into a localized, human-readable format on the Android platform.
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
    return if (fhirDate.contains("T")) {
        formatLocalizedDateTime(fhirDate, language)
    } else {
        val locale = Locale.forLanguageTag(language)
        try {
            val date = LocalDate.parse(fhirDate)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            date.format(formatter)
        } catch (e: java.time.format.DateTimeParseException) {
            println("Date parsing failed: ${e.message}")
            fhirDate // Fallback to raw string on parse error
        }
    }
}

/**
 * Formats a FHIR datetime string into a localized, human-readable format on the Android platform.
 *
 * @param fhirDateTime The datetime string in FHIR standard format (e.g., ISO 8601).
 * @param language The BCP-47 language tag to format the datetime with.
 * @return The localized datetime string, or the original [fhirDateTime] if parsing fails.
 */
actual fun formatLocalizedDateTime(
    fhirDateTime: String,
    language: String,
): String {
    if (fhirDateTime.isBlank()) return fhirDateTime
    val locale = Locale.forLanguageTag(language)
    return try {
        val dateTime =
            try {
                ZonedDateTime.parse(fhirDateTime)
            } catch (_: java.time.format.DateTimeParseException) {
                java.time.LocalDateTime
                    .parse(fhirDateTime)
                    .atZone(java.time.ZoneId.systemDefault())
            }
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
        dateTime.format(formatter)
    } catch (e: java.time.format.DateTimeParseException) {
        println("DateTime parsing failed: ${e.message}")
        fhirDateTime
    }
}
