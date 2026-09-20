/**
 * @file SetupPlatformPrivacyAndroidTest.kt
 * Contains declarations for SetupPlatformPrivacyAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Headless unit applier for testing SetupPlatformPrivacy without Android View hierarchy.
 */
private class PrivacyHeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * Fake lifecycle owner providing a manageable [LifecycleRegistry].
 */
private class FakeLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = registry
}

/**
 * Android host tests for SetupPlatformPrivacy and AppPrivacyManager.
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class SetupPlatformPrivacyAndroidTest {
    /**
     * Tests that currentAppPrivacyManager lifecycle state transitions execute cleanly on Android.
     */
    @Test
    fun testSetupPlatformPrivacyRuns() {
        currentAppPrivacyManager.onAppMovedToBackground()
        assertNotNull(currentAppPrivacyManager)
        currentAppPrivacyManager.onAppMovedToForeground()
        assertNotNull(currentAppPrivacyManager)
    }

    /**
     * Verifies SetupPlatformPrivacy lifecycle event observation and cleanup on disposal.
     */
    @Test
    fun testSetupPlatformPrivacyComposableLifecycle() {
        val applier = PrivacyHeadlessApplier()
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(applier, recomposer)
        val fakeOwner = FakeLifecycleOwner()
        fakeOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composition.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides fakeOwner) {
                SetupPlatformPrivacy()
            }
        }

        // 1. Trigger ON_RESUME -> Foreground
        fakeOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertEquals(AppPrivacyState.FOREGROUND_VISIBLE, currentAppPrivacyManager.privacyState.value)

        // 2. Trigger ON_PAUSE -> Background
        fakeOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertEquals(AppPrivacyState.BACKGROUND_OBSCURED, currentAppPrivacyManager.privacyState.value)

        // 3. Trigger else branch (ON_STOP)
        fakeOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        // 4. Dispose composition -> exercises onDispose block
        composition.dispose()
    }
}
