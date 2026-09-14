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
 * @param useGrouping Whether to format with localized thousands grouping separators.
 * @return The formatted decimal string.
 */
fun formatLocalizedDecimal(
    number: Double,
    localeTag: String = "en",
    decimalPlaces: Int = 2,
    useGrouping: Boolean = false,
): String {
    val lang = localeTag.lowercase().split("-", "_").first()
    val isCommaDecimal = lang in setOf("es", "de", "fr", "it", "pt", "ru")
    val isSpaceGrouping = lang in setOf("fr", "ru")
    val isArabic = lang == "ar"

    val multiplier = 10.0.pow(decimalPlaces)
    val rounded = round(number * multiplier) / multiplier
    val isNegative = rounded < 0.0
    val absRounded = abs(rounded)
    val intPart = absRounded.toLong()
    val fracPart = abs(round((absRounded - intPart) * multiplier)).toLong()
    val fracStr = fracPart.toString().padStart(decimalPlaces, '0').trimEnd('0')

    val intStr =
        if (useGrouping) {
            val groupSeparator =
                when {
                    isSpaceGrouping -> " "
                    isCommaDecimal -> "."
                    else -> ","
                }
            val rawInt = intPart.toString()
            val sb = StringBuilder()
            val len = rawInt.length
            for (i in 0 until len) {
                sb.append(rawInt[i])
                val distFromEnd = len - 1 - i
                if (distFromEnd > 0 && distFromEnd % 3 == 0) {
                    sb.append(groupSeparator)
                }
            }
            sb.toString()
        } else {
            intPart.toString()
        }

    val sign = if (isNegative) "-" else ""
    val decimalSep = if (isCommaDecimal) "," else "."
    val raw =
        if (fracStr.isEmpty()) {
            "$sign$intStr"
        } else {
            "$sign$intStr$decimalSep$fracStr"
        }

    return if (isArabic) {
        val arabicZero = '٠'.code
        val arabicDot = '٫'
        val arabicComma = '٬'
        buildString(raw.length) {
            for (i in 0 until raw.length) {
                val ch = raw[i]
                if (ch == '.') {
                    append(arabicDot)
                } else if (ch == ',') {
                    append(arabicComma)
                } else if (ch == '-') {
                    append('-')
                } else {
                    append((arabicZero + (ch - '0')).toChar())
                }
            }
        }
    } else {
        raw
    }
}

/**
 * Parses a localized numeric or decimal string back into a standard [Double].
 *
 * Handles localized decimal commas, standard decimal points, thousands separators, and Arabic-Indic numerals.
 *
 * @param text The localized decimal string representation to parse.
 * @return The parsed [Double] value, or null if parsing fails.
 */
fun parseLocalizedDecimal(text: String): Double? {
    if (text.isBlank()) return null
    val arabicZero = '٠'.code
    val ascii =
        buildString(text.length) {
            for (i in 0 until text.length) {
                val ch = text[i]
                when (ch) {
                    in '٠'..'٩' -> append((ch.code - arabicZero).toString())
                    '٫' -> append('.')
                    '٬' -> append(',')
                    else -> append(ch)
                }
            }
        }.trim()

    val withoutSpaces = ascii.replace(" ", "").replace("\u00A0", "").replace("\u202F", "")
    val lastDot = withoutSpaces.lastIndexOf('.')
    val lastComma = withoutSpaces.lastIndexOf(',')

    val normalized =
        if (lastDot != -1 && lastComma != -1) {
            if (lastDot > lastComma) {
                withoutSpaces.replace(",", "")
            } else {
                withoutSpaces.replace(".", "").replace(",", ".")
            }
        } else if (lastComma != -1) {
            val afterComma = withoutSpaces.length - 1 - lastComma
            if (withoutSpaces.count { it == ',' } > 1 || afterComma == 3) {
                withoutSpaces.replace(",", "")
            } else {
                withoutSpaces.replace(",", ".")
            }
        } else if (withoutSpaces.count { it == '.' } > 1) {
            withoutSpaces.replace(".", "")
        } else {
            withoutSpaces
        }

    return normalized.toDoubleOrNull()
}
