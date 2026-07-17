/**
 * @file TestInstantTest.kt
 * Contains declarations for TestInstantTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

class TestInstantTest {
    @Test
    fun testTestInstant() {
        // Doesn't return anything, just ensures it executes without crash
        test(1600000000000L)
        assertTrue(true)
    }
}
