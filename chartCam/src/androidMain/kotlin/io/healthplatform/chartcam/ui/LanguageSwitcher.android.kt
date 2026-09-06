/**
 * @file LanguageSwitcher.android.kt
 * Contains declarations for LanguageSwitcher.android.kt.
 *
 * File defining the Android-specific implementation for changing the application language.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.AndroidAppInit
import java.util.Locale

/**
 * Changes the current application language on the Android platform.
 *
 * This function updates the default locale and updates the active resources configuration
 * to ensure application-level resource lookups react immediately.
 *
 * @param language The IETF BCP 47 language tag (e.g., "en", "es", "ja", "he", "zh-TW") to set as the new app language.
 */
@Suppress("DEPRECATION")
actual fun changeAppLanguage(language: String) {
    val locale = Locale.forLanguageTag(language)
    Locale.setDefault(locale)
    val context = AndroidAppInit.getContext()
    val resources = context.resources
    val config = resources.configuration
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
    context.createConfigurationContext(config)
}
