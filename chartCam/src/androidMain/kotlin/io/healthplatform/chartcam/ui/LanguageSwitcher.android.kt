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
 * This function updates the default locale and creates a new configuration context
 * to apply the new locale setting.
 *
 * @param language The ISO language code (e.g., "en", "es") to set as the new app language.
 */
actual fun changeAppLanguage(language: String) {
    val locale = Locale.Builder().setLanguage(language).build()
    Locale.setDefault(locale)
    val context = AndroidAppInit.getContext()
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
}
