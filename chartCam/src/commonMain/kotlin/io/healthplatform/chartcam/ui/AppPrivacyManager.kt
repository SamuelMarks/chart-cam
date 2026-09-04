/**
 * @file AppPrivacyManager.kt
 * Contains declarations for AppPrivacyManager.kt.
 *
 * Manages privacy obscuring for sensitive UI screens and automatic app lockout
 * when the application enters the background or recents task switcher.
 */
package io.healthplatform.chartcam.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Visual privacy state indicating whether the UI is foreground visible or background obscured.
 */
enum class AppPrivacyState {
    /** The application is in the active foreground and content is visible. */
    FOREGROUND_VISIBLE,

    /** The application is backgrounded or in the task switcher, requiring sensitive content to be obscured. */
    BACKGROUND_OBSCURED,
}

/**
 * Manages screen obscuring and inactivity timeout lockout for clinical data security.
 *
 * @param lockoutTimeoutMs The duration of backgrounding in milliseconds after which the app locks.
 */
class AppPrivacyManager(
    private val lockoutTimeoutMs: Long = 60_000L,
) {
    private val _privacyState = MutableStateFlow(AppPrivacyState.FOREGROUND_VISIBLE)

    /**
     * Flow of the current privacy obscuring state.
     */
    val privacyState: StateFlow<AppPrivacyState> = _privacyState.asStateFlow()

    private val _isLocked = MutableStateFlow(false)

    /**
     * Flow indicating whether the app is currently locked due to timeout.
     */
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long? = null

    /**
     * Invoked when the application transitions to the background or the OS task switcher.
     * Obscures sensitive UI content immediately.
     *
     * @param nowMs The timestamp in milliseconds when backgrounding occurred.
     */
    fun onAppMovedToBackground(nowMs: Long = Clock.System.now().toEpochMilliseconds()) {
        _privacyState.value = AppPrivacyState.BACKGROUND_OBSCURED
        backgroundTimestamp = nowMs
    }

    /**
     * Invoked when the application transitions back to the foreground.
     * Evaluates whether the background duration exceeded the lockout timeout.
     *
     * @param nowMs The timestamp in milliseconds when foregrounding occurred.
     */
    fun onAppMovedToForeground(nowMs: Long = Clock.System.now().toEpochMilliseconds()) {
        val bgTime = backgroundTimestamp
        if (bgTime != null && (nowMs - bgTime) >= lockoutTimeoutMs) {
            _isLocked.value = true
        }

        if (!_isLocked.value) {
            _privacyState.value = AppPrivacyState.FOREGROUND_VISIBLE
        }
        backgroundTimestamp = null
    }

    /**
     * Unlocks the session following successful clinician re-authentication.
     */
    fun unlock() {
        _isLocked.value = false
        _privacyState.value = AppPrivacyState.FOREGROUND_VISIBLE
    }
}
