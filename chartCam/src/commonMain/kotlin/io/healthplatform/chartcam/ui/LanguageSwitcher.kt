package io.healthplatform.chartcam.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val currentLanguageState = MutableStateFlow("en")

expect fun changeAppLanguage(language: String)

fun setAppLanguage(language: String) {
    currentLanguageState.value = language
    changeAppLanguage(language)
}
