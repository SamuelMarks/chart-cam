/**
 * @file WebKeystoreHardwareProvider.kt
 * Shared Web (JS and WasmJS) implementation of [KeystoreHardwareProvider].
 */
package io.healthplatform.chartcam.storage

/**
 * Shared Web implementation of [KeystoreHardwareProvider] for JS and WasmJS targets.
 */
open class WebKeystoreHardwareProvider : KeystoreHardwareProvider {
    /**
     * Web platforms do not provide direct hardware KeyStore access.
     *
     * @return A [Result] failure indicating hardware KeyStore is unavailable on Web.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Hardware KeyStore is not supported on Web"))

    /**
     * Retrieves the current biometric hardware status on Web runtimes.
     *
     * @return [BiometricHardwareStatus.NO_HARDWARE] on web browser runtimes.
     */
    override fun getHardwareStatus(): BiometricHardwareStatus = BiometricHardwareStatus.NO_HARDWARE
}

/**
 * Typealias preserving source compatibility for JS callers referencing [JsKeystoreHardwareProvider].
 */
typealias JsKeystoreHardwareProvider = WebKeystoreHardwareProvider

/**
 * Typealias preserving source compatibility for WasmJS callers referencing [WasmJsKeystoreHardwareProvider].
 */
typealias WasmJsKeystoreHardwareProvider = WebKeystoreHardwareProvider

/**
 * Creates a [WebKeystoreHardwareProvider] instance for JS and WasmJS targets.
 *
 * @return The Web keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = WebKeystoreHardwareProvider()
