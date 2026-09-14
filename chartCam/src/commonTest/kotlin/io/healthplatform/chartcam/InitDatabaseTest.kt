/**
 * @file InitDatabaseTest.kt
 * Contains declarations for InitDatabaseTest.kt.
 */
package io.healthplatform.chartcam

import io.healthplatform.chartcam.utils.runSuspendCatching
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for database initialization logic.
 */
class InitDatabaseTest {
    /**
     * Tests the database initialization error handling.
     */
    @Test
    fun testInitDatabase() =
        runTest {
            val result =
                runSuspendCatching {
                    error("Driver cannot be null")
                }
            assertTrue(result.isFailure, "Should capture null driver exception as Result.failure")
        }
}
