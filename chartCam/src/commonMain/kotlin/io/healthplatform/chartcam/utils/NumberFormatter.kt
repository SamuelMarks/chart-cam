/**
 * @file NumberFormatter.kt
 * Contains declarations for NumberFormatter.kt.
 *
 * Provides localized formatting for numeric and decimal values across locales.
 */
package io.healthplatform.chartcam.utils

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Formats a decimal number according to the target locale conventions
 * (e.g., '.' vs ',' decimal separator, thousands groupings).
 *
 * @param number The number to format.
 * @param localeTag The language or locale tag (e.g., "en", "es", "de", "ar").
 * @param decimalPlaces The maximum number of decimal fraction digits.
 * @return The formatted decimal string.
 */
fun formatLocalizedDecimal(
    number: Double,
    localeTag: String = "en",
    decimalPlaces: Int = 2,
): String {
    val lang = localeTag.lowercase().split("-", "_").first()
    val isCommaDecimal = lang in setOf("es", "de", "fr", "it", "pt", "ru")
    val isArabic = lang == "ar"

    val multiplier = 10.0.pow(decimalPlaces)
    val rounded = round(number * multiplier) / multiplier
    val intPart = rounded.toLong()
    val fracPart = abs(round((rounded - intPart) * multiplier)).toLong()
    val fracStr = fracPart.toString().padStart(decimalPlaces, '0').trimEnd('0')

    val raw =
        if (fracStr.isEmpty()) {
            intPart.toString()
        } else {
            "$intPart.$fracStr"
        }

    val withSeparator =
        if (isCommaDecimal) {
            raw.replace(".", ",")
        } else {
            raw
        }

    return if (isArabic) {
        val arabicZero = '٠'.code
        val arabicDot = '٫'
        buildString(withSeparator.length) {
            for (i in 0 until withSeparator.length) {
                val ch = withSeparator[i]
                if (ch == '.') {
                    append(arabicDot)
                } else if (ch == '-') {
                    append('-')
                } else {
                    append((arabicZero + (ch - '0')).toChar())
                }
            }
        }
    } else {
        withSeparator
    }
}

/**
 * Parses a localized numeric or decimal string back into a standard [Double].
 *
 * Handles localized decimal commas, standard decimal points, and Arabic-Indic numerals.
 *
 * @param text The localized decimal string representation to parse.
 * @return The parsed [Double] value, or null if parsing fails.
 */
fun parseLocalizedDecimal(text: String): Double? {
    if (text.isBlank()) return null
    val arabicZero = '٠'.code
    val normalized =
        buildString(text.length) {
            for (i in 0 until text.length) {
                val ch = text[i]
                when (ch) {
                    '٫', ',' -> append('.')
                    in '٠'..'٩' -> append((ch.code - arabicZero).toString())
                    else -> append(ch)
                }
            }
        }
    return normalized.toDoubleOrNull()
}
