/**
 * @file CreatePatientDialogTest.kt
 * Contains declarations for CreatePatientDialogTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Common test for [CreatePatientDialog] and flexible date parsing.
 */
class CreatePatientDialogTest {
    /**
     * Verifies that [parseFlexibleDate] parses ISO-8601 format correctly.
     */
    @Test
    fun testParseFlexibleDateIso() {
        val parsed = parseFlexibleDate("1990-01-01")
        assertEquals(LocalDate(1990, 1, 1), parsed)
    }

    /**
     * Verifies that [parseFlexibleDate] parses US date formats correctly for English locale.
     */
    @Test
    fun testParseFlexibleDateUsFormat() {
        val parsed = parseFlexibleDate("05/15/1985", "en")
        assertEquals(LocalDate(1985, 5, 15), parsed)
    }

    /**
     * Verifies that [parseFlexibleDate] parses international DD/MM/YYYY for non-English locales.
     */
    @Test
    fun testParseFlexibleDateInternationalFormat() {
        val parsedEs = parseFlexibleDate("15/05/1985", "es")
        assertEquals(LocalDate(1985, 5, 15), parsedEs)

        val parsedHe = parseFlexibleDate("15.05.1985", "he")
        assertEquals(LocalDate(1985, 5, 15), parsedHe)
    }

    /**
     * Verifies that [parseFlexibleDate] parses East Asian YYYY/MM/DD correctly.
     */
    @Test
    fun testParseFlexibleDateEastAsianFormat() {
        val parsedZh = parseFlexibleDate("1985/05/15", "zh")
        assertEquals(LocalDate(1985, 5, 15), parsedZh)

        val parsedJa = parseFlexibleDate("1985.05.15", "ja")
        assertEquals(LocalDate(1985, 5, 15), parsedJa)
    }

    /**
     * Verifies that [parseFlexibleDate] handles invalid inputs gracefully.
     */
    @Test
    fun testParseFlexibleDateInvalid() {
        assertNull(parseFlexibleDate(""))
        assertNull(parseFlexibleDate("   "))
        assertNull(parseFlexibleDate("not-a-date"))
        assertNull(parseFlexibleDate("2020-99-99"))
        assertNull(parseFlexibleDate("01/02"))
        assertNull(parseFlexibleDate("01/02/2020/05"))
    }

    /**
     * Verifies that [parseFlexibleDate] parses British/Commonwealth English DD/MM/YYYY formats.
     */
    @Test
    fun testParseFlexibleDateCommonwealthEnglish() {
        val parsedGb = parseFlexibleDate("15/05/1985", "en-GB")
        assertEquals(LocalDate(1985, 5, 15), parsedGb)

        val parsedAu = parseFlexibleDate("15/05/1985", "en-AU")
        assertEquals(LocalDate(1985, 5, 15), parsedAu)
    }

    /**
     * Verifies that [parseFlexibleDate] parses Day-first patterns and falls back correctly.
     */
    @Test
    fun testParseFlexibleDateDayFirstFallback() {
        // In es (Day First), "05/25/1985" has 25 as second param, should fallback to month=5, day=25
        val parsedEs = parseFlexibleDate("05/25/1985", "es")
        assertEquals(LocalDate(1985, 5, 25), parsedEs)

        // In en (Month First), "25/05/1985" has 25 as first param, should fallback to month=5, day=25
        val parsedEn = parseFlexibleDate("25/05/1985", "en")
        assertEquals(LocalDate(1985, 5, 25), parsedEn)

        // Invalid year in year-first pattern
        val parsedInvalidYearZh = parseFlexibleDate("999/05/25", "zh")
        assertNull(parsedInvalidYearZh)
    }

    /**
     * Verifies [onDatePickerConfirm] with both a selected date and null fallback.
     */
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test
    fun testOnDatePickerConfirm() {
        val state =
            androidx.compose.material3.DatePickerState(
                initialSelectedDateMillis = 1700000000000L,
                locale = io.healthplatform.chartcam.createTestCalendarLocale("en"),
            )
        val formatted = onDatePickerConfirm(state, "en")
        assertTrue(formatted.isNotBlank())

        val nullState =
            androidx.compose.material3.DatePickerState(
                initialSelectedDateMillis = null,
                locale = io.healthplatform.chartcam.createTestCalendarLocale("en"),
            )
        val fallbackFormatted = onDatePickerConfirm(nullState, "zh")
        assertTrue(fallbackFormatted.isNotBlank())
    }

    /**
     * Verifies that [parseFlexibleDate] parses 2-digit years correctly (adding century base 2000).
     */
    @Test
    fun testParseFlexibleDateTwoDigitYear() {
        val parsedEn = parseFlexibleDate("05/15/85", "en")
        assertEquals(LocalDate(2085, 5, 15), parsedEn)

        val parsedZh2 = parseFlexibleDate("85/05/15", "zh")
        assertEquals(LocalDate(2085, 5, 15), parsedZh2)

        val parsedZh4 = parseFlexibleDate("1985/05/15", "zh")
        assertEquals(LocalDate(1985, 5, 15), parsedZh4)

        // 3-digit year should fail
        val parsed3Digit = parseFlexibleDate("05/15/985", "en")
        assertNull(parsed3Digit)

        val parsed3DigitEnd = parseFlexibleDate("15/05/985", "es")
        assertNull(parsed3DigitEnd)
    }
}
