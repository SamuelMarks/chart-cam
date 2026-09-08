/**
 * @file LevelerOverlayJvmTest.kt
 * Contains declarations for LevelerOverlayJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test

/**
 * Test class for LevelerOverlay on JVM.
 */
class LevelerOverlayJvmTest {
    /**
     * Test leveler overlay on JVM when device is level.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayWhenLevel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LevelerOverlay(pitch = 0f, roll = 0f)
            }
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is level").assertIsDisplayed()
        }
    }

    /**
     * Test leveler overlay on JVM when device is tilted.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayWhenTilted() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LevelerOverlay(pitch = 15f, roll = 15f)
            }
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is tilted").assertIsDisplayed()
        }
    }
}
