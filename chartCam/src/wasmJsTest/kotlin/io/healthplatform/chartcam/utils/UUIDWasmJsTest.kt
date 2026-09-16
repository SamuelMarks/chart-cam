/**
 * @file UUIDWasmJsTest.kt
 * Contains declarations for UUIDWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Test class for UUID on WasmJS.
 */
class UUIDWasmJsTest {
    /**
     * Test UUID generation and uniqueness on WasmJS.
     */
    @Test
    fun testUUIDWasmJs() {
        val u1 = UUID.randomUUID()
        val u2 = UUID.randomUUID()
        assertNotNull(u1)
        assertNotNull(u2)
        assertNotEquals(u1, u2)
        assertEquals(36, u1.length)
    }
}
