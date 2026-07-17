/**
 * @file LevelerOverlayJvmTest.kt
 * Contains declarations for LevelerOverlayJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class LevelerOverlayJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlay() =
        runComposeUiTest {
            setContent {
                LevelerOverlay(pitch = 0f, roll = 0f)
            }
            // Since LevelerOverlay draws on canvas, there is no text usually unless we use content descriptions/semantics.
            // If it doesn't crash, the UI test passes.
        }
}
