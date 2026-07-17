/**
 * @file DateFormatterJvmTest.kt
 * Contains declarations for DateFormatterJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatterJvmTest {
    private val defaultLocale = Locale.getDefault()

    @BeforeTest
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @AfterTest
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun testFormatLocalizedDate_isBlank() {
        assertEquals("", formatLocalizedDate(""))
        assertEquals("   ", formatLocalizedDate("   "))
    }

    @Test
    fun testFormatLocalizedDate_withTime() {
        val fhirDate = "2026-07-09T10:00:00Z"
        // In US locale, FormatStyle.MEDIUM outputs something like "Jul 9, 2026, 10:00:00 AM" or "Jul 9, 2026, 10:00:00\u202fAM" depending on JDK version.
        // We'll just verify it doesn't throw and parses properly.
        val formatted = formatLocalizedDate(fhirDate)
        assert(formatted.contains("2026"))
        assert(formatted.contains("Jul"))
        assert(formatted.contains("9"))
    }

    @Test
    fun testFormatLocalizedDate_dateOnly() {
        val fhirDate = "2026-07-09"
        val formatted = formatLocalizedDate(fhirDate)
        assertEquals("Jul 9, 2026", formatted)
    }

    @Test
    fun testFormatLocalizedDate_exceptionFallback() {
        val invalidDate = "Invalid-Date-String"
        assertEquals("Invalid-Date-String", formatLocalizedDate(invalidDate))
    }
}
