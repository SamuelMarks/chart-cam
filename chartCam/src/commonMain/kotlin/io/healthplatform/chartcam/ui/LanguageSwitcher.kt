/**
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
