/**
 * @file DateFormatter.ios.kt
 */
package io.healthplatform.chartcam.utils

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithFullDate
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSLocale

/**
 * Helper to parse date.
 * @param fhirDate The fhirDate.
 * @return The result.
 */
private fun parseFhirDate(fhirDate: String): NSDate? {
    val iso = NSISO8601DateFormatter()
    val d1 = iso.dateFromString(fhirDate)
    if (d1 != null) return d1
    val iso2 =
        NSISO8601DateFormatter().apply {
            formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
        }
    val iso3 =
        NSISO8601DateFormatter().apply {
            formatOptions = NSISO8601DateFormatWithFullDate
        }
    return iso2.dateFromString(fhirDate) ?: iso3.dateFromString(fhirDate)
}

/**
 * Formats a FHIR date or datetime string into a localized format.
 *
 * @param fhirDate The date string.
 * @param language The language code.
 * @return The localized date string.
 */
actual fun formatLocalizedDate(
    fhirDate: String,
    language: String,
): String {
    var res = fhirDate
    if (fhirDate.isNotBlank()) {
        val date = parseFhirDate(fhirDate)
        if (date != null) {
            val formatter = NSDateFormatter()
            formatter.locale = NSLocale(localeIdentifier = language)
            formatter.dateStyle = NSDateFormatterMediumStyle
            formatter.timeStyle = if (fhirDate.contains("T")) NSDateFormatterMediumStyle else 0UL

            try {
                res = formatter.stringFromDate(date)
            } catch (ignored: Exception) {
                println(ignored.message)
            }
        }
    }
    return res
}

/**
 * Formats a FHIR datetime string into a localized format.
 *
 * @param fhirDateTime The datetime string.
 * @param language The language code.
 * @return The localized datetime string.
 */
actual fun formatLocalizedDateTime(
    fhirDateTime: String,
    language: String,
): String {
    var res = fhirDateTime
    if (fhirDateTime.isNotBlank()) {
        val date = parseFhirDate(fhirDateTime)
        if (date != null) {
            val formatter = NSDateFormatter()
            formatter.locale = NSLocale(localeIdentifier = language)
            formatter.dateStyle = NSDateFormatterMediumStyle
            formatter.timeStyle = NSDateFormatterMediumStyle

            try {
                res = formatter.stringFromDate(date)
            } catch (ignored: Exception) {
                println(ignored.message)
            }
        }
    }
    return res
}
