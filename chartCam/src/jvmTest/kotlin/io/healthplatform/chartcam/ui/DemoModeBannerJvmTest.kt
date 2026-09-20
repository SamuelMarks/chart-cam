/**
 * @file DemoModeBannerJvmTest.kt
 * Contains declarations for DemoModeBannerJvmTest.kt.
 *
 * JVM Compose UI tests validating the clinical safety banner [DemoModeBanner].
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

            // Also test semantics onClick action and custom modifier recomposition
            val modState = androidx.compose.runtime.mutableStateOf<Modifier>(Modifier.fillMaxWidth())
            val exitLambdaState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            setContent {
                DemoModeBanner(
                    onExitDemo = exitLambdaState.value,
                    modifier = modState.value,
                )
            }
            waitForIdle()
            onNodeWithTag(TAG_EXIT_DEMO_BUTTON).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            // Mutate modifier and lambda to trigger composer.changed true branch
            modState.value = Modifier.fillMaxWidth().testTag("recomposed_banner")
            exitLambdaState.value = { val x = 1 }
            waitForIdle()

            // Trigger parent recomposition where DemoModeBanner inputs do NOT change (smart recomposition skip)
            val parentState = androidx.compose.runtime.mutableStateOf(0)
            val stableExit: () -> Unit = {}
            setContent {
                val p = parentState.value
                androidx.compose.material3.Text("Parent: $p")
                DemoModeBanner(onExitDemo = stableExit)
            }
            waitForIdle()
            parentState.value = 1
            waitForIdle()
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
