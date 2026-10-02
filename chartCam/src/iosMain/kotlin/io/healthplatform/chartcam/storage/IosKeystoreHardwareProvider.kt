/**
 * @file IosKeystoreHardwareProvider.kt
 * Contains declarations for IosKeystoreHardwareProvider.kt.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.healthplatform.chartcam.storage

import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/**
 * iOS implementation of [KeystoreHardwareProvider] backed by the Apple Secure Enclave.
 */
class IosKeystoreHardwareProvider : KeystoreHardwareProvider {
    /**
     * Checks whether biometric hardware is available and supported.
     *
     * @return A [Result] indicating success if hardware backed, or failure.
     */
    override fun checkHardwareBacked(): Result<Unit> =
        runCatching {
            val context = LAContext()
            val canAuth = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
            if (!canAuth) {
                error("Secure Enclave biometrics not available on this device")
            }
        }

    /**
     * Retrieves the current biometric hardware status.
     *
     * @return The [BiometricHardwareStatus].
     */
    override fun getHardwareStatus(): BiometricHardwareStatus {
        val context = LAContext()
        val canAuth = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
        return if (canAuth) BiometricHardwareStatus.AVAILABLE else BiometricHardwareStatus.NOT_ENROLLED
    }

    /**
     * Prompts the iOS user for FaceID or TouchID evaluation.
     *
     * @param title The dialog title.
     * @param subtitle The dialog subtitle / localized reason.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun promptBiometrics(
        title: String,
        subtitle: String,
    ): Result<BiometricAuthResult> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            kotlin.coroutines.suspendCoroutine { cont ->
                val context = LAContext()
                val reason = subtitle.ifEmpty { title }
                context.evaluatePolicy(
                    LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                    localizedReason = reason,
                ) { success, error ->
                    if (success) {
                        cont.resume(Result.success(BiometricAuthResult.Success))
                    } else {
                        val authResult =
                            when (error?.code) {
                                LAErrorUserCancel,
                                LAErrorUserFallback,
                                -> BiometricAuthResult.FallbackToPassword
                                LAErrorBiometryLockout -> BiometricAuthResult.TemporarilyLockedOut(30)
                                LAErrorBiometryNotAvailable,
                                LAErrorBiometryNotEnrolled,
                                -> BiometricAuthResult.HardwareError(error.localizedDescription)
                                LAErrorAuthenticationFailed -> BiometricAuthResult.Failed(1)
                                else -> BiometricAuthResult.HardwareError(error?.localizedDescription ?: "Unknown error")
                            }
                        cont.resume(Result.success(authResult))
                    }
                }
            }
        }
}

/**
 * Creates an [IosKeystoreHardwareProvider] instance.
 *
 * @return The iOS keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = IosKeystoreHardwareProvider()
