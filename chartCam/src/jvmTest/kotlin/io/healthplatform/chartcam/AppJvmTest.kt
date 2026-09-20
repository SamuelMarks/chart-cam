/**
 * @file AppJvmTest.kt
 * Contains declarations for AppJvmTest.kt.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.currentAppPrivacyManager
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for App on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class AppJvmTest {
    /**
     * Test app renders on JVM.
     */
    @Test
    fun testAppRenders() =
        runComposeUiTest {
            setAppLanguage("en")
            setContent {
                App()
            }

            onRoot().assertExists()
        }

    /**
     * Tests privacy shield overlay and session inactivity lockout behaviors.
     */
    @Test
    fun testAppPrivacyShieldAndLockout() =
        runComposeUiTest {
            setAppLanguage("en")
            currentAppPrivacyManager.unlock()

            setContent {
                App()
            }

            waitForIdle()

            // Trigger background obscuring
            val now = 1_000_000L
            currentAppPrivacyManager.onAppMovedToBackground(now)
            waitForIdle()

            onNodeWithText("ChartCam Security Shield", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription("Session Locked", useUnmergedTree = true).assertIsDisplayed()

            // Transition to foreground past lockout timeout (e.g. 70 seconds later)
            currentAppPrivacyManager.onAppMovedToForeground(now + 70_000L)
            waitForIdle()

            assertTrue(currentAppPrivacyManager.isLocked.value)
            onNodeWithText("Session Locked Due to Inactivity", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText("Unlock", useUnmergedTree = true).assertIsDisplayed()

            // Perform unlock
            onNodeWithText("Unlock", useUnmergedTree = true).performClick()
            waitForIdle()

            assertFalse(currentAppPrivacyManager.isLocked.value)
        }
}
