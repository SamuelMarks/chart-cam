/**
 * @file LanguageSwitcherIosTest.kt
 * Contains declarations for LanguageSwitcherIosTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates iOS localization setup.
 */
class LanguageSwitcherIosTest {
    /** Verifies current language state is initialized on iOS. */
    @Test
    fun testLanguageSwitcherIos() {
        val lang = currentLanguageState.value
        assertTrue(lang.isNotEmpty())
    }
}
