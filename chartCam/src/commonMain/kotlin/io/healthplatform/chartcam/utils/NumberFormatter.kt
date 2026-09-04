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
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        withSeparator
            .map { ch ->
                when {
                    ch in '0'..'9' -> arabicDigits[ch - '0']
                    ch == '.' -> '٫'
                    ch == ',' -> '٬'
                    else -> ch
                }
            }.joinToString("")
    } else {
        withSeparator
    }
}
