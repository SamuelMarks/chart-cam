/**
 * @file AndroidDateFormatterTest.kt
 * Contains declarations for AndroidDateFormatterTest.kt.
 */
package io.healthplatform.chartcam.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidDateFormatterTest {
    @Test
    fun testFormatLocalizedDate() {
        val fhirDate = "2023-01-01"
        val formatted = formatLocalizedDate(fhirDate)
        assertNotEquals(fhirDate, formatted) // Assuming MEDIUM format changes it from raw

        val fhirDateTime = "2023-01-01T12:00:00Z"
        val formattedTime = formatLocalizedDate(fhirDateTime)
        assertNotEquals(fhirDateTime, formattedTime)

        val invalid = "not-a-date"
        assertEquals(invalid, formatLocalizedDate(invalid))
    }
}
