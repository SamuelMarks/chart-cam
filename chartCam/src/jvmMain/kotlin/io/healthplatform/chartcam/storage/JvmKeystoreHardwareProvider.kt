/**
 * @file JvmKeystoreHardwareProvider.kt
 * Contains declarations for JvmKeystoreHardwareProvider.kt.
 */
package io.healthplatform.chartcam.storage

/**
 * Desktop JVM implementation of [KeystoreHardwareProvider].
 */
class JvmKeystoreHardwareProvider : KeystoreHardwareProvider {
    /**
     * Desktop JVM does not have an integrated hardware KeyStore or Secure Enclave.
     *
     * @return A [Result] indicating failure on desktop JVM environments.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Hardware KeyStore is not supported on desktop JVM"))

    /**
     * Retrieves the current biometric hardware status on Desktop JVM.
     *
     * @return [BiometricHardwareStatus.NO_HARDWARE] on standard desktop runtimes.
     */
    override fun getHardwareStatus(): BiometricHardwareStatus = BiometricHardwareStatus.NO_HARDWARE
}

/**
 * Creates a [JvmKeystoreHardwareProvider] instance.
 *
 * @return The JVM keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = JvmKeystoreHardwareProvider()
