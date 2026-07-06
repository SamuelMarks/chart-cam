/**
 * @file LanguageSwitcher.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) implementation for dynamically changing the application's language
 * by modifying the browser's `navigator` language properties via JavaScript interoperability.
 */
package io.healthplatform.chartcam.ui

import kotlin.js.toJsString

/**
 * Modifies the `navigator.language` and `navigator.languages` properties in the browser
 * environment to forcefully simulate a language change.
 *
 * @param language The new language code (e.g., "en", "es") represented as a [JsAny] JavaScript string.
 */
private fun setNavigatorLanguage(language: JsAny) {
    js(
        """
        Object.defineProperty(navigator, 'language', {
            value: language,
            configurable: true
        });
        Object.defineProperty(navigator, 'languages', {
            value: [language],
            configurable: true
        });
    """,
    )
}

/**
 * Changes the active language of the application on the Web (WasmJs) target.
 * Updates the browser's perceived navigator language.
 *
 * @param language The language code (e.g., "en", "es", "ja") to switch to.
 */
actual fun changeAppLanguage(language: String) {
    setNavigatorLanguage(language.toJsString())
}
