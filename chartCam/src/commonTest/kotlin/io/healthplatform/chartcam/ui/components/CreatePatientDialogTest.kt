/**
 * @file CreatePatientDialogTest.kt
 * Contains declarations for CreatePatientDialogTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
     * Verifies that [parseFlexibleDate] falls back gracefully when month exceeds 12 in US format.
     */
    @Test
    fun testParseFlexibleDateFallbackOnInvalidMonth() {
        val parsed = parseFlexibleDate("25/01/1985", "en-US")
        assertEquals(LocalDate(1985, 1, 25), parsed)
    }
}
