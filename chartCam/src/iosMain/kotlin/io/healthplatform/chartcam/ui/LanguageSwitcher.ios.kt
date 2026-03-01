package io.healthplatform.chartcam.ui

import platform.Foundation.NSUserDefaults

actual fun changeAppLanguage(language: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(language), "AppleLanguages")
    NSUserDefaults.standardUserDefaults.synchronize()
}
