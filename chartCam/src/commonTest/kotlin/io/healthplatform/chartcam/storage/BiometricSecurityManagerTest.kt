/**
 * @file BiometricSecurityManagerTest.kt
 * Contains declarations for BiometricSecurityManagerTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests covering Section 1: Biometric & Key Invalidation.
 */
class BiometricSecurityManagerTest {
    /**
     * In-memory test storage for verification.
     */
    private class FakeSecureStorage : SecureStorage {
        private val map = mutableMapOf<String, String>()

        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(key: String): String? = map[key]

        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Test keystore behavior when new biometric credentials are added or modified on the host OS.
     */
    @Test
    fun testKeyInvalidationOnNewEnrollment() {
        val storage = FakeSecureStorage()
        storage.save(BiometricSecurityManager.KEY_BIOMETRIC_TOKEN, "active_biometric_token_123")

        val manager = BiometricSecurityManager(storage)
        assertEquals("active_biometric_token_123", storage.getString(BiometricSecurityManager.KEY_BIOMETRIC_TOKEN))

        // Host OS notifies new enrollment or modified biometrics
        manager.onBiometricCredentialsChanged()

        // Biometric token must be wiped from secure storage
        assertNull(storage.getString(BiometricSecurityManager.KEY_BIOMETRIC_TOKEN))

        // Subsequent authentication attempt should return KeyPermanentlyInvalidated
        val result = manager.authenticate(simulateSuccess = true)
        assertEquals(BiometricAuthResult.KeyPermanentlyInvalidated, result)
    }

    /**
     * Verify handling when consecutive failed biometric attempts trigger temporary or permanent device lockout.
     */
    @Test
    fun testBiometricLockoutAndFallback() {
        val storage = FakeSecureStorage()
        val manager = BiometricSecurityManager(storage, maxAttemptsBeforeTempLockout = 3, maxAttemptsBeforePermanentLockout = 5)

        // Attempt 1 fails -> 2 remaining
        val res1 = manager.recordFailedAttempt()
        assertTrue(res1 is BiometricAuthResult.Failed)
        assertEquals(2, res1.attemptsRemaining)

        // Attempt 2 fails -> 1 remaining
        val res2 = manager.recordFailedAttempt()
        assertTrue(res2 is BiometricAuthResult.Failed)
        assertEquals(1, res2.attemptsRemaining)

        // Attempt 3 fails -> Temporary Lockout
        val res3 = manager.recordFailedAttempt()
        assertTrue(res3 is BiometricAuthResult.TemporarilyLockedOut)
        assertEquals(30, res3.lockoutDurationSeconds)

        // While temporarily locked out, authenticate attempts return TemporarilyLockedOut
        val authWhileLocked = manager.authenticate(simulateSuccess = true)
        assertTrue(authWhileLocked is BiometricAuthResult.TemporarilyLockedOut)

        // Attempt 4 and 5 fail -> Permanent Lockout
        manager.recordFailedAttempt()
        val res5 = manager.recordFailedAttempt()
        assertEquals(BiometricAuthResult.PermanentlyLockedOut, res5)

        // Consecutive failure when already permanently locked
        val resAlreadyLocked = manager.recordFailedAttempt()
        assertEquals(BiometricAuthResult.PermanentlyLockedOut, resAlreadyLocked)

        // Permanent lockout forces credential fallback
        val authWhilePermLocked = manager.authenticate(simulateSuccess = true)
        assertEquals(BiometricAuthResult.PermanentlyLockedOut, authWhilePermLocked)

        // Authenticate with simulateSuccess = false routes to recordFailedAttempt
        val freshManager = BiometricSecurityManager(storage)
        val authFailed = freshManager.authenticate(simulateSuccess = false)
        assertTrue(authFailed is BiometricAuthResult.Failed)

        // Fallback: Primary password verification resets lockout
        manager.resetLockout()
        val authAfterReset = manager.authenticate(simulateSuccess = true)
        assertEquals(BiometricAuthResult.Success, authAfterReset)
    }

