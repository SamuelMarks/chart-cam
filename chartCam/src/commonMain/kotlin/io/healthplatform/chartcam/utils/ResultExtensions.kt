/**
 * @file ResultExtensions.kt
 * Utility extension functions and builders for Coroutine-safe [Result] handling.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.CancellationException

/**
 * Executes the given suspending [block] and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it
 * as a failure.
 *
 * Re-throws [CancellationException] immediately so that coroutine cancellation and structured concurrency
 * are not broken.
 *
 * @param T The return type of the encapsulated computation.
 * @param block The suspending lambda operation to execute safely.
 * @return A [Result] containing either the successful value of type [T] or the encapsulated exception.
 */
@Suppress("ForbiddenException", "TooGenericExceptionCaught")
inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
