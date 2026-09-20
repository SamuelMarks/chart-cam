/**
 * @file UUIDIosTest.kt
 * Contains declarations for UUIDIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Test class for UUID on iOS.
 */
class UUIDIosTest {
    /**
     * Verifies that UUID.randomUUID creates valid, format-compliant and unique strings on iOS.
     */
    @Test
    fun testRandomUUIDOnIos() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        assertEquals(36, uuid1.length)
        assertEquals(36, uuid2.length)
        assertEquals('-', uuid1[8])
        assertEquals('-', uuid1[13])
        assertEquals('-', uuid1[18])
        assertEquals('-', uuid1[23])
        assertNotEquals(uuid1, uuid2, "Consecutive UUIDs should not match")
    }
}
