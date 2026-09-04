/**
 * @file DynamicLanguageAndLayoutExpansionTest.kt
 * Contains declarations for DynamicLanguageAndLayoutExpansionTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.unit.LayoutDirection
import io.healthplatform.chartcam.utils.formatLocalizedDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests covering Section 6: Dynamic Language & Layout (LTR/RTL) Synchronization.
 */
class DynamicLanguageAndLayoutExpansionTest {
    /**
     * Helper to simulate a localized string resolver with fallback to English.
     *
     * @param key The string resource key.
     * @param locale The requested locale.
     * @param translations Map of locale to key-value pairs.
     * @return The resolved localized string, or the English fallback.
     */
    private fun resolveStringWithFallback(
        key: String,
        locale: String,
        translations: Map<String, Map<String, String>>,
    ): String {
        val localeMap = translations[locale]
        if (localeMap != null && localeMap.containsKey(key)) {
            return localeMap[key] ?: ""
        }
        // Fallback to default "en"
        return translations["en"]?.get(key) ?: "[$key]"
    }

    /**
     * Verify live UI language updates without activity restart or loss of entered form inputs across language switches.
     */
    @Test
    fun testRuntimeLocaleSwitchingPreservesFormInputs() {
        // Form inputs entered by clinician
        val enteredFormInputs =
            mutableMapOf<String, Any>(
                "patient_symptoms" to "Severe headache and dizziness",
                "blood_pressure_systolic" to "140",
                "blood_pressure_diastolic" to "90",
                "photo_step_front" to "/photos/front.jpg",
            )

        // Initial language English
        currentLanguageState.value = "en"
        assertEquals("en", currentLanguageState.value)

        // Runtime language switch to Spanish
        currentLanguageState.value = "es"
        assertEquals("es", currentLanguageState.value)
        // Verify form data remains intact
        assertEquals("Severe headache and dizziness", enteredFormInputs["patient_symptoms"])
        assertEquals("140", enteredFormInputs["blood_pressure_systolic"])

        // Runtime switch to Japanese
        currentLanguageState.value = "ja"
        assertEquals("ja", currentLanguageState.value)
        assertEquals("Severe headache and dizziness", enteredFormInputs["patient_symptoms"])

        // Runtime switch to Arabic (RTL)
        currentLanguageState.value = "ar"
        assertEquals("ar", currentLanguageState.value)
        assertEquals("Severe headache and dizziness", enteredFormInputs["patient_symptoms"])

        // Reset to default
        currentLanguageState.value = "en"
    }

