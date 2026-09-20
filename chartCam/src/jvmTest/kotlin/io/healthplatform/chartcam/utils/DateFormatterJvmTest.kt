/**
 * @file DateFormatterJvmTest.kt
 * Tests verifying unified multiplatform DateFormatter behavior on the JVM.
 */
package io.healthplatform.chartcam.utils

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for DateFormatter on JVM.
 */
class DateFormatterJvmTest {
    private val defaultLocale = Locale.getDefault()

    /**
     * Sets up the test environment.
     */
    @BeforeTest
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    /**
     * Tears down the test environment.
     */
    @AfterTest
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    /**
     * Tests formatLocalizedDate with blank input.
     */
    @Test
    fun testFormatLocalizedDate_isBlank() {
        assertEquals("", formatLocalizedDate(""))
        assertEquals("", formatLocalizedDate("   "))
    }

    /**
     * Tests formatLocalizedDate with time.
     */
    @Test
    fun testFormatLocalizedDate_withTime() {
        val fhirDate = "2026-07-09T10:00:00Z"
        val formatted = formatLocalizedDate(fhirDate, "en")
        assertEquals("07/09/2026 10:00", formatted)
    }

    /**
     * Tests formatLocalizedDate with date only.
     */
    @Test
    fun testFormatLocalizedDate_dateOnly() {
        val fhirDate = "2026-07-09"
        val formatted = formatLocalizedDate(fhirDate, "en")
        assertEquals("07/09/2026", formatted)
    }

    /**
     * Tests formatLocalizedDate with partial dates.
     */
    @Test
    fun testFormatLocalizedDate_partialDates() {
        val yearOnly = formatLocalizedDate("1985", "en")
        assertEquals("1985", yearOnly)

        val yearMonth = formatLocalizedDate("1985-03", "en")
        assertEquals("03/1985", yearMonth)
    }

    /**
     * Tests formatLocalizedDate exception fallback.
     */
    @Test
    fun testFormatLocalizedDate_exceptionFallback() {
        val invalidDate = "Invalid-Date-String"
        assertEquals("Invalid-Date-String", formatLocalizedDate(invalidDate))
    }

    /**
     * Tests formatLocalizedDateTime with valid and invalid inputs on JVM.
     */
    @Test
    fun testFormatLocalizedDateTime_jvm() {
        assertEquals("", formatLocalizedDateTime(""))
        val fhirDate = "2026-07-09T10:00:00Z"
        val formatted = formatLocalizedDateTime(fhirDate, "en")
        assertEquals("07/09/2026 10:00", formatted)

        val invalidDate = "Invalid-DateTime-String"
        assertEquals("Invalid-DateTime-String", formatLocalizedDateTime(invalidDate, "en"))
    }
}
