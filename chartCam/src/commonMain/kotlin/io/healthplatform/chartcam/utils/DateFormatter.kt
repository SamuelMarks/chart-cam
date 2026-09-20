/**
 * @file DateFormatter.kt
 * Contains declarations for DateFormatter.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.datetime.LocalDate

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
fun formatLocalizedDate(
    fhirDate: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): String = formatLocalizedDateCatching(fhirDate, language).getOrDefault(fhirDate)

/**
 * Formats a FHIR DateTime string according to the user's current locale.
 *
 * @param fhirDateTime The raw FHIR datetime string (e.g., "1990-01-01T10:00:00Z").
 * @param language The BCP-47 language tag to use for formatting (e.g. "en", "es", "ja").
 * @return The locale-formatted datetime string.
 */
fun formatLocalizedDateTime(
    fhirDateTime: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): String = formatLocalizedDateTimeCatching(fhirDateTime, language).getOrDefault(fhirDateTime)

/**
 * Formats a FHIR date or datetime string into a localized, human-readable format returning a [Result].
 *
 * @param fhirDate The raw FHIR date string (e.g., "1990-01-01" or "1990-01-01T10:00:00Z").
 * @param language The BCP-47 language tag to use for formatting (e.g. "en", "es", "ja").
 * @return A [Result] enclosing the locale-formatted date string.
 */
