/**
 * @file LanguageSwitcherJsTest.kt
 * Contains declarations for LanguageSwitcherJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for LanguageSwitcher on JS.
 */
class LanguageSwitcherJsTest {
    /**
     * Test language state initialization on JS.
     */
    @Test
    fun testLanguageSwitcherJs() {
        val lang = currentLanguageState.value
        assertTrue(lang.isNotBlank())
    }
}
