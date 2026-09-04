/**
 * @file LanguageSwitcher.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation for dynamically changing the application's language
 * by modifying the browser's `navigator` language properties via JavaScript interoperability.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.ui

import kotlin.js.toJsString

private const val SET_NAVIGATOR_LANGUAGE_JS =
    "(language) => { Object.defineProperty(navigator, 'language', { value: language, configurable: true }); " +
        "Object.defineProperty(navigator, 'languages', { value: [language], configurable: true }); }"

/**
 * Modifies the `navigator.language` and `navigator.languages` properties in the browser
 * environment to forcefully simulate a language change.
 *
 * @param language The new language code (e.g., "en", "es") represented as a [JsAny] JavaScript string.
 */
@JsFun(SET_NAVIGATOR_LANGUAGE_JS)
private external fun setNavigatorLanguage(language: JsAny)

/**
 * Changes the active language of the application on the Web (WasmJs) target.
 * Updates the browser's perceived navigator language.
 *
 * @param language The language code (e.g., "en", "es", "ja") to switch to.
 */
actual fun changeAppLanguage(language: String) {
    setNavigatorLanguage(language.toJsString())
}