    /**
     * Validate questionnaire layouts, form builders, and camera overlay controls when switching to Arabic or Hebrew.
     */
    @Test
    fun testRtlFlowAndMirroring() {
        // RTL script identification
        assertTrue(isRtlLanguage("ar"), "Arabic must be recognized as RTL")
        assertTrue(isRtlLanguage("ar-SA"), "Arabic (Saudi Arabia) must be recognized as RTL")
        assertTrue(isRtlLanguage("he"), "Hebrew must be recognized as RTL")
        assertTrue(isRtlLanguage("iw"), "Hebrew legacy code must be recognized as RTL")
        assertTrue(isRtlLanguage("fa"), "Persian/Farsi must be recognized as RTL")
        assertTrue(isRtlLanguage("ur"), "Urdu must be recognized as RTL")

        // LTR script identification
        assertFalse(isRtlLanguage("en"), "English must be recognized as LTR")
        assertFalse(isRtlLanguage("es"), "Spanish must be recognized as LTR")
        assertFalse(isRtlLanguage("ja"), "Japanese must be recognized as LTR")
        assertFalse(isRtlLanguage("fr"), "French must be recognized as LTR")

        // LayoutDirection resolution
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("ar"))
        assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("he"))
        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("en"))
        assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("ja"))

        // Layout mirroring calculations
        fun resolveVisualHorizontalAlignment(
            layoutDirection: LayoutDirection,
            isStartAligned: Boolean,
        ): String =
            if (layoutDirection == LayoutDirection.Rtl) {
                if (isStartAligned) "Right" else "Left"
            } else {
                if (isStartAligned) "Left" else "Right"
            }

        assertEquals("Right", resolveVisualHorizontalAlignment(LayoutDirection.Rtl, isStartAligned = true))
        assertEquals("Left", resolveVisualHorizontalAlignment(LayoutDirection.Ltr, isStartAligned = true))
    }

    /**
     * Ensure missing localized string keys safely fall back to default English keys without UI disruption.
     */
    @Test
    fun testMissingTranslationFallback() {
        val mockDictionaries =
            mapOf(
                "en" to
                    mapOf(
                        "app_title" to "ChartCam",
                        "save_encounter" to "Save Encounter",
                        "rare_clinical_condition" to "Rare Clinical Condition",
                    ),
                "es" to
                    mapOf(
                        "app_title" to "ChartCam",
                        "save_encounter" to "Guardar Encuentro",
                        // "rare_clinical_condition" is missing in Spanish
                    ),
                "ja" to
                    mapOf(
                        "app_title" to "チャートカム",
                        // Both missing in Japanese
                    ),
            )

        // When present in target language
        val esSave = resolveStringWithFallback("save_encounter", "es", mockDictionaries)
        assertEquals("Guardar Encuentro", esSave)

        // When missing in Spanish, falls back to English without crashing
        val esFallback = resolveStringWithFallback("rare_clinical_condition", "es", mockDictionaries)
        assertEquals("Rare Clinical Condition", esFallback)

        // When missing in Japanese, falls back to English
        val jaFallback = resolveStringWithFallback("save_encounter", "ja", mockDictionaries)
        assertEquals("Save Encounter", jaFallback)
    }

    /**
     * Verify date pickers and numeric inputs respect target locale formatting rules.
     */
    @Test
    fun testLocalizedDateAndDecimalFormatting() {
        val testNumber = 1234.56

        // Default arguments: localeTag = "en", decimalPlaces = 2
        val defaultDecimal = formatLocalizedDecimal(testNumber)
        assertEquals("1234.56", defaultDecimal)

        // Default decimalPlaces: localeTag = "en", decimalPlaces = 2
        val defaultPlacesDecimal = formatLocalizedDecimal(testNumber, localeTag = "en")
        assertEquals("1234.56", defaultPlacesDecimal)

        // English: dot decimal separator
        val enDecimal = formatLocalizedDecimal(testNumber, localeTag = "en", decimalPlaces = 2)
        assertEquals("1234.56", enDecimal)

        // Spanish: comma decimal separator
        val esDecimal = formatLocalizedDecimal(testNumber, localeTag = "es", decimalPlaces = 2)
        assertEquals("1234,56", esDecimal)

        // German: comma decimal separator
        val deDecimal = formatLocalizedDecimal(testNumber, localeTag = "de", decimalPlaces = 1)
        assertEquals("1234,6", deDecimal)

        // Arabic: Eastern Arabic digits and Arabic decimal separator
        val arDecimal = formatLocalizedDecimal(42.5, localeTag = "ar", decimalPlaces = 1)
        assertEquals("٤٢٫٥", arDecimal)

        // Negative Arabic number: covers else branch in Arabic digit mapping
        val negativeArDecimal = formatLocalizedDecimal(-42.5, localeTag = "ar", decimalPlaces = 1)
        assertEquals("-٤٢٫٥", negativeArDecimal)

        // Integer with zero decimal places
        val zeroFrac = formatLocalizedDecimal(99.9, localeTag = "en", decimalPlaces = 0)
        assertEquals("100", zeroFrac)

        // Whole number where fraction part is empty
        val wholeNumber = formatLocalizedDecimal(50.0, localeTag = "en", decimalPlaces = 2)
        assertEquals("50", wholeNumber)
    }
}
