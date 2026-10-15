/**
 * @file LocaleProvider.kt
 * Provides a domain-layer localization state holder free of UI dependencies.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for accessing the active BCP-47 application language tag in domain and repository layers.
 */
fun interface LocaleProvider {
    /**
     * Returns the currently active BCP-47 language tag (e.g., "en", "es", "ja", "he", "zh-TW").
     *
     * @return The active language tag string.
     */
    fun currentLanguage(): String
}

/**
 * Shared domain-level reactive state flow holding the currently selected language code.
 */
val appLocaleState: MutableStateFlow<String> = MutableStateFlow("en")

/**
 * Default [LocaleProvider] reading from [appLocaleState].
 */
object DefaultLocaleProvider : LocaleProvider {
    /**
     * Read-only [StateFlow] of the current language tag.
     */
    val languageFlow: StateFlow<String> = appLocaleState

    /**
     * Returns the currently active language tag from [appLocaleState].
     *
     * @return The active BCP-47 language tag string.
     */
    override fun currentLanguage(): String = appLocaleState.value
}
