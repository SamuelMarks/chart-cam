/**
 * @file LanguageSwitcher.kt
 * Contains declarations for LanguageSwitcher.kt.
 *
 * Provides functionality for managing and changing the application's language state.
 */
package io.healthplatform.chartcam.ui

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A mutable state flow holding the currently selected language code (e.g., "en", "es", "ja").
 * This flow can be collected by UI components to react to language changes.
 */
val currentLanguageState = MutableStateFlow("en")

/**
 * Platform-specific implementation for updating the application's language configuration.
 *
 * @param language The IETF BCP 47 language tag (e.g., "en-US", "ja") to switch to.
 */
expect fun changeAppLanguage(language: String)

/**
 * Sets the application language by updating the internal state flow and invoking
 * the platform-specific language change routine.
 *
 * @param language The IETF BCP 47 language tag to apply globally.
 */
fun setAppLanguage(language: String) {
    currentLanguageState.value = language
    changeAppLanguage(language)
}

/**
 * Determines whether a given language code corresponds to a right-to-left (RTL) script.
 *
 * @param language The ISO 639 language code or BCP 47 language tag (e.g. "ar", "he", "fa", "ur").
 * @return True if RTL, false otherwise.
 */
fun isRtlLanguage(language: String): Boolean {
    val lang = language.lowercase().split("-", "_").first()
    return lang in setOf("ar", "he", "iw", "fa", "ur", "yi")
}

/**
 * Returns the appropriate [androidx.compose.ui.unit.LayoutDirection] for a given language code.
 *
 * @param language The language tag.
 * @return LayoutDirection.Rtl for RTL languages, LayoutDirection.Ltr otherwise.
 */
fun getLayoutDirectionForLanguage(language: String): androidx.compose.ui.unit.LayoutDirection =
    if (isRtlLanguage(language)) {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }
