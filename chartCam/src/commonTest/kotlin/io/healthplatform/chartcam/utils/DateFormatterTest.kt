/**
 * @file DateFormatterTest.kt
 * Contains declarations for DateFormatterTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

class DateFormatterTest {
    @Test
    fun testFormatLocalizedDate() {
        val date = "1990-01-01T10:00:00Z"
        val formatted = formatLocalizedDate(date)
        assertNotNull(formatted)
    }
}
