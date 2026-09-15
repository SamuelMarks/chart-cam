/**
 * @file AppPrivacyLifecycleTest.kt
 * Unit tests verifying AppPrivacyManager background obscuring and inactivity timeout lockout.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for [AppPrivacyManager].
 */
class AppPrivacyLifecycleTest {
    /**
     * Verifies that moving to background obscures UI state.
     */
    @Test
    fun testBackgroundTransitionsToObscured() {
        val manager = AppPrivacyManager(lockoutTimeoutMs = 5000L)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, manager.privacyState.value)
        assertFalse(manager.isLocked.value)

        manager.onAppMovedToBackground()
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)
    }

    /**
     * Verifies foregrounding within timeout restores visibility without locking.
     */
    @Test
    fun testForegroundWithinTimeoutRestoresVisibility() {
        val manager = AppPrivacyManager(lockoutTimeoutMs = 10000L)
        manager.onAppMovedToBackground()
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)

        manager.onAppMovedToForeground()
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, manager.privacyState.value)
        assertFalse(manager.isLocked.value)
    }

    /**
     * Verifies background duration exceeding timeout results in session lockout.
     */
    @Test
    fun testBackgroundExceedingTimeoutTriggersLockout() {
        val manager = AppPrivacyManager(lockoutTimeoutMs = 50L)
        val startMs = 10000L
        manager.onAppMovedToBackground(nowMs = startMs)
        manager.onAppMovedToForeground(nowMs = startMs + 100L)
        assertTrue(manager.isLocked.value)

        manager.unlock()
        assertFalse(manager.isLocked.value)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, manager.privacyState.value)
    }
}
