/**
 * @file UUIDIosTest.kt
 * Contains declarations for UUIDIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test class for UUID on iOS.
 */
class UUIDIosTest {
    /**
     * Verifies that UUID.randomUUID creates valid and unique strings on iOS.
     */
    @Test
    fun testRandomUUIDOnIos() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        assertTrue(uuid1.isNotEmpty(), "UUID string should not be empty")
        assertTrue(uuid2.isNotEmpty(), "UUID string should not be empty")
        assertNotEquals(uuid1, uuid2, "Consecutive UUIDs should not match")
    }
}
