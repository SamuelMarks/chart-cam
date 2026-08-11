/**
 * @file LanguageSwitcherJvmTest.kt
 * Contains declarations for LanguageSwitcherJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals

/**
 * Test class for LanguageSwitcher on JVM.
 */
class LanguageSwitcherJvmTest {
    /**
     * Tests changing app language.
     */
    @Test
    fun testChangeAppLanguage() {
        val originalLocale = Locale.getDefault()

        try {
            setAppLanguage("ja")
            assertEquals("ja", currentLanguageState.value)
            assertEquals("ja", Locale.getDefault().language)

            setAppLanguage("es")
            assertEquals("es", currentLanguageState.value)
            assertEquals("es", Locale.getDefault().language)
        } finally {
            Locale.setDefault(originalLocale)
            currentLanguageState.value = originalLocale.language
        }
    }
}
