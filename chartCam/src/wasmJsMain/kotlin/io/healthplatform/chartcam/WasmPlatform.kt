/**
 * @file Platform.wasmJs.kt
 * @file Platform.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of the [Platform] interface.
 */
package io.healthplatform.chartcam

/**
 * Represents the WebAssembly platform running in a web browser context.
 */
class WasmPlatform : Platform {
    /**
     * The human-readable name of the platform.
     */
    override val name: String = "Web with Kotlin/Wasm"
}

/**
 * Retrieves the current [Platform] instance for the WebAssembly target.
 *
 * @return An instance of [WasmPlatform].
 */
actual fun getPlatform(): Platform = WasmPlatform()
