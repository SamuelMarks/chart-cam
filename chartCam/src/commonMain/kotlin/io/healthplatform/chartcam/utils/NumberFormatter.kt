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

private const val THOUSANDS_GROUP_SIZE = 3

/**
 * Formats the integer portion with localized grouping separators.
 *
 * @param intPart The absolute integer part.
 * @param useGrouping Whether grouping is enabled.
 * @param isSpaceGrouping Whether space is used as separator.
 * @param isCommaDecimal Whether period is used as thousands separator.
 * @return The formatted integer string.
 */
private fun formatIntegerPart(
    intPart: Long,
    useGrouping: Boolean,
    isSpaceGrouping: Boolean,
    isCommaDecimal: Boolean,
): String {
    if (!useGrouping) return intPart.toString()
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
        if (distFromEnd > 0 && distFromEnd % THOUSANDS_GROUP_SIZE == 0) {
            sb.append(groupSeparator)
        }
    }
    return sb.toString()
}

/**
 * Converts Latin digits and separators to Eastern Arabic glyphs.
 *
 * @param raw The Latin string.
 * @return String with Arabic numerals and separators.
 */
private fun toArabicDigits(raw: String): String {
    val arabicZero = '٠'.code
    val arabicDot = '٫'
    val arabicComma = '٬'
    return buildString(raw.length) {
        for (i in 0 until raw.length) {
            val ch = raw[i]
            when (ch) {
                '.' -> append(arabicDot)
                ',' -> append(arabicComma)
                '-' -> append('-')
                else -> append((arabicZero + (ch - '0')).toChar())
            }
        }
    }
}

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

    val intStr = formatIntegerPart(intPart, useGrouping, isSpaceGrouping, isCommaDecimal)
    val sign = if (isNegative) "-" else ""
    val decimalSep = if (isCommaDecimal) "," else "."
    val raw = if (fracStr.isEmpty()) "$sign$intStr" else "$sign$intStr$decimalSep$fracStr"

    return if (isArabic) toArabicDigits(raw) else raw
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
