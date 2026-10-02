/**
 * @file AndroidKeystoreHardwareProvider.kt
 * Contains declarations for AndroidKeystoreHardwareProvider.kt.
 */
@file:Suppress("MaxLineLength", "ReturnCount")

package io.healthplatform.chartcam.storage

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
    override fun getHardwareStatus(): BiometricHardwareStatus {
        val context = contextProvider?.invoke() ?: runCatching { AndroidAppInit.getContext() }.getOrNull()
        if (context == null) return BiometricHardwareStatus.NO_HARDWARE

        val biometricManager = BiometricManager.from(context)
        return when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricHardwareStatus.AVAILABLE // Hardware exists but unavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricHardwareStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricHardwareStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricHardwareStatus.NO_HARDWARE
            else -> BiometricHardwareStatus.NO_HARDWARE
        }
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
    ): Result<BiometricAuthResult> {
        val status = getHardwareStatus()
        if (status != BiometricHardwareStatus.AVAILABLE) {
            return Result.success(BiometricAuthResult.HardwareError("Biometrics unavailable: $status"))
        }

        val activity =
            AndroidAppInit.currentActivity
                ?: return Result.success(BiometricAuthResult.HardwareError("No active FragmentActivity to display BiometricPrompt"))

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        /**
                         * Called when an unrecoverable error has been encountered and the operation is complete.
                         *
                         * @param errorCode An integer ID associated with the error.
                         * @param errString A human-readable string that describes the error.
                         */
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            val result =
                                when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                    BiometricPrompt.ERROR_CANCELED,
                                    -> BiometricAuthResult.FallbackToPassword
                                    BiometricPrompt.ERROR_LOCKOUT -> BiometricAuthResult.TemporarilyLockedOut(30)
                                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthResult.PermanentlyLockedOut
                                    else -> BiometricAuthResult.HardwareError(errString.toString())
                                }
                            if (continuation.isActive) {
                                continuation.resume(Result.success(result))
                            }
                        }

                        /**
                         * Called when a biometric is recognized.
                         *
                         * @param result An object containing authentication-related data.
                         */
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            if (continuation.isActive) {
                                continuation.resume(Result.success(BiometricAuthResult.Success))
                            }
                        }

                        /**
                         * Called when a biometric is valid but not recognized.
                         */
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // Don't resume on failed. Android handles retries automatically until ERROR_LOCKOUT.
                            // We will let it hit ERROR_LOCKOUT or Success.
                        }
                    },
                )

            val promptInfo =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    ).build()

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }

            biometricPrompt.authenticate(promptInfo)
        }
    }
}

/**
 * Creates an [AndroidKeystoreHardwareProvider] instance.
 *
 * @return The Android keystore hardware provider.
 */
actual fun createKeystoreHardwareProvider(): KeystoreHardwareProvider = AndroidKeystoreHardwareProvider()
