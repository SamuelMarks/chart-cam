/**
 * @file UUIDTest.kt
 * Contains declarations for UUIDTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for the UUID generation logic.
 */
class UUIDTest {
    /**
     * Verifies that the random UUID generated is not empty.
     */
    @Test
    fun testRandomUUID() {
        val uuid = UUID.randomUUID()
        assertTrue(uuid.isNotEmpty())
    }
}
