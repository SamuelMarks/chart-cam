/**
 * @file LanguageSwitcherTest.kt
 * Unit tests for LanguageSwitcher functions and state management.
 */

package io.healthplatform.chartcam.ui

import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [LanguageSwitcherKt] covering RTL detection, Traditional Chinese detection,
 * layout direction resolution, and app language state transitions.
 */
class LanguageSwitcherTest {
    /**
     * Verifies setting application language updates currentLanguageState flow.
     */
    @Test
    fun testSetAppLanguageUpdatesState() {
        setAppLanguage("en")
        assertEquals("en", currentLanguageState.value)

        setAppLanguage("ja")
        assertEquals("ja", currentLanguageState.value)

        // Reset back to English
        setAppLanguage("en")
        assertEquals("en", currentLanguageState.value)
    }

    /**
     * Verifies RTL language detection across all supported RTL language codes and scripts.
     */
    @Test
    fun testIsRtlLanguage() {
        assertTrue(isRtlLanguage("ar"))
        assertTrue(isRtlLanguage("ar-SA"))
        assertTrue(isRtlLanguage("ar_EG"))
        assertTrue(isRtlLanguage("he"))
        assertTrue(isRtlLanguage("he-IL"))
        assertTrue(isRtlLanguage("iw"))
        assertTrue(isRtlLanguage("iw-IL"))
        assertTrue(isRtlLanguage("fa"))
        assertTrue(isRtlLanguage("fa-IR"))
        assertTrue(isRtlLanguage("ur"))
        assertTrue(isRtlLanguage("ur-PK"))
        assertTrue(isRtlLanguage("yi"))

        assertFalse(isRtlLanguage("en"))
        assertFalse(isRtlLanguage("en-US"))
        assertFalse(isRtlLanguage("es"))
        assertFalse(isRtlLanguage("ja"))
        assertFalse(isRtlLanguage("zh"))
        assertFalse(isRtlLanguage("fr"))
    }

    /**
     * Verifies getLayoutDirectionForLanguage maps RTL and LTR languages correctly.
     */
    @Test
    fun testGetLayoutDirectionForLanguage() {
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("ar"))
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("he"))
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("fa"))
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("ur"))
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("yi"))

        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("en"))
        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("ja"))
        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("zh-TW"))
        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("es"))
    }

    /**
     * Verifies Traditional Chinese and vertical text language detection rules.
     */
    @Test
    fun testTraditionalChineseAndVerticalTextRules() {
        // Traditional Chinese valid variants
        assertTrue(isTraditionalChinese("zh"))
        assertTrue(isTraditionalChinese("zh-TW"))
        assertTrue(isTraditionalChinese("zh_TW"))
        assertTrue(isTraditionalChinese("zh-HK"))
        assertTrue(isTraditionalChinese("zh-MO"))
        assertTrue(isTraditionalChinese("zh_MO"))
        assertTrue(isTraditionalChinese("zh-Hant"))
        assertTrue(isTraditionalChinese("zh-Hant-TW"))

        // Simplified variants must return false
        assertFalse(isTraditionalChinese("zh-CN"))
        assertFalse(isTraditionalChinese("zh-Hans"))
        assertFalse(isTraditionalChinese("zh-SG"))
        assertFalse(isTraditionalChinese("zh-Hans-TW"))

        // Non-Chinese languages with similar region tags
        assertFalse(isTraditionalChinese("en"))
        assertFalse(isTraditionalChinese("en-TW"))
        assertFalse(isTraditionalChinese("ja"))
        assertFalse(isTraditionalChinese("ko"))

        // Unrecognized Chinese dialect/variant without traditional tags
        assertFalse(isTraditionalChinese("zh-XYZ"))

        // Vertical text language delegates to Traditional Chinese
        assertTrue(isVerticalTextLanguage("zh-TW"))
        assertTrue(isVerticalTextLanguage("zh-Hant"))
        assertFalse(isVerticalTextLanguage("zh-CN"))
        assertFalse(isVerticalTextLanguage("en"))
    }
}
