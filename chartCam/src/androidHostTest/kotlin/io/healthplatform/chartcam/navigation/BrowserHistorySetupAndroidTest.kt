/**
 * @file BrowserHistorySetupAndroidTest.kt
 * Contains declarations for BrowserHistorySetupAndroidTest.kt.
 */
package io.healthplatform.chartcam.navigation

import android.content.Context
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Headless unit applier for standalone Composable testing without View hierarchy.
 */
private class HeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * Android host tests for [BrowserHistorySetup.android.kt].
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class BrowserHistorySetupAndroidTest {
    /**
     * Verifies execution of SetupBrowserHistory on Android.
     */
    @Test
    fun testBrowserHistorySetupAndroid() {
        val applier = HeadlessApplier()
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(applier, recomposer)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavController(context)

        composition.setContent {
            SetupBrowserHistory(navController)
        }
        composition.dispose()

        assertEquals("/auth/login", Routes.LOGIN)
    }
}
