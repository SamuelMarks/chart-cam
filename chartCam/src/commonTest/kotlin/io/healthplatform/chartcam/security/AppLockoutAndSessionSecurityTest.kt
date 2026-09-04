/**
 * @file AppLockoutAndSessionSecurityTest.kt
 * Contains declarations for AppLockoutAndSessionSecurityTest.kt.
 */
package io.healthplatform.chartcam.security

import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.AppPrivacyManager
import io.healthplatform.chartcam.ui.AppPrivacyState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests covering Section 7: App Lockout & Session Security.
 */
class AppLockoutAndSessionSecurityTest {
    /**
     * In-memory fake secure storage for lockout and session security tests.
     */
    private class ThreadSafeFakeSecureStorage : SecureStorage {
        private val map = mutableMapOf<String, String>()

        /**
         * Persists a key-value pair in memory storage.
         *
         * @param key The key to associate with the value.
         * @param value The value to store.
         */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /**
         * Retrieves a securely stored value by key.
         *
         * @param key The key to look up.
         * @return The stored string value or null if not found.
         */
        override fun getString(key: String): String? = map[key]

        /**
         * Deletes a securely stored value by key.
         *
         * @param key The key of the item to delete.
         */
        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Ensure sensitive UI screens are obscured or blanked when the app moves to the background/recents task switcher.
     */
    @Test
    fun testBackgroundObscuringVerification() {
        val privacyManager = AppPrivacyManager(lockoutTimeoutMs = 30_000L)

        // Initial foreground state
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, privacyManager.privacyState.value)
        assertFalse(privacyManager.isLocked.value)

        // App moves to background / recents task switcher at t = 1000
        privacyManager.onAppMovedToBackground(nowMs = 1000L)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, privacyManager.privacyState.value)

        // Case 1: App returns to foreground within 10 seconds (less than 30s timeout)
        privacyManager.onAppMovedToForeground(nowMs = 11_000L)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, privacyManager.privacyState.value)
        assertFalse(privacyManager.isLocked.value)

        // Case 2: App moves to background and remains for 40 seconds (exceeding 30s timeout)
        privacyManager.onAppMovedToBackground(nowMs = 20_000L)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, privacyManager.privacyState.value)

        privacyManager.onAppMovedToForeground(nowMs = 65_000L)
        assertTrue(privacyManager.isLocked.value, "App must be locked after exceeding background timeout")
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, privacyManager.privacyState.value, "UI must remain obscured while locked")

        // Clinician unlocks with credentials
        privacyManager.unlock()
        assertFalse(privacyManager.isLocked.value)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, privacyManager.privacyState.value)
    }

    /**
     * Test token refresh races when multiple background requests fire simultaneously upon access token expiration.
     */
    @Test
    fun testSessionRaceConditions() =
        runTest {
            val storage = ThreadSafeFakeSecureStorage()
            storage.save("refresh_token", "valid_refresh_token_xyz")
            storage.save("access_token", "expired_access_token_123")

            val authRepo = AuthRepository(storage)

            // Fire 20 simultaneous background requests attempting to refresh token concurrently
            val concurrentRequests = 20
            val refreshJobs =
                (1..concurrentRequests).map {
                    async {
                        authRepo.refreshToken()
                    }
                }

            val results = refreshJobs.awaitAll()

            // All concurrent requests should succeed without race conditions or exceptions
            assertTrue(results.all { it }, "All simultaneous token refresh calls must succeed")

            // Verified that a valid refreshed access token exists in storage
            val refreshedToken = storage.getString("access_token")
            assertNotNull(refreshedToken)
            assertTrue(refreshedToken.startsWith("refreshed_access_token_"))
        }
}
