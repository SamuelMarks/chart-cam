/**
 * @file LanguageMenuTest.kt
 * Contains declarations for LanguageMenuTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Common test suite for LanguageMenu component tags and metadata.
 */
class LanguageMenuTest {
    /**
     * Verifies that the test tag for LanguageMenu anchor is properly defined.
     */
    @Test
    fun testLanguageMenuTag() {
        assertEquals("language_menu_button", TAG_LANGUAGE_MENU_BUTTON)
    }
}
