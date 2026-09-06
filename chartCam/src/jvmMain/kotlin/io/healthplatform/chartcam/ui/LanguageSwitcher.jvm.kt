/**
 * @file LanguageSwitcher.jvm.kt
 * Language switching utility for the JVM platform.
 */
package io.healthplatform.chartcam.ui

import java.util.Locale

/**
 * Changes the default application language for the JVM by setting the default [Locale].
 *
 * @param language The IETF BCP 47 language tag (e.g., "en", "es", "ja", "he", "zh-TW") to switch to.
 */
actual fun changeAppLanguage(language: String) {
    val locale = Locale.forLanguageTag(language)
    Locale.setDefault(locale)
}
