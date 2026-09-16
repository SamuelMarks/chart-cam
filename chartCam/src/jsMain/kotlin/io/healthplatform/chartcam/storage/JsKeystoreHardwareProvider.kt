/**
 * @file JsKeystoreHardwareProvider.kt
 * Contains declarations for JsKeystoreHardwareProvider.kt.
 */
package io.healthplatform.chartcam.storage

/**
 * JS implementation of [KeystoreHardwareProvider].
 */
class JsKeystoreHardwareProvider : KeystoreHardwareProvider {
    /**
     * Web platform does not provide direct hardware KeyStore access.
     *
     * @return A [Result] failure indicating hardware KeyStore is unavailable on Web.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Hardware KeyStore is not supported on Web"))

    /**
     * Retrieves the current biometric hardware status on JS Web.
     *
     * @return [BiometricHardwareStatus.NO_HARDWARE] on web browser runtimes.
     */
    override fun getHardwareStatus(): BiometricHardwareStatus = BiometricHardwareStatus.NO_HARDWARE
}

/**
 * Creates a [JsKeystoreHardwareProvider] instance.
 *
 * @return The JS keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = JsKeystoreHardwareProvider()
