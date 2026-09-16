/**
 * @file DateFormatterJsTest.kt
 * Contains declarations for DateFormatterJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for DateFormatter on JS.
 */
class DateFormatterJsTest {
    /**
     * Test date formatting on JS.
     */
    @Test
    fun testDateFormatterJs() {
        val formatted = formatLocalizedDate("2026-09-16")
        assertNotNull(formatted)
        assertTrue(formatted.isNotBlank())
    }
}
