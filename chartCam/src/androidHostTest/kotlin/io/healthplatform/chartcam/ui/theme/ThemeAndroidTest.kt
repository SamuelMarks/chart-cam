/**
 * @file ThemeAndroidTest.kt
 * Contains declarations for ThemeAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Headless unit applier for testing Composable theme resolution without Android View hierarchy.
 */
private class ThemeHeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * Android host unit tests for [resolvePlatformColorScheme].
 */
@RunWith(RobolectricTestRunner::class)
class ThemeAndroidTest {
    /**
     * Verifies platform color scheme resolution on Android 12+ (API 33).
     */
    @Config(manifest = Config.NONE, sdk = [33])
    @Test
    fun testResolvePlatformColorSchemeApi33() {
        val applier = ThemeHeadlessApplier()
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(applier, recomposer)
        val context = ApplicationProvider.getApplicationContext<Context>()

        var darkDynamic: ColorScheme? = null
        var lightDynamic: ColorScheme? = null
        var darkStatic: ColorScheme? = null
        var lightStatic: ColorScheme? = null

        composition.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                darkDynamic = resolvePlatformColorScheme(darkTheme = true, dynamicColor = true)
                lightDynamic = resolvePlatformColorScheme(darkTheme = false, dynamicColor = true)
                darkStatic = resolvePlatformColorScheme(darkTheme = true, dynamicColor = false)
                lightStatic = resolvePlatformColorScheme(darkTheme = false, dynamicColor = false)
            }
        }
        composition.dispose()

        assertNotNull(darkDynamic)
        assertNotNull(lightDynamic)
        assertEquals(DarkColors, darkStatic)
        assertEquals(LightColors, lightStatic)
    }

    /**
     * Verifies platform color scheme resolution on Android versions below API 31 (API 30).
     */
    @Config(manifest = Config.NONE, sdk = [30])
    @Test
    fun testResolvePlatformColorSchemePreApi31() {
        val applier = ThemeHeadlessApplier()
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(applier, recomposer)
        val context = ApplicationProvider.getApplicationContext<Context>()

        var darkDynamicFallback: ColorScheme? = null
        var lightDynamicFallback: ColorScheme? = null

        composition.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                darkDynamicFallback = resolvePlatformColorScheme(darkTheme = true, dynamicColor = true)
                lightDynamicFallback = resolvePlatformColorScheme(darkTheme = false, dynamicColor = true)
            }
        }
        composition.dispose()

        assertEquals(DarkColors, darkDynamicFallback)
        assertEquals(LightColors, lightDynamicFallback)
    }
}
