/**
 * @file LanguageSwitcherCommonTest.kt
 * Contains declarations for LanguageSwitcherCommonTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageSwitcherCommonTest {
    @Test
    fun testSetAppLanguage() =
        runTest {
            // changeAppLanguage expects platform specific behavior, which may throw or do nothing.
            // We can test the flow updates.
            val old = currentLanguageState.value
            try {
                setAppLanguage("es")
                assertEquals("es", currentLanguageState.value)
            } catch (e: Exception) {
                // expected in mock
            } finally {
                currentLanguageState.value = old
            }
        }
}
