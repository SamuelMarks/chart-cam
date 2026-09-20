/**
 * @file ThemeIosTest.kt
 * Contains unit tests for iOS theme color scheme resolution.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Headless unit applier for testing Composable theme resolution on iOS.
 */
private class ThemeIosHeadlessApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

/**
 * Validates iOS platform color scheme resolution across theme modes and dynamic color requests.
 */
class ThemeIosTest {
    /**
     * Verifies that resolvePlatformColorScheme resolves dark and light schemes correctly on iOS.
     */
    @Test
    fun testResolvePlatformColorSchemeIos() {
        val applier = ThemeIosHeadlessApplier()
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(applier, recomposer)

        composition.setContent {
            val lightScheme = resolvePlatformColorScheme(darkTheme = false, dynamicColor = false)
            val darkScheme = resolvePlatformColorScheme(darkTheme = true, dynamicColor = false)
            val dynamicLight = resolvePlatformColorScheme(darkTheme = false, dynamicColor = true)
            val dynamicDark = resolvePlatformColorScheme(darkTheme = true, dynamicColor = true)

            assertEquals(LightColors, lightScheme)
            assertEquals(DarkColors, darkScheme)
            assertEquals(LightColors, dynamicLight)
            assertEquals(DarkColors, dynamicDark)
        }
        composition.dispose()
    }
}
