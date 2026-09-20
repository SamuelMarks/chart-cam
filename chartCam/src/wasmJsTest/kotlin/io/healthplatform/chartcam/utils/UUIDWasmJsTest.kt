/**
 * @file UUIDWasmJsTest.kt
 * Contains declarations for UUIDWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for UUID on WasmJS.
 */
class UUIDWasmJsTest {
    /**
     * Verifies UUID version 4 formatting and uniqueness on WasmJS.
     */
    @Test
    fun testUUIDWasmJs() {
        val u1 = UUID.randomUUID()
        val u2 = UUID.randomUUID()
        assertNotNull(u1)
        assertNotNull(u2)
        assertNotEquals(u1, u2)
        assertEquals(36, u1.length)
        assertEquals('-', u1[8])
        assertEquals('-', u1[13])
        assertEquals('-', u1[18])
        assertEquals('-', u1[23])
        assertEquals('4', u1[14])
        assertTrue(u1[19] in listOf('8', '9', 'a', 'b'))
    }
}
