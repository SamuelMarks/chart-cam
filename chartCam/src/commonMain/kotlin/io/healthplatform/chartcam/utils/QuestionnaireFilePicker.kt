/**
 * @file QuestionnaireFilePicker.kt
 * Contains declarations for QuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

/**
 * Multiplatform interface for selecting and reading offline questionnaire JSON files.
 */
interface QuestionnaireFilePicker {
    /**
     * Prompts the user to pick an offline questionnaire JSON file from the local file system.
     *
     * @return A [Result] enclosing the JSON string content, or failure.
     */
    suspend fun pickQuestionnaireFile(): Result<String>
}

/**
 * Creates a platform-specific [QuestionnaireFilePicker] instance.
 *
 * @return The [QuestionnaireFilePicker].
 */
expect fun createQuestionnaireFilePicker(): QuestionnaireFilePicker
