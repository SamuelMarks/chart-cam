/**
 * @file SetupPlatformPrivacyJsTest.kt
 * Contains declarations for SetupPlatformPrivacyJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Headless unit applier for testing SetupPlatformPrivacy on JS.
 */
private class PrivacyJsHeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * JS unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyJsTest {
    /**
     * Verifies SetupPlatformPrivacy references currentAppPrivacyManager on JS.
     */
    @Test
    fun testSetupPlatformPrivacyJs() {
        assertNotNull(currentAppPrivacyManager)
    }

    /**
     * Verifies background and foreground privacy transitions on JS.
     */
    @Test
    fun testSetupPlatformPrivacyJsTransitions() {
        currentAppPrivacyManager.onAppMovedToBackground()
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, currentAppPrivacyManager.privacyState.value)
        currentAppPrivacyManager.onAppMovedToForeground()
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, currentAppPrivacyManager.privacyState.value)
    }

    /**
     * Verifies SetupPlatformPrivacy composable lifecycle execution and disposal on JS.
     */
    @Test
    fun testSetupPlatformPrivacyComposableJs() {
        val applier = PrivacyJsHeadlessApplier()
        val composition = Composition(applier, Recomposer(Dispatchers.Unconfined))
        composition.setContent {
            SetupPlatformPrivacy()
        }
        composition.dispose()
        assertNotNull(currentAppPrivacyManager)
    }
}
