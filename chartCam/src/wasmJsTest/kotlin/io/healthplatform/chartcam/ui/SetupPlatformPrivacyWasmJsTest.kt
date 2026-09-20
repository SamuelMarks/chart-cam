/**
 * @file SetupPlatformPrivacyWasmJsTest.kt
 * Contains declarations for SetupPlatformPrivacyWasmJsTest.kt.
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
 * Headless unit applier for testing SetupPlatformPrivacy on WasmJs.
 */
private class PrivacyWasmJsHeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * WasmJs unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyWasmJsTest {
    /**
     * Verifies SetupPlatformPrivacy references currentAppPrivacyManager on WasmJs.
     */
    @Test
    fun testSetupPlatformPrivacyWasmJs() {
        assertNotNull(currentAppPrivacyManager)
    }

    /**
     * Verifies background and foreground privacy transitions on WasmJs.
     */
    @Test
    fun testSetupPlatformPrivacyWasmJsTransitions() {
        currentAppPrivacyManager.onAppMovedToBackground()
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, currentAppPrivacyManager.privacyState.value)
        currentAppPrivacyManager.onAppMovedToForeground()
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, currentAppPrivacyManager.privacyState.value)
    }

    /**
     * Verifies SetupPlatformPrivacy composable lifecycle execution and disposal on WasmJs.
     */
    @Test
    fun testSetupPlatformPrivacyComposableWasmJs() {
        val applier = PrivacyWasmJsHeadlessApplier()
        val composition = Composition(applier, Recomposer(Dispatchers.Unconfined))
        composition.setContent {
            SetupPlatformPrivacy()
        }
        composition.dispose()
        assertNotNull(currentAppPrivacyManager)
    }
}
