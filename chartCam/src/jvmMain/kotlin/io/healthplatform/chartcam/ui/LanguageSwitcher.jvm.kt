package io.healthplatform.chartcam.ui

import java.util.Locale

actual fun changeAppLanguage(language: String) {
    Locale.setDefault(Locale(language))
}
