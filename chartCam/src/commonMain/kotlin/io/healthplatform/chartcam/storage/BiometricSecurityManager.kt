/**
 * @file BiometricSecurityManager.kt
 * Contains declarations for BiometricSecurityManager.kt.
 *
 * Manages biometric authentication state, hardware keystore validation,
 * enrollment invalidation detection, and lockout/fallback policies.
 */
package io.healthplatform.chartcam.storage

/**
 * Status of hardware biometric sensors and keystore backing.
 */
enum class BiometricHardwareStatus {
    /** Hardware biometric sensor and keystore are available and operational. */
    AVAILABLE,

    /** Device does not possess biometric hardware. */
    NO_HARDWARE,

    /** Biometric hardware exists but is currently unavailable or disabled by policy. */
    HARDWARE_UNAVAILABLE,

    /** Biometric hardware is available, but no biometric credentials are enrolled. */
    NOT_ENROLLED,
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricAuthResult {
    /** Biometric authentication succeeded. */
    data object Success : BiometricAuthResult()

    /**
     * Biometric authentication failed with attempts remaining before lockout.
     *
     * @property attemptsRemaining The number of retry attempts remaining.
     */
    data class Failed(
        val attemptsRemaining: Int,
    ) : BiometricAuthResult()

    /**
     * Biometrics temporarily locked out due to consecutive failed attempts.
     *
     * @property lockoutDurationSeconds The time in seconds the user must wait.
     */
    data class TemporarilyLockedOut(
        val lockoutDurationSeconds: Int,
    ) : BiometricAuthResult()

    /** Biometrics permanently locked out; fallback to primary credentials required. */
    data object PermanentlyLockedOut : BiometricAuthResult()

    /** Biometric key was invalidated because new credentials were added or modified on the host OS. */
    data object KeyPermanentlyInvalidated : BiometricAuthResult()

    /**
     * Biometric hardware or keystore encountered an error.
     *
     * @property message Descriptive error message.
     */
    data class HardwareError(
        val message: String,
    ) : BiometricAuthResult()

    /** Explicit fallback to password or PIN requested. */
    data object FallbackToPassword : BiometricAuthResult()
}

/**
 * Platform abstraction for keystore hardware backing.
 */
interface KeystoreHardwareProvider {
    /**
     * Verifies whether hardware-backed key storage (e.g., Secure Enclave or StrongBox) is available.
     *
     * @return True if hardware backed, false otherwise.
     */
    fun isHardwareBacked(): Boolean