    /**
     * Ensure graceful degradation or actionable error reporting when hardware-backed key attestation/storage is unavailable.
     */
    @Test
    fun testHardwareKeystoreUnavailability() {
        val storage = FakeSecureStorage()

        // Test with no hardware
        val noHardwareProvider = DefaultKeystoreHardwareProvider(isHardware = false, status = BiometricHardwareStatus.NO_HARDWARE)
        val managerNoHardware = BiometricSecurityManager(storage, hardwareProvider = noHardwareProvider)
        assertFalse(managerNoHardware.isHardwareBackedKeystore())
        assertEquals(BiometricHardwareStatus.NO_HARDWARE, managerNoHardware.checkKeystoreAvailability())
        val authNoHardware = managerNoHardware.authenticate(simulateSuccess = true)
        assertTrue(authNoHardware is BiometricAuthResult.HardwareError)

        // Test with hardware unavailable (disabled by policy / StrongBox unavailable)
        val unavailProvider = DefaultKeystoreHardwareProvider(isHardware = false, status = BiometricHardwareStatus.HARDWARE_UNAVAILABLE)
        val managerUnavail = BiometricSecurityManager(storage, hardwareProvider = unavailProvider)
        assertEquals(BiometricHardwareStatus.HARDWARE_UNAVAILABLE, managerUnavail.checkKeystoreAvailability())
        val authUnavail = managerUnavail.authenticate(simulateSuccess = true)
        assertTrue(authUnavail is BiometricAuthResult.HardwareError)

        // Test not enrolled
        val notEnrolledProvider = DefaultKeystoreHardwareProvider(isHardware = true, status = BiometricHardwareStatus.NOT_ENROLLED)
        val managerNotEnrolled = BiometricSecurityManager(storage, hardwareProvider = notEnrolledProvider)
        val authNotEnrolled = managerNotEnrolled.authenticate(simulateSuccess = true)
        assertTrue(authNotEnrolled is BiometricAuthResult.HardwareError)
    }

    /**
     * Unit test iOS Keychain and Android Keystore wrapper error paths for status codes like
     * errSecAuthFailed, errSecItemNotFound, errSecDuplicateItem, or KeyPermanentlyInvalidatedException.
     */
    @Test
    fun testPlatformSecureStorageEdgeCases() {
        val storage = FakeSecureStorage()
        val manager = BiometricSecurityManager(storage)

        // FallbackToPassword object
        assertEquals("FallbackToPassword", BiometricAuthResult.FallbackToPassword.toString())

        // iOS Keychain: errSecAuthFailed (-25293)
        val resAuthFailed = manager.mapPlatformErrorCode(BiometricSecurityManager.IOS_ERR_SEC_AUTH_FAILED)
        assertTrue(resAuthFailed is BiometricAuthResult.Failed)

        // iOS Keychain: errSecItemNotFound (-25300)
        val resNotFound = manager.mapPlatformErrorCode(BiometricSecurityManager.IOS_ERR_SEC_ITEM_NOT_FOUND)
        assertTrue(resNotFound is BiometricAuthResult.HardwareError)

        // iOS Keychain: errSecDuplicateItem (-25299)
        val resDup = manager.mapPlatformErrorCode(BiometricSecurityManager.IOS_ERR_SEC_DUPLICATE_ITEM)
        assertTrue(resDup is BiometricAuthResult.HardwareError)

        // iOS Keychain: errSecInteractionNotAllowed (-25308)
        val resInteractionNotAllowed = manager.mapPlatformErrorCode(BiometricSecurityManager.IOS_ERR_SEC_INTERACTION_NOT_ALLOWED)
        assertTrue(resInteractionNotAllowed is BiometricAuthResult.TemporarilyLockedOut)

        // Unknown iOS status code fallback
        val resUnknown = manager.mapPlatformErrorCode(-99999)
        assertTrue(resUnknown is BiometricAuthResult.HardwareError)

        // Android Keystore: KeyPermanentlyInvalidatedException (by class name)
        class KeyPermanentlyInvalidatedException : Exception()
        val resKeyInvalidByClass = manager.handlePlatformException(KeyPermanentlyInvalidatedException())
        assertEquals(BiometricAuthResult.KeyPermanentlyInvalidated, resKeyInvalidByClass)

        // Android Keystore: KeyPermanentlyInvalidatedException (by message)
        val keyInvalidEx = RuntimeException("android.security.keystore.KeyPermanentlyInvalidatedException: Key invalidated")
        val resKeyInvalid = manager.handlePlatformException(keyInvalidEx)
        assertEquals(BiometricAuthResult.KeyPermanentlyInvalidated, resKeyInvalid)

        // Android Keystore: UserNotAuthenticatedException (by class name)
        class UserNotAuthenticatedException : Exception()
        val resUserNotAuthByClass = manager.handlePlatformException(UserNotAuthenticatedException())
        assertTrue(resUserNotAuthByClass is BiometricAuthResult.Failed)

        // Android Keystore: UserNotAuthenticatedException (by message)
        val userNotAuthEx = RuntimeException("android.security.keystore.UserNotAuthenticatedException: User not authenticated")
        val resUserNotAuth = manager.handlePlatformException(userNotAuthEx)
        assertTrue(resUserNotAuth is BiometricAuthResult.Failed)

        // Exception with null message
        val nullMessageEx = Exception()
        val resNullMessage = manager.handlePlatformException(nullMessageEx)
        assertTrue(resNullMessage is BiometricAuthResult.HardwareError)

        // Generic platform security error
        val genericEx = IllegalStateException("Hardware keystore communication failed")
        val resGeneric = manager.handlePlatformException(genericEx)
        assertTrue(resGeneric is BiometricAuthResult.HardwareError)
    }
}
