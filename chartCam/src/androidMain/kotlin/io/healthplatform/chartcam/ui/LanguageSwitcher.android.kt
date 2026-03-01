package io.healthplatform.chartcam.ui

import java.util.Locale
import io.healthplatform.chartcam.AndroidAppInit

actual fun changeAppLanguage(language: String) {
    val locale = Locale(language)
    Locale.setDefault(locale)
    val context = AndroidAppInit.getContext()
    val config = context.resources.configuration
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
