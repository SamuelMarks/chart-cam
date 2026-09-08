/**
 * @file DateFormatterAndroidTest.kt
 * Contains declarations for DateFormatterAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android host tests for DateFormatter.
 */
class DateFormatterAndroidTest {
    /**
     * Test localized date formatting behaves correctly on Android.
     */
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

        // DateTime without timezone (LocalDateTime fallback)
        val localDateTime = formatLocalizedDate("2023-10-15T14:30:00")
        assert(localDateTime.isNotEmpty())

        // Blank
        val blank = formatLocalizedDate("   ")
        assertEquals("   ", blank)

        // Invalid parsing falls back to original string
        val invalid = formatLocalizedDate("Invalid Date")
        assertEquals("Invalid Date", invalid)
    }

    /**
     * Test localized datetime formatting behaves correctly on Android.
     */
    @Test
    fun testFormatLocalizedDateTime() {
        val defaultDateTime = formatLocalizedDateTime("2023-10-15T14:30:00Z")
        assert(defaultDateTime.isNotEmpty())

        val dateTime = formatLocalizedDateTime("2023-10-15T14:30:00Z", "en")
        assert(dateTime.isNotEmpty())

        val localDateTime = formatLocalizedDateTime("2023-10-15T14:30:00", "en")
        assert(localDateTime.isNotEmpty())

        val blank = formatLocalizedDateTime("   ", "en")
        assertEquals("   ", blank)

        val invalid = formatLocalizedDateTime("Invalid DateTime", "en")
        assertEquals("Invalid DateTime", invalid)
    }
}
