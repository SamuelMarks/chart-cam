/**
 * @file DispatcherProvider.kt
 * Provides a Kotlin Multiplatform abstraction for coroutine dispatchers to enable
 * deterministic testing and structured concurrency across all targets.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over [CoroutineDispatcher]s used across ViewModels, repositories, and services.
 * Injecting [DispatcherProvider] instead of hardcoding [Dispatchers] enables deterministic
 * unit testing in `commonTest` using `StandardTestDispatcher`.
 */
interface DispatcherProvider {
    /**
     * Dispatcher confined to the main UI thread.
     */
    val main: CoroutineDispatcher

    /**
     * Dispatcher optimized for CPU-intensive background work.
     */
    val default: CoroutineDispatcher

    /**
     * Dispatcher optimized for disk or network I/O operations.
     */
    val io: CoroutineDispatcher
}

/**
 * Default production implementation of [DispatcherProvider] backed by standard [Dispatchers].
 */
object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher
        get() = Dispatchers.Main

    override val default: CoroutineDispatcher
        get() = Dispatchers.Default

    override val io: CoroutineDispatcher
        get() = Dispatchers.Default
}
