/**
 * @file ResultExtensionsTest.kt
 * Unit tests verifying behavior of coroutine-safe Result extension utilities.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Validates [runSuspendCatching] behaves correctly under success, standard failure, and cancellation.
 */
class ResultExtensionsTest {
    /**
     * Verifies that successful blocks return Result.success.
     */
    @Test
    fun testRunSuspendCatchingSuccess() =
        runTest {
            val result = runSuspendCatching { 42 }
            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrNull())
        }

    /**
     * Verifies that standard exceptions are captured as Result.failure.
     */
    @Test
    fun testRunSuspendCatchingFailure() =
        runTest {
            val result =
                runSuspendCatching {
                    error("Operation failed")
                }
            assertTrue(result.isFailure)
            assertEquals("Operation failed", result.exceptionOrNull()?.message)
        }

    /**
     * Verifies that CancellationException is re-thrown and not swallowed.
     */
    @Test
    fun testRunSuspendCatchingRethrowsCancellation() =
        runTest {
            assertFailsWith<CancellationException> {
                runSuspendCatching {
                    throw CancellationException("Cancelled") // allow-exception
                }
            }
        }
}
