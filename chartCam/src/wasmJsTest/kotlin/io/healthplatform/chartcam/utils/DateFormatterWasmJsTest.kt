/**
 * @file DateFormatterWasmJsTest.kt
 * Contains declarations for DateFormatterWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for DateFormatter on WasmJS.
 */
class DateFormatterWasmJsTest {
    /**
     * Test date formatter on WasmJS does not produce object Object string pointers.
     */
    @Test
    fun testDateFormatterWasmJs() {
        val formattedDate = formatLocalizedDate("2023-10-25", "en")
        assertFalse(formattedDate.contains("[object Object]"), "Formatted date must not contain [object Object]")
        assertTrue(formattedDate.isNotBlank())

        val formattedDateTime = formatLocalizedDateTime("2023-10-25T14:30:00Z", "en")
        assertFalse(formattedDateTime.contains("[object Object]"), "Formatted datetime must not contain [object Object]")
        assertTrue(formattedDateTime.isNotBlank())

        assertEquals("", formatLocalizedDate("", "en"))
        assertEquals("", formatLocalizedDateTime("", "en"))

        // Multiple languages
        for (lang in listOf("en", "es", "ja", "he")) {
            val res = formatLocalizedDate("2024-06-15", lang)
            assertFalse(res.contains("[object Object]"))
            assertTrue(res.isNotBlank())
        }
    }
}
