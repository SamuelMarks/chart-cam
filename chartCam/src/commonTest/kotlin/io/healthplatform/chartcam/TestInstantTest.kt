/**
 * @file TestInstantTest.kt
 * Contains declarations for TestInstantTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for test instant functionality.
 */
class TestInstantTest {
    /**
     * Test the [test] instant method.
     */
    @Test
    fun testTestInstant() {
        // Doesn't return anything, just ensures it executes without crash
        test(1600000000000L)
        assertTrue(true)
    }
}
