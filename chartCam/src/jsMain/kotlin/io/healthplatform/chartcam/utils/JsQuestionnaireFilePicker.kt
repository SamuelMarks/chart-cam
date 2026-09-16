/**
 * @file JsQuestionnaireFilePicker.kt
 * Contains declarations for JsQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.localStorage

/**
 * JS implementation of [QuestionnaireFilePicker].
 */
class JsQuestionnaireFilePicker : QuestionnaireFilePicker {
    /**
     * Reads a questionnaire JSON string from browser local storage or cached session.
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
 * Creates a JS-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [JsQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = JsQuestionnaireFilePicker()
