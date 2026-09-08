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
 * Formats a FHIR DateTime string according to the user's current locale.
 *
 * @param fhirDateTime The raw FHIR datetime string (e.g., "1990-01-01T10:00:00Z").
 * @param language The BCP-47 language tag to use for formatting (e.g. "en", "es", "ja").
 * @return The locale-formatted datetime string.
 */
expect fun formatLocalizedDateTime(
    fhirDateTime: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): String

/**
 * Represents the ordering convention of date components for input entry.
 *
 * @property pattern The pattern string representing the date layout.
 */
enum class DatePattern(
    val pattern: String,
) {
    /** Day-first ordering convention (e.g. "DD/MM/YYYY"). */
    DAY_FIRST("DD/MM/YYYY"),

    /** Year-first ordering convention (e.g. "YYYY/MM/DD"). */
    YEAR_FIRST("YYYY/MM/DD"),

    /** ISO standard ordering convention (e.g. "YYYY-MM-DD"). */
    ISO_STANDARD("YYYY-MM-DD"),
}

/**
 * Resolves the structured [DatePattern] for a given language tag.
 *
 * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "en").
 * @return The corresponding [DatePattern] enum value.
 */
fun resolveDatePattern(language: String = io.healthplatform.chartcam.ui.currentLanguageState.value): DatePattern {
    val lower = language.lowercase()
    val parts = lower.split("-", "_")
    val lang = parts.first()
    val region = if (parts.size > 1) parts[1] else ""
    val isCommonwealthEnglish = lang == "en" && region in setOf("gb", "uk", "au", "nz", "ie", "za", "in", "sg")
    return when {
        lang in setOf("zh", "ja") -> DatePattern.YEAR_FIRST
        lang in setOf("es", "he", "iw", "fr", "de", "it", "pt", "ru") || isCommonwealthEnglish -> DatePattern.DAY_FIRST
        else -> DatePattern.ISO_STANDARD
    }
}

/**
 * Returns the localized date input pattern string for the given language.
 *
 * @param language The BCP-47 language tag (e.g. "es", "ja", "he", "en").
 * @return The localized pattern string (e.g. "DD/MM/YYYY", "YYYY/MM/DD", or "YYYY-MM-DD").
 */
fun getLocalizedDatePattern(language: String = io.healthplatform.chartcam.ui.currentLanguageState.value): String =
    resolveDatePattern(language).pattern

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