fun formatLocalizedDateCatching(
    fhirDate: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<String> {
    if (fhirDate.isBlank()) return Result.success("")
    val parsedDate = parseFhirDate(fhirDate)
    return if (parsedDate.isSuccess) {
        parsedDate.flatMap { it.formatLocalized(language) }
    } else {
        parseFhirDateTime(fhirDate).flatMap { it.formatLocalized(language, preserveOffset = false) }
    }
}

/**
 * Formats a FHIR datetime string into a localized, human-readable format returning a [Result].
 *
 * @param fhirDateTime The raw FHIR datetime string (e.g., "1990-01-01T10:00:00Z").
 * @param language The BCP-47 language tag to use for formatting (e.g. "en", "es", "ja").
 * @return A [Result] enclosing the locale-formatted datetime string.
 */
fun formatLocalizedDateTimeCatching(
    fhirDateTime: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<String> {
    if (fhirDateTime.isBlank()) return Result.success("")
    return parseFhirDateTime(fhirDateTime).flatMap { it.formatLocalized(language) }
}

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

    /** Month-first ordering convention (e.g. "MM/DD/YYYY"). */
    MONTH_FIRST("MM/DD/YYYY"),

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
    val isUsEnglish = lang == "en" && (region == "us" || region.isEmpty())
    return when {
        lang in setOf("zh", "ja") -> DatePattern.YEAR_FIRST
        isUsEnglish -> DatePattern.MONTH_FIRST
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
 * Formats a [LocalDate] into a string matching the given [DatePattern].
 *
 * @param date The date to format.
 * @param pattern The [DatePattern] convention to format against.
 * @return The formatted date string.
 */
fun formatDateForPattern(
    date: LocalDate,
    pattern: DatePattern,
): String {
    val iso = date.toString()
    val parts = iso.split("-")
    val y = parts[0]
    val m = parts[1]
    val d = parts[2]
    return when (pattern) {
        DatePattern.DAY_FIRST -> "$d/$m/$y"
        DatePattern.MONTH_FIRST -> "$m/$d/$y"
        DatePattern.YEAR_FIRST -> "$y/$m/$d"
        DatePattern.ISO_STANDARD -> iso
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

/**
 * Safely parses an ISO date string into a [LocalDate], encapsulating the result in a [Result].
 *
 * @param dateStr The date string to parse.
 * @return A [Result] containing the parsed [LocalDate] or failure.
 */
fun parseIsoDate(dateStr: String): Result<LocalDate> = runCatching { LocalDate.parse(dateStr.trim()) }

/**
 * Safely parses a string into a [dev.ohs.fhir.model.r4.FhirDate], encapsulating the result in a [Result].
 *
 * @param string The FHIR date string to parse.
 * @return A [Result] enclosing the parsed [dev.ohs.fhir.model.r4.FhirDate] or an error.
 */
fun parseFhirDate(string: String): Result<dev.ohs.fhir.model.r4.FhirDate> =
    runCatching {
        dev.ohs.fhir.model.r4.FhirDate
            .fromString(string.trim())
    }

/**
 * Safely parses a string into a [dev.ohs.fhir.model.r4.FhirDateTime], encapsulating the result in a [Result].
 *
 * @param string The FHIR datetime string to parse.
 * @return A [Result] enclosing the parsed [dev.ohs.fhir.model.r4.FhirDateTime] or an error.
 */
fun parseFhirDateTime(string: String): Result<dev.ohs.fhir.model.r4.FhirDateTime> =
    runCatching {
        dev.ohs.fhir.model.r4.FhirDateTime
            .fromString(string.trim())
    }

/**
 * Formats a [dev.ohs.fhir.model.r4.FhirDate] according to the specified language layout.
 *
 * @param language The BCP-47 language tag to use for formatting.
 * @return A [Result] enclosing the formatted localized date string.
 */
fun dev.ohs.fhir.model.r4.FhirDate.formatLocalized(
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<String> =
    runCatching {
        val pattern = resolveDatePattern(language)
        when (this) {
            is dev.ohs.fhir.model.r4.FhirDate.Year -> value.toString()
            is dev.ohs.fhir.model.r4.FhirDate.YearMonth -> {
                val y = value.year.toString()
                val m = value.toString().substringAfter('-')
                when (pattern) {
                    DatePattern.YEAR_FIRST -> "$y/$m"
                    DatePattern.MONTH_FIRST -> "$m/$y"
                    DatePattern.DAY_FIRST -> "$m/$y"
                    DatePattern.ISO_STANDARD -> "$y-$m"
                }
            }
            is dev.ohs.fhir.model.r4.FhirDate.Date -> formatDateForPattern(date, pattern)
        }
    }

/**
 * Formats a [dev.ohs.fhir.model.r4.FhirDateTime] according to the specified language layout.
 *
 * @param language The BCP-47 language tag to use for formatting.
 * @param preserveOffset Whether to append the timezone UTC offset string.
 * @return A [Result] enclosing the formatted localized datetime string.
 */
fun dev.ohs.fhir.model.r4.FhirDateTime.formatLocalized(
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
    preserveOffset: Boolean = false,
): Result<String> =
    runCatching {
        val pattern = resolveDatePattern(language)
        when (this) {
            is dev.ohs.fhir.model.r4.FhirDateTime.Year -> value.toString()
            is dev.ohs.fhir.model.r4.FhirDateTime.YearMonth -> {
                val y = value.year.toString()
                val m = value.toString().substringAfter('-')
                when (pattern) {
                    DatePattern.YEAR_FIRST -> "$y/$m"
                    DatePattern.MONTH_FIRST -> "$m/$y"
                    DatePattern.DAY_FIRST -> "$m/$y"
                    DatePattern.ISO_STANDARD -> "$y-$m"
                }
            }
            is dev.ohs.fhir.model.r4.FhirDateTime.Date -> formatDateForPattern(date, pattern)
            is dev.ohs.fhir.model.r4.FhirDateTime.DateTime -> {
                val datePart = formatDateForPattern(dateTime.date, pattern)
                val hour = dateTime.hour.toString().padStart(2, '0')
                val min = dateTime.minute.toString().padStart(2, '0')
                val sec = if (dateTime.second > 0) ":${dateTime.second.toString().padStart(2, '0')}" else ""
                val offset = if (preserveOffset) " $utcOffset" else ""
                "$datePart $hour:$min$sec$offset".trim()
            }
        }
    }

/**
 * Formats a [dev.ohs.fhir.model.r4.FhirDateTime] with full timezone offset preservation.
 *
 * @param language The BCP-47 language tag to use for formatting.
 * @return A [Result] enclosing the formatted localized datetime string with timezone offset.
 */
fun dev.ohs.fhir.model.r4.FhirDateTime.formatLocalizedWithOffset(
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<String> = formatLocalized(language, preserveOffset = true)
