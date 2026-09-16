/**
 * @file UUIDAndroidTest.kt
 * Contains declarations for UUIDAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Android host tests for UUID.
 */
class UUIDAndroidTest {
    /**
     * Verifies UUID generation and format on Android.
     */
    @Test
    fun testUUIDAndroid() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals(id1, id2)
        assertEquals(36, id1.length)
    }
}
