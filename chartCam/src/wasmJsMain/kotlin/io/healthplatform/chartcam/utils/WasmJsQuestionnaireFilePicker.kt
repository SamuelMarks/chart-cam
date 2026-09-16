/**
 * @file WasmJsQuestionnaireFilePicker.kt
 * Contains declarations for WasmJsQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.localStorage

/**
 * WebAssembly (WasmJs) implementation of [QuestionnaireFilePicker].
 */
class WasmJsQuestionnaireFilePicker : QuestionnaireFilePicker {
    /**
     * Reads a questionnaire JSON string from browser storage.
     *
     * @return A [Result] enclosing the JSON string content or failure.
     */
    override suspend fun pickQuestionnaireFile(): Result<String> =
        runCatching {
            val content = localStorage.getItem("chartcam_imported_questionnaire")
            content ?: error("No staged questionnaire file in browser storage")
        }
}

/**
 * Creates a WasmJs-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [WasmJsQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = WasmJsQuestionnaireFilePicker()
