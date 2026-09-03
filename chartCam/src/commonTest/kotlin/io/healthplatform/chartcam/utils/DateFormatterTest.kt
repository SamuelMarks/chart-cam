/**
 * @file DateFormatterTest.kt
 * Contains tests for localized date and time formatting.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
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
}
