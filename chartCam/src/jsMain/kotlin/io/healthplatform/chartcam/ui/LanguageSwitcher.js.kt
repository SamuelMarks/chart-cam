package io.healthplatform.chartcam.ui

private fun setNavigatorLanguage(language: String) {
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
    setNavigatorLanguage(language)
}

