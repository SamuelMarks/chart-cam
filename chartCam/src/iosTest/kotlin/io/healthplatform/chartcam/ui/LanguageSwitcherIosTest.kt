/**
 * @file LanguageSwitcherIosTest.kt
 * Contains declarations for LanguageSwitcherIosTest.kt.
 */
package io.healthplatform.chartcam.ui

import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates iOS localization setup and language switching.
 */
class LanguageSwitcherIosTest {
    /**
     * Verifies current language state is initialized on iOS.
     */
    @Test
    fun testLanguageSwitcherIos() {
        val lang = currentLanguageState.value
        assertTrue(lang.isNotEmpty())
    }

    /**
     * Verifies that changeAppLanguage updates AppleLanguages in NSUserDefaults on iOS.
     */
    @Test
    fun testChangeAppLanguageIos() {
        changeAppLanguage("es")
        val defaults = NSUserDefaults.standardUserDefaults
        val appleLanguages = defaults.stringArrayForKey("AppleLanguages")?.filterIsInstance<String>()
        assertTrue(appleLanguages != null && appleLanguages.contains("es"))

        // Reset to English
        changeAppLanguage("en")
        val resetLanguages = defaults.stringArrayForKey("AppleLanguages")?.filterIsInstance<String>()
        assertTrue(resetLanguages != null && resetLanguages.contains("en"))
    }
}
