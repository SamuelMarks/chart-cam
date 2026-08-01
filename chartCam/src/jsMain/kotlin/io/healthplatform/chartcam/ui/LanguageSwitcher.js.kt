/**
 * @file LanguageSwitcher.js.kt
 * Provides JS-specific localization utilities.
 */
package io.healthplatform.chartcam.ui

/**
 * Sets the browser's navigator language using a JS snippet.
 *
 * @param language The locale identifier to set (e.g., "en", "es").
 */
private fun setNavigatorLanguage(language: String) {
    val applyLang: dynamic =
        js(
            """
            (function(lang) {
                Object.defineProperty(navigator, 'language', {
                    value: lang,
                    configurable: true
                });
                Object.defineProperty(navigator, 'languages', {
                    value: [lang],
                    configurable: true
                });
            })
            """,
        )
    applyLang(language)
}

/**
 * Changes the application language in the current environment.
 * In a web browser context, this overwrites the `navigator.language` properties.
 *
 * @param language The string identifying the desired locale.
 */
actual fun changeAppLanguage(language: String) {
    setNavigatorLanguage(language)
}
