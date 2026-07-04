package io.healthplatform.chartcam.ui

import kotlin.js.toJsString

private fun setNavigatorLanguage(language: JsAny) {
    js("""
        Object.defineProperty(navigator, 'language', {
            value: language,
            configurable: true
        });
        Object.defineProperty(navigator, 'languages', {
            value: [language],
            configurable: true
        });
    """)
}

actual fun changeAppLanguage(language: String) {
    setNavigatorLanguage(language.toJsString())
}

