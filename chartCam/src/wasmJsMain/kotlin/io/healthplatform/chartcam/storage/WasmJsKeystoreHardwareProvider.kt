/**
 * @file WasmJsKeystoreHardwareProvider.kt
 * Contains declarations for WasmJsKeystoreHardwareProvider.kt.
 */
package io.healthplatform.chartcam.storage

/**
 * WasmJS implementation of [KeystoreHardwareProvider].
 */
class WasmJsKeystoreHardwareProvider : KeystoreHardwareProvider {
    /**
     * Web platform does not provide direct hardware KeyStore access.
     *
     * @return A [Result] failure indicating hardware KeyStore is unavailable on Web.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Hardware KeyStore is not supported on WebAssembly"))

    /**
     * Retrieves the current biometric hardware status on WasmJS Web.
     *
     * @return [BiometricHardwareStatus.NO_HARDWARE] on WebAssembly runtimes.
     */
    override fun getHardwareStatus(): BiometricHardwareStatus = BiometricHardwareStatus.NO_HARDWARE
}

/**
 * Creates a [WasmJsKeystoreHardwareProvider] instance.
 *
 * @return The WasmJS keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = WasmJsKeystoreHardwareProvider()
