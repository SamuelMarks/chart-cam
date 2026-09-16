/**
 * @file EmptyJsTest.kt
 * Contains declarations for EmptyJsTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Basic JS sanity checks.
 */
class EmptyJsTest {
    /**
     * Test string trimming behavior in JS.
     */
    @Test
    fun testEmptyJs() {
        val str = "  js_test  "
        assertEquals("js_test", str.trim())
    }
}
