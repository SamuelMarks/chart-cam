/**
 * @file ThemeJvmTest.kt
 * Contains declarations for ThemeJvmTest.kt.
 *
 * JVM unit tests for theme color scheme resolution and dynamic color settings.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests verifying theme and platform color scheme resolution.
 */
class ThemeJvmTest {
    /**
     * Verifies that resolvePlatformColorScheme correctly resolves light and dark themes on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testResolvePlatformColorSchemeJvm() {
        runComposeUiTest {
            setContent {
                val lightScheme = resolvePlatformColorScheme(darkTheme = false, dynamicColor = false)
                val darkScheme = resolvePlatformColorScheme(darkTheme = true, dynamicColor = false)
                val dynamicScheme = resolvePlatformColorScheme(darkTheme = false, dynamicColor = true)

                assertEquals(LightColors, lightScheme)
                assertEquals(DarkColors, darkScheme)
                assertEquals(LightColors, dynamicScheme)
            }
        }
    }

    /**
     * Verifies that AppTheme renders with dynamic color flags without failure.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemeDynamicColorComposable() {
        runComposeUiTest {
            setContent {
                AppTheme(darkTheme = false, dynamicColor = true) {
                    assertNotNull(AppShapes)
                }
            }
        }
    }
}
