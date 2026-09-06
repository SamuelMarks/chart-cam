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
 *
 * @param fhirDate The raw FHIR date string (e.g., "1990-01-01" or "1990-01-01T10:00:00Z").
 * @param language The BCP-47 language tag to use for formatting (e.g. "en", "es", "ja").
 * @return The locale-formatted date string.
 */
expect fun formatLocalizedDate(
    fhirDate: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): String

/**
 * Returns the localized date input pattern string for the given language.
 *
 * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "en").
 * @return The localized pattern string (e.g. "DD/MM/YYYY", "YYYY/MM/DD", or "YYYY-MM-DD").
 */
fun getLocalizedDatePattern(language: String = io.healthplatform.chartcam.ui.currentLanguageState.value): String {
    val lang = language.lowercase().split("-", "_").first()
    return when (lang) {
        "zh", "ja" -> "YYYY/MM/DD"
        "es", "he" -> "DD/MM/YYYY"
        else -> "YYYY-MM-DD"
    }
}

/**
 * Returns the localized date-time input pattern string for the given language.
 *
 * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "en").
 * @return The localized pattern string (e.g. "DD/MM/YYYY HH:MM", "YYYY/MM/DD HH:MM", or "YYYY-MM-DD HH:MM").
 */
fun getLocalizedDateTimePattern(language: String = io.healthplatform.chartcam.ui.currentLanguageState.value): String {
    val datePattern = getLocalizedDatePattern(language)
    return "$datePattern HH:MM"
}
