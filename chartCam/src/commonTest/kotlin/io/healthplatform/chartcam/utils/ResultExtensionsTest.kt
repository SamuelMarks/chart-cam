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

    /**
     * Verifies that flatMap applies the transform on success.
     */
    @Test
    fun testFlatMapSuccess() {
        val initial = Result.success(10)
        val transformed = initial.flatMap { Result.success(it * 2) }
        assertTrue(transformed.isSuccess)
        assertEquals(20, transformed.getOrNull())
    }

    /**
     * Verifies that flatMap propagates failure from the initial Result.
     */
    @Test
    fun testFlatMapPropagatesInitialFailure() {
        val initial: Result<Int> = Result.failure(IllegalStateException("Initial error"))
        val transformed = initial.flatMap { Result.success(it * 2) }
        assertTrue(transformed.isFailure)
        assertEquals("Initial error", transformed.exceptionOrNull()?.message)
    }

    /**
     * Verifies that flatMap propagates failure from the transform function.
     */
    @Test
    fun testFlatMapPropagatesTransformFailure() {
        val initial = Result.success(10)
        val transformed = initial.flatMap { Result.failure<Int>(IllegalStateException("Transform error")) }
        assertTrue(transformed.isFailure)
        assertEquals("Transform error", transformed.exceptionOrNull()?.message)
    }
}
