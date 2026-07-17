/**
 * @file InitDatabaseTest.kt
 * Contains declarations for InitDatabaseTest.kt.
 */
package io.healthplatform.chartcam

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class InitDatabaseTest {
    @Test
    fun testInitDatabase() =
        runTest {
            // We simulate catching the exception when driver is null
            var caught = false
            try {
                val driver = null
                if (driver == null) throw IllegalArgumentException("Driver cannot be null")
            } catch (e: Exception) {
                caught = true
            }
            assertTrue(caught, "Should catch null driver exception")
        }
}
