/**
 * @file TestInstantTest.kt
 * Contains declarations for TestInstantTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for test instant functionality.
 */
class TestInstantTest {
    /**
     * Test the [test] instant method with defined epoch millis.
     */
    @Test
    fun testTestInstant() {
        val millis = 1600000000000L
        test(millis)
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
        assertEquals(1600000000000L, instant.toEpochMilliseconds())
    }
}
