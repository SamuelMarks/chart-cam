/**
 * @file LanguageSwitcher.jvm.kt
 * Language switching utility for the JVM platform.
 */
package io.healthplatform.chartcam.ui

import java.util.Locale

/**
 * Changes the default application language for the JVM by setting the default [Locale].
 *
 * @param language The ISO 639 language code (e.g., "en", "es", "ja") to switch to.
 */
actual fun changeAppLanguage(language: String) {
    Locale.setDefault(Locale.Builder().setLanguage(language).build())
}
