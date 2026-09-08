/**
 * @file LanguageMenuJvmTest.kt
 * Contains declarations for LanguageMenuJvmTest.kt.
 *
 * JVM Compose UI tests for the [LanguageMenu] component.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.ui.setAppLanguage
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Test suite validating UI interactions and language updates in [LanguageMenu].
 */
class LanguageMenuJvmTest {
    /**
     * Verifies that clicking the language menu button expands the menu items and changes the language.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLanguageMenuSelection() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LanguageMenu()
            }
            waitForIdle()

            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).assertIsDisplayed()
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()

            onNodeWithText("Español").assertIsDisplayed()
            onNodeWithText("Español").performClick()
            waitForIdle()

            assertEquals("es", currentLanguageState.value)
        }
    }
}