    /**
     * Retrieves the current biometric hardware status.
     *
     * @return The [BiometricHardwareStatus] representing device capabilities.
     */
    fun getHardwareStatus(): BiometricHardwareStatus
}

/**
 * Default mock/fallback provider for systems without specialized hardware.
 *
 * @param isHardware True if simulated hardware should be marked as available.
 * @param status The initial hardware status to return.
 */
class DefaultKeystoreHardwareProvider(
    private val isHardware: Boolean = true,
    private val status: BiometricHardwareStatus = BiometricHardwareStatus.AVAILABLE,
) : KeystoreHardwareProvider {
    /**
     * Returns whether hardware keystore is backed.
     *
     * @return True if hardware backed.
     */
    override fun isHardwareBacked(): Boolean = isHardware

    /**
     * Returns the current hardware status.
     *
     * @return The status.
     */
    override fun getHardwareStatus(): BiometricHardwareStatus = status
}

/**
 * Security manager handling biometric authentication lifecycle, keystore invalidation,
 * lockout backoff policies, and platform secure storage error recovery.
 *
 * @param secureStorage Underlying secure storage for persisting tokens and auth metadata.
 * @param hardwareProvider Hardware provider for checking keystore attestation and status.
 * @param maxAttemptsBeforeTempLockout Failed attempt threshold for temporary lockout.
 * @param maxAttemptsBeforePermanentLockout Failed attempt threshold for permanent lockout.
 */
class BiometricSecurityManager(
    private val secureStorage: SecureStorage,
    private val hardwareProvider: KeystoreHardwareProvider = DefaultKeystoreHardwareProvider(),
    private val maxAttemptsBeforeTempLockout: Int = 5,
    private val maxAttemptsBeforePermanentLockout: Int = 10,
) {
    private var failedAttempts: Int = 0
    private var isTempLocked: Boolean = false
    private var isPermLocked: Boolean = false
    private var keyInvalidated: Boolean = false

    /**
     * Constant keys used for biometric storage entries.
     */
    companion object {
        /** Key for biometric session token. */
        const val KEY_BIOMETRIC_TOKEN = "biometric_auth_token"

        /** iOS Keychain error code: errSecAuthFailed (-25293). */
        const val IOS_ERR_SEC_AUTH_FAILED = -25293

        /** iOS Keychain error code: errSecItemNotFound (-25300). */
        const val IOS_ERR_SEC_ITEM_NOT_FOUND = -25300

        /** iOS Keychain error code: errSecDuplicateItem (-25299). */
        const val IOS_ERR_SEC_DUPLICATE_ITEM = -25299

        /** iOS Keychain error code: errSecInteractionNotAllowed (-25308). */
        const val IOS_ERR_SEC_INTERACTION_NOT_ALLOWED = -25308
    }

    /**
     * Checks device hardware keystore availability, providing graceful degradation status.
     *
     * @return The [BiometricHardwareStatus] detailing availability or degradation mode.
     */
    fun checkKeystoreAvailability(): BiometricHardwareStatus = hardwareProvider.getHardwareStatus()

    /**
     * Checks if hardware keystore backing is available.
     *
     * @return True if backed by Secure Enclave/StrongBox, false if software-only or unavailable.
     */
    fun isHardwareBackedKeystore(): Boolean = hardwareProvider.isHardwareBacked()

    /**
     * Called when host OS reports or keystore throws that biometric credentials have been added/modified,
     * permanently invalidating the previous keys.
     *
     * Clears all cached biometric credentials and marks the key state as invalidated.
     */
    fun onBiometricCredentialsChanged() {
        keyInvalidated = true
        secureStorage.delete(KEY_BIOMETRIC_TOKEN)
    }

    /**
     * Records a failed biometric authentication attempt and computes the resulting security action.
     *
     * @return The updated [BiometricAuthResult] following the failed attempt.
     */
    fun recordFailedAttempt(): BiometricAuthResult {
        if (isPermLocked) {
            return BiometricAuthResult.PermanentlyLockedOut
        }
        failedAttempts++

        return when {
            failedAttempts >= maxAttemptsBeforePermanentLockout -> {
                isPermLocked = true
                BiometricAuthResult.PermanentlyLockedOut
            }
            failedAttempts >= maxAttemptsBeforeTempLockout -> {
                isTempLocked = true
                BiometricAuthResult.TemporarilyLockedOut(lockoutDurationSeconds = 30)
            }
            else -> {
                val remaining = maxAttemptsBeforeTempLockout - failedAttempts
                BiometricAuthResult.Failed(attemptsRemaining = remaining)
            }
        }
    }

    /**
     * Resets failed attempt counters upon successful primary credential verification (password or PIN).
     */
    fun resetLockout() {
        failedAttempts = 0
        isTempLocked = false
        isPermLocked = false
        keyInvalidated = false
    }

    /**
     * Evaluates a biometric authentication request.
     *
     * @param simulateSuccess For testing, simulate success if true, failure if false.
     * @return The [BiometricAuthResult] indicating the outcome.
     */
    fun authenticate(simulateSuccess: Boolean): BiometricAuthResult {
        val status = hardwareProvider.getHardwareStatus()
        return when {
            keyInvalidated -> BiometricAuthResult.KeyPermanentlyInvalidated
            isPermLocked -> BiometricAuthResult.PermanentlyLockedOut
            isTempLocked -> BiometricAuthResult.TemporarilyLockedOut(lockoutDurationSeconds = 30)
            status == BiometricHardwareStatus.NO_HARDWARE ||
                status == BiometricHardwareStatus.HARDWARE_UNAVAILABLE ->
                BiometricAuthResult.HardwareError("Biometric hardware unavailable on this device.")
            status == BiometricHardwareStatus.NOT_ENROLLED ->
                BiometricAuthResult.HardwareError("No biometric credentials enrolled on this device.")
            simulateSuccess -> {
                failedAttempts = 0
                BiometricAuthResult.Success
            }
            else -> recordFailedAttempt()
        }
    }

    /**
     * Translates platform-specific secure storage error codes (e.g. iOS Keychain status codes)
     * into a domain-level [BiometricAuthResult].
     *
     * @param statusCode The native OS status code.
     * @return The mapped [BiometricAuthResult].
     */
    fun mapPlatformErrorCode(statusCode: Int): BiometricAuthResult =
        when (statusCode) {
            IOS_ERR_SEC_AUTH_FAILED -> BiometricAuthResult.Failed(attemptsRemaining = 0)
            IOS_ERR_SEC_ITEM_NOT_FOUND ->
                BiometricAuthResult.HardwareError("Item not found in secure storage.")
            IOS_ERR_SEC_DUPLICATE_ITEM ->
                BiometricAuthResult.HardwareError("Duplicate item detected in secure storage.")
            IOS_ERR_SEC_INTERACTION_NOT_ALLOWED ->
                BiometricAuthResult.TemporarilyLockedOut(lockoutDurationSeconds = 30)
            else -> BiometricAuthResult.HardwareError("Secure storage failure with code: $statusCode")
        }

    /**
     * Handles platform-specific exception (such as Android KeyPermanentlyInvalidatedException).
     *
     * @param throwable The caught exception from Keystore or Keychain operations.
     * @return The translated [BiometricAuthResult].
     */
    fun handlePlatformException(throwable: Throwable): BiometricAuthResult {
        val message = throwable.message ?: ""
        return if (throwable::class.simpleName == "KeyPermanentlyInvalidatedException" ||
            message.contains("KeyPermanentlyInvalidatedException")
        ) {
            onBiometricCredentialsChanged()
            BiometricAuthResult.KeyPermanentlyInvalidated
        } else if (throwable::class.simpleName == "UserNotAuthenticatedException" ||
            message.contains("UserNotAuthenticatedException")
        ) {
            BiometricAuthResult.Failed(attemptsRemaining = 0)
        } else {
            BiometricAuthResult.HardwareError("Platform security exception: ${throwable.message}")
        }
    }
}
