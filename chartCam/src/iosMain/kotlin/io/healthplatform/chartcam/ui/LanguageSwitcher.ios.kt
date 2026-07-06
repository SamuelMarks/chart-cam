/**
 * iOS implementation for changing the application language.
 */
package io.healthplatform.chartcam.ui

import platform.Foundation.NSUserDefaults

/**
 * Changes the application language on iOS.
 *
 * This function updates the "AppleLanguages" key in the standard user defaults
 * to the specified language and synchronizes the defaults. The app may need to be
 * restarted for the changes to take full effect.
 *
 * @param language The language code to set (e.g., "en", "es", "ja").
 */
actual fun changeAppLanguage(language: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(language), "AppleLanguages")
    NSUserDefaults.standardUserDefaults.synchronize()
}
