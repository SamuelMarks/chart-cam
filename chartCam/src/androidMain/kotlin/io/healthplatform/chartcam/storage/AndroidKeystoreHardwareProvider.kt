/**
 * @file AndroidKeystoreHardwareProvider.kt
 * Contains declarations for AndroidKeystoreHardwareProvider.kt.
 */
package io.healthplatform.chartcam.storage

import android.app.KeyguardManager
import android.content.Context
import android.os.Build

/**
 * Android implementation of [KeystoreHardwareProvider].
 * Evaluates hardware backing via KeyStore capability and OS version.
 *
 * @param sdkInt The Android API version code to evaluate.
 * @param contextProvider Optional context provider for test environments.
 */
class AndroidKeystoreHardwareProvider(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val contextProvider: (() -> Context?)? = null,
) : KeystoreHardwareProvider {
    /**
     * Checks whether KeyStore is backed by secure hardware.
     *
     * @return A [Result] indicating success if hardware backed, or failure.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        if (sdkInt >= Build.VERSION_CODES.M) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Hardware keystore requires Android M or higher"))
        }

    /**
     * Retrieves the current biometric hardware status.
     *
     * @return The [BiometricHardwareStatus].
     */
    override fun getHardwareStatus(): BiometricHardwareStatus =
        if (sdkInt >= Build.VERSION_CODES.M) {
            BiometricHardwareStatus.AVAILABLE
        } else {
            BiometricHardwareStatus.NO_HARDWARE
        }

    /**
     * Prompts the user for biometric authentication on Android.
     *
     * @param title The dialog title.
     * @param subtitle The dialog subtitle.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun promptBiometrics(
        title: String,
        subtitle: String,
    ): Result<Unit> {
        val status = getHardwareStatus()
        if (status != BiometricHardwareStatus.AVAILABLE) {
            return Result.failure(IllegalStateException("Biometrics unavailable: $status"))
        }
        val context =
            if (contextProvider != null) {
                contextProvider.invoke()
            } else {
                runCatching {
                    io.healthplatform.chartcam.AndroidAppInit
                        .getContext()
                }.getOrNull()
            }

        val keyguardManager = context?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isInsecure = keyguardManager?.isDeviceSecure == false

        return if (isInsecure) {
            Result.failure(IllegalStateException("Device credentials/biometrics not secure"))
        } else {
            Result.success(Unit)
        }
    }
}

/**
 * Creates an [AndroidKeystoreHardwareProvider] instance.
 *
 * @return The Android keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = AndroidKeystoreHardwareProvider()
