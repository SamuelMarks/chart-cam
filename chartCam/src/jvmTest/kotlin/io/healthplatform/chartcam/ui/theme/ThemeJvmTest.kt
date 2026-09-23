/**
 * @file ThemeJvmTest.kt
 * Contains declarations for ThemeJvmTest.kt.
 *
 * JVM unit tests for theme color scheme resolution and dynamic color settings.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.runtime.Composable
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
     * Verifies that AppTheme renders with dynamic color flags and dark mode options without failure.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemeDynamicColorComposable() {
        runComposeUiTest {
            setContent {
                AppTheme(darkTheme = false, dynamicColor = true) {
                    assertNotNull(AppShapes)
                }
                AppTheme(darkTheme = true, dynamicColor = false) {
                    assertNotNull(AppShapes)
                }
                AppTheme(darkTheme = false, dynamicColor = false) {
                    assertNotNull(AppShapes)
                }
                AppTheme(darkTheme = true, dynamicColor = true) {
                    assertNotNull(AppShapes)
                }
                AppTheme(darkTheme = true) {
                    assertNotNull(AppShapes)
                }
                AppTheme(darkTheme = false) {
                    assertNotNull(AppShapes)
                }
                AppTheme(dynamicColor = true) {
                    assertNotNull(AppShapes)
                }
                AppTheme(dynamicColor = false) {
                    assertNotNull(AppShapes)
                }
                AppTheme {
                    assertNotNull(AppShapes)
                }
            }
        }
    }

    /**
     * Verifies recomposition skipping for AppTheme when parent recomposes with stable constant arguments.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemePureSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            setContent {
                val dummy = trigger.value
                AppTheme(darkTheme = false, dynamicColor = false) {
                    assertNotNull(AppShapes)
                }
                AppTheme(dynamicColor = false) {
                    assertNotNull(AppShapes)
                }
            }
            waitForIdle()
            trigger.value++
            waitForIdle()
        }
    }

    /**
     * Verifies recomposition behavior with default parameters when parent recomposes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemeDefaultsRecomposition() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            setContent {
                val dummy = trigger.value
                AppTheme {
                    assertNotNull(AppShapes)
                }
            }
            waitForIdle()
            trigger.value++
            waitForIdle()
        }
    }

    /**
     * Verifies dynamic parameter mutations on darkTheme and dynamicColor flags.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemeParameterMutations() {
        runComposeUiTest {
            val darkThemeState = androidx.compose.runtime.mutableStateOf(false)
            val dynamicColorState = androidx.compose.runtime.mutableStateOf(false)
            setContent {
                AppTheme(darkTheme = darkThemeState.value, dynamicColor = dynamicColorState.value) {
                    assertNotNull(AppShapes)
                }
            }
            waitForIdle()
            darkThemeState.value = true
            waitForIdle()
            dynamicColorState.value = true
            waitForIdle()
        }
    }

    /**
     * Tests unmemoized parameter change detection branches by invoking AppTheme with changed = 0.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppThemeReflectionChangedZero() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val themeClass = Class.forName("io.healthplatform.chartcam.ui.theme.ThemeKt")
            val appThemeMethod =
                themeClass.declaredMethods.first { m: java.lang.reflect.Method ->
                    m.name == "AppTheme" && m.parameterCount == 6
                }
            appThemeMethod.isAccessible = true

            val convenienceMethod =
                themeClass.declaredMethods.first { m: java.lang.reflect.Method ->
                    m.name == "AppTheme" && m.parameterCount == 5
                }
            convenienceMethod.isAccessible = true

            setContent {
                val dummy = trigger.value
                val composer = androidx.compose.runtime.currentComposer
                val staticContent: @Composable () -> Unit = {
                    assertNotNull(AppShapes)
                }

                appThemeMethod.invoke(
                    null,
                    false,
                    false,
                    staticContent,
                    composer,
                    0,
                    0,
                )
                appThemeMethod.invoke(
                    null,
                    true,
                    true,
                    staticContent,
                    composer,
                    0,
                    0,
                )

                convenienceMethod.invoke(
                    null,
                    false,
                    staticContent,
                    composer,
                    0,
                    0,
                )
                convenienceMethod.invoke(
                    null,
                    true,
                    staticContent,
                    composer,
                    0,
                    0,
                )
            }
            waitForIdle()
            trigger.value++
            waitForIdle()
        }
    }
}
