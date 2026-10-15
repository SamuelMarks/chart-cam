/**
 * @file TestInstantTest.kt
 * Contains unit tests verifying epoch millisecond to UTC LocalDate conversion.
 */
package io.healthplatform.chartcam

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for instant to UTC [LocalDate] conversion.
 */
class TestInstantTest {
    /**
     * Verifies converting epoch milliseconds to a UTC [LocalDate].
     */
    @Test
    fun testTestInstant() {
        val millis = 1600000000000L
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
        val date = instant.toLocalDateTime(TimeZone.UTC).date
        assertEquals(LocalDate(2020, 9, 13), date)
        assertEquals(1600000000000L, instant.toEpochMilliseconds())
    }
}
