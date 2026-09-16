/**
 * @file DateFormatterIosTest.kt
 * Contains declarations for DateFormatterIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verification for date formatting behaviors on iOS.
 */
class DateFormatterIosTest {
    /** Verifies formatLocalizedDate formatting on iOS. */
    @Test
    fun testDateFormatterIos() {
        val formatted = formatLocalizedDate("1990-05-20")
        assertNotNull(formatted)
        assertTrue(formatted.isNotBlank())
    }
}
