/**
 * @file LanguageSwitcherWasmJsTest.kt
 * Contains declarations for LanguageSwitcherWasmJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for LanguageSwitcher on WasmJS.
 */
class LanguageSwitcherWasmJsTest {
    /**
     * Test language state initialization on WasmJS.
     */
    @Test
    fun testLanguageSwitcherWasmJs() {
        val lang = currentLanguageState.value
        assertTrue(lang.isNotBlank())
    }
}
