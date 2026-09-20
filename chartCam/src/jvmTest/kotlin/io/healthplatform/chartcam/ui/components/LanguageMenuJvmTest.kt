/**
 * @file LanguageMenuJvmTest.kt
 * Contains declarations for LanguageMenuJvmTest.kt.
 *
 * JVM Compose UI tests for the [LanguageMenu] component.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
     * Verifies that clicking the language menu button expands the menu items and changes the language across all options.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLanguageMenuSelection() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LanguageMenu(modifier = Modifier.testTag("custom_lang_menu"))
            }
            waitForIdle()

            onNodeWithTag("custom_lang_menu").assertIsDisplayed()
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).assertIsDisplayed()

            // 1. Select Spanish
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()
            onNodeWithText("Español").assertIsDisplayed()
            onNodeWithText("Español").performClick()
            waitForIdle()
            assertEquals("es", currentLanguageState.value)

            // 2. Select Japanese
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()
            onNodeWithText("日本語").assertIsDisplayed()
            onNodeWithText("日本語").performClick()
            waitForIdle()
            assertEquals("ja", currentLanguageState.value)

            // 3. Select Hebrew
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()
            onNodeWithText("עברית").assertIsDisplayed()
            onNodeWithText("עברית").performClick()
            waitForIdle()
            assertEquals("he", currentLanguageState.value)

            // 4. Select Traditional Chinese
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()
            onNodeWithText("繁體中文").assertIsDisplayed()
            onNodeWithText("繁體中文").performClick()
            waitForIdle()
            assertEquals("zh", currentLanguageState.value)

            // 5. Select English
            onNodeWithTag(TAG_LANGUAGE_MENU_BUTTON).performClick()
            waitForIdle()
            onNodeWithText("English").assertIsDisplayed()
            onNodeWithText("English").performClick()
            waitForIdle()
            assertEquals("en", currentLanguageState.value)
        }
    }
}
