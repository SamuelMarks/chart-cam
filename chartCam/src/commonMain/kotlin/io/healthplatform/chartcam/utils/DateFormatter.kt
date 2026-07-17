/**
 * @file DateFormatter.kt
 * Contains declarations for DateFormatter.kt.
 */
package io.healthplatform.chartcam.utils

/**
 * Formats a FHIR Date or DateTime string according to the user's current locale.
 *
 * Example:
 * ```kotlin
 * val displayDate = formatLocalizedDate("1990-01-01T10:00:00Z")
 * println(displayDate) // Output varies by locale, e.g., "Jan 1, 1990"
 * ```

 * @param fhirDate The raw FHIR date string (e.g., "1990-01-01" or "1990-01-01T10:00:00Z").
 * @return The locale-formatted date string.
 */
expect fun formatLocalizedDate(fhirDate: String): String
