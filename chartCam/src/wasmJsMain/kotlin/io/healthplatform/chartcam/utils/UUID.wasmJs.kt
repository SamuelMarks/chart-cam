/**
 * @file UUID.wasmJs.kt
 * @file UUID.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation for generating Universally Unique Identifiers (UUIDs).
 */
package io.healthplatform.chartcam.utils

/**
 * A utility object for generating UUIDs on the WebAssembly platform.
 */
actual object UUID {
    /**
     * Generates a random UUID string.
     * Note: This is currently a stub implementation returning a placeholder string.
     *
     * @return A string representing a UUID (e.g., "js-uuid-placeholder-0000").
     */
    actual fun randomUUID(): String = "js-uuid-placeholder-0000"
}
