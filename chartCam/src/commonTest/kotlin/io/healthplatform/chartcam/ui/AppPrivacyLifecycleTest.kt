/**
 * @file AppPrivacyLifecycleTest.kt
 * Unit tests verifying AppPrivacyManager background obscuring and inactivity timeout lockout.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)

        manager.unlock()
        assertFalse(manager.isLocked.value)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, manager.privacyState.value)
    }

    /**
     * Verifies calling foreground when the app was never marked as backgrounded.
     */
    @Test
    fun testForegroundWithoutPriorBackground() {
        val manager = AppPrivacyManager(lockoutTimeoutMs = 5000L)
        // Never called onAppMovedToBackground, so backgroundTimestamp is null
        manager.onAppMovedToForeground()
        assertFalse(manager.isLocked.value)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, manager.privacyState.value)
    }

    /**
     * Verifies calling foreground when the app is already locked and backgroundTimestamp is null or non-null.
     */
    @Test
    fun testForegroundWhenAlreadyLocked() {
        val manager = AppPrivacyManager(lockoutTimeoutMs = 50L)
        manager.onAppMovedToBackground(nowMs = 1000L)
        manager.onAppMovedToForeground(nowMs = 1100L)
        assertTrue(manager.isLocked.value)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)

        // Call foreground again with bgTime null: !_isLocked.value is false, stays locked and obscured
        manager.onAppMovedToForeground(nowMs = 1200L)
        assertTrue(manager.isLocked.value)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)

        // Background again and foreground within timeout while still locked: !_isLocked.value is false
        manager.onAppMovedToBackground(nowMs = 1300L)
        manager.onAppMovedToForeground(nowMs = 1310L)
        assertTrue(manager.isLocked.value)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, manager.privacyState.value)
    }

    /**
     * Verifies default constructor parameters and global singleton instance.
     */
    @Test
    fun testDefaultConstructorAndGlobalInstance() {
        val defaultManager = AppPrivacyManager()
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, defaultManager.privacyState.value)
        assertFalse(defaultManager.isLocked.value)

        assertNotNull(currentAppPrivacyManager)
        currentAppPrivacyManager.unlock()
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, currentAppPrivacyManager.privacyState.value)
    }

    /**
     * Verifies AppPrivacyState enum values and entries.
     */
    @Test
    fun testAppPrivacyStateEnum() {
        val visible = AppPrivacyState.valueOf("FOREGROUND_VISIBLE")
        val obscured = AppPrivacyState.valueOf("BACKGROUND_OBSCURED")
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, visible)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, obscured)
        assertEquals(2, AppPrivacyState.entries.size)
    }
}
