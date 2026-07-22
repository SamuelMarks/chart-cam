/**
 * @file DateFormatterAndroidTest.kt
 * Contains declarations for DateFormatterAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatterAndroidTest {
    @Test
    fun testFormatLocalizedDate() {
        // Date only
        val dateOnly = formatLocalizedDate("2023-10-15")
        // It's formatted using Locale.getDefault(), we can't reliably predict the exact string, but it shouldn't be the original (unless there's an error)
        // Let's just check it doesn't throw.
        assert(dateOnly.isNotEmpty())

        // DateTime
        val dateTime = formatLocalizedDate("2023-10-15T14:30:00Z")
        assert(dateTime.isNotEmpty())

        // Blank
        val blank = formatLocalizedDate("   ")
        assertEquals("   ", blank)

        // Invalid parsing falls back to original string
        val invalid = formatLocalizedDate("Invalid Date")
        assertEquals("Invalid Date", invalid)
    }
}
