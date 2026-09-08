/**
 * @file DemoModeBannerJvmTest.kt
 * Contains declarations for DemoModeBannerJvmTest.kt.
 *
 * JVM Compose UI tests validating the clinical safety banner [DemoModeBanner].
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.components.DemoModeBanner
import io.healthplatform.chartcam.ui.components.TAG_DEMO_BANNER
import io.healthplatform.chartcam.ui.components.TAG_EXIT_DEMO_BUTTON
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Validates rendering, user interaction, and localization of [DemoModeBanner].
 */
class DemoModeBannerJvmTest {
    /**
     * Verifies that the demo mode banner renders safety copy and provides an exit trigger.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDemoModeBannerRenderingAndExit() {
        setAppLanguage("en")
        runComposeUiTest {
            var exitInvoked = false
            setContent {
                DemoModeBanner(
                    onExitDemo = { exitInvoked = true },
                )
            }
            waitForIdle()

            onNodeWithTag(TAG_DEMO_BANNER).assertIsDisplayed()
            onNodeWithText("DEMO MODE — Sample Data Only").assertIsDisplayed()
            onNodeWithText("Do not enter real protected health information (PHI).").assertIsDisplayed()

            val exitButton = onNodeWithTag(TAG_EXIT_DEMO_BUTTON)
            exitButton.assertIsDisplayed().assertHasClickAction()
            onNodeWithText("Exit Demo").assertIsDisplayed()

            exitButton.performClick()
            waitForIdle()

            assertTrue(exitInvoked)
        }
    }

    /**
     * Verifies that the demo mode banner updates its copy when language changes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDemoModeBannerLocalization() {
        runComposeUiTest {
            setContent {
                DemoModeBanner(
                    onExitDemo = {},
                )
            }
            waitForIdle()

            setAppLanguage("es")
            waitForIdle()

            onNodeWithText("MODO DEMO — Solo Datos de Muestra").assertIsDisplayed()
            onNodeWithText("Salir de Demo").assertIsDisplayed()

            // Reset back to English
            setAppLanguage("en")
            waitForIdle()

            onNodeWithText("DEMO MODE — Sample Data Only").assertIsDisplayed()
        }
    }
}
