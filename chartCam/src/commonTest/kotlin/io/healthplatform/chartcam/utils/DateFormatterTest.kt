/**
 * @file DateFormatterTest.kt
 * Contains tests for localized date and time formatting.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [formatLocalizedDate] to ensure proper localized parsing of FHIR dates.
 */
class DateFormatterTest {
    /**
     * Verifies that formatting a raw FHIR date string returns a non-empty,
     * reasonably formatted localized string (as the exact format depends on the test runner's locale).
     */
    @Test
    fun testFormatLocalizedDate() {
        val fhirDate = "1990-01-01T10:00:00Z"
        val formatted = formatLocalizedDate(fhirDate)

        // Since we don't strictly know the environment's locale (could be US, UK, etc.),
        // we assert that it correctly processed the date without crashing and returned something meaningful.
        assertTrue(formatted.isNotEmpty(), "Formatted date should not be empty")

        // In most locales, it will contain the year 1990.
        assertTrue(formatted.contains("1990"), "Formatted date should contain the parsed year '1990'")
    }

    /**
     * Verifies getLocalizedDatePattern for all language branches and default argument.
     */
    @Test
    fun testGetLocalizedDatePattern() {
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("ja"))
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("zh"))
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("zh-TW"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("es"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("he"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("es-ES"))
        assertEquals("YYYY-MM-DD", getLocalizedDatePattern("en"))
        assertEquals("YYYY-MM-DD", getLocalizedDatePattern("fr"))
        assertTrue(getLocalizedDatePattern().isNotEmpty())
    }

    /**
     * Verifies getLocalizedDateTimePattern for all language branches and default argument.
     */
    @Test
    fun testGetLocalizedDateTimePattern() {
        assertEquals("YYYY/MM/DD HH:MM", getLocalizedDateTimePattern("ja"))
        assertEquals("YYYY/MM/DD HH:MM", getLocalizedDateTimePattern("zh"))
        assertEquals("DD/MM/YYYY HH:MM", getLocalizedDateTimePattern("es"))
        assertEquals("DD/MM/YYYY HH:MM", getLocalizedDateTimePattern("he"))
        assertEquals("YYYY-MM-DD HH:MM", getLocalizedDateTimePattern("en"))
        assertTrue(getLocalizedDateTimePattern().isNotEmpty())
    }
}
