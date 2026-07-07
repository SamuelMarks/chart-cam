/**
 * File defining the Android-specific implementation for changing the application language.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.AndroidAppInit
import java.util.Locale

/**
 * Changes the current application language on the Android platform.
 *
 * This function updates the default locale, updates the configuration of the current context
 * retrieved from [AndroidAppInit], and applies the new locale setting to the app's resources.
 *
 * @param language The ISO language code (e.g., "en", "es") to set as the new app language.
 */
@Suppress("DEPRECATION")
actual fun changeAppLanguage(language: String) {
    val locale = Locale.Builder().setLanguage(language).build()
    Locale.setDefault(locale)
    val context = AndroidAppInit.getContext()
    val config = context.resources.configuration
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
