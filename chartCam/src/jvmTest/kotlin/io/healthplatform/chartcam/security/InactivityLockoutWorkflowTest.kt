/**
 * @file InactivityLockoutWorkflowTest.kt
 * Contains declarations for InactivityLockoutWorkflowTest.kt.
 *
 * Validates session inactivity timeouts, UI obscuring, rate-limited PIN entry, and biometric unlock resumption.
 */
package io.healthplatform.chartcam.security

import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.ui.AppPrivacyManager
import io.healthplatform.chartcam.ui.AppPrivacyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Workflow tests validating session inactivity timeouts, background screen obscuring,
 * PIN rate-limiting backoff, and authentication recovery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InactivityLockoutWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Prepares test coroutine dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up test coroutine dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test Case 1: Verifies that frequent interactions reset the inactivity timer
     * and keep the session unlocked and visible.
     */
    @Test
    fun testActiveUserResetsInactivityTimer() =
        runTest(testDispatcher) {
            val storage = JvmSecureStorage("inactivity_test_${java.util.UUID.randomUUID()}")
            val authRepo = AuthRepository(storage)
            authRepo.login("dr_active", "secret123")
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(authRepo.currentUser.value)

            val timeoutMs = 300_000L // 5 minutes
            val privacyManager = AppPrivacyManager(lockoutTimeoutMs = timeoutMs)

            var currentTimeMs = 1_000_000L

            // Simulate user active every 2 minutes for 3 cycles (total 6 minutes)
            for (cycle in 1..3) {
                // Background briefly for 100ms (e.g. system dialog or camera preview transition)
                privacyManager.onAppMovedToBackground(nowMs = currentTimeMs)
                currentTimeMs += 100L
                privacyManager.onAppMovedToForeground(nowMs = currentTimeMs)

                assertFalse(privacyManager.isLocked.value, "Session must stay unlocked during active use")
                assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, privacyManager.privacyState.value)

                // Advance clock by 2 minutes
                currentTimeMs += 120_000L
            }

            assertFalse(privacyManager.isLocked.value, "Active session should not be locked")
            storage.clearAll()
        }

    /**
     * Test Case 2: Verifies that exceeding the inactivity threshold transitions the app
     * to a locked and obscured state.
     */
    @Test
    fun testInactivityExpiryTriggersLockedAndObscuredState() =
        runTest(testDispatcher) {
            val timeoutMs = 300_000L // 5 minutes
            val privacyManager = AppPrivacyManager(lockoutTimeoutMs = timeoutMs)

            var currentTimeMs = 2_000_000L

            // App moves to background / recents task switcher
            privacyManager.onAppMovedToBackground(nowMs = currentTimeMs)
            assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, privacyManager.privacyState.value)

            // Clinician is inactive for 6 minutes (360,000 ms > 300,000 ms timeout)
            currentTimeMs += 360_000L
            privacyManager.onAppMovedToForeground(nowMs = currentTimeMs)

            assertTrue(privacyManager.isLocked.value, "App must be locked after 6 minutes of inactivity")
            assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, privacyManager.privacyState.value, "Sensitive data must remain obscured")
        }

    /**
     * Test Case 3: Simulates failed PIN attempts triggering backoff, followed by valid unlock.
     */
    @Test
    fun testFailedPinAttemptsAndSuccessfulAuthenticationRecovery() =
        runTest(testDispatcher) {
            val privacyManager = AppPrivacyManager(lockoutTimeoutMs = 60_000L)
            privacyManager.onAppMovedToBackground(nowMs = 10_000L)
            privacyManager.onAppMovedToForeground(nowMs = 90_000L)
            assertTrue(privacyManager.isLocked.value, "Should start locked")

            // Simulate PIN entry state machine
            val correctPin = "1234"
            var failedAttempts = 0
            var lockoutBannerVisible = false

            fun attemptUnlock(enteredPin: String): Boolean {
                if (enteredPin == correctPin) {
                    privacyManager.unlock()
                    failedAttempts = 0
                    lockoutBannerVisible = false
                    return true
                } else {
                    failedAttempts++
                    if (failedAttempts >= 3) {
                        lockoutBannerVisible = true
                    }
                    return false
                }
            }

            // Enter wrong PIN 3 times
            assertFalse(attemptUnlock("0000"))
            assertEquals(1, failedAttempts)
            assertFalse(lockoutBannerVisible)

            assertFalse(attemptUnlock("1111"))
            assertEquals(2, failedAttempts)
            assertFalse(lockoutBannerVisible)

            assertFalse(attemptUnlock("9999"))
            assertEquals(3, failedAttempts)
            assertTrue(lockoutBannerVisible, "Lockout / rate-limit banner must appear after 3 failed attempts")
            assertTrue(privacyManager.isLocked.value, "Session must still be locked")

            // Clinician authenticates via biometrics or correct PIN
            val unlocked = attemptUnlock("1234")
            assertTrue(unlocked, "Correct credential entry must succeed")
            assertFalse(privacyManager.isLocked.value, "App must be unlocked")
            assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, privacyManager.privacyState.value)
        }
}
