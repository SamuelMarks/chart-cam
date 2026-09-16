/**
 * @file AndroidQuestionnaireFilePicker.kt
 * Contains declarations for AndroidQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of [QuestionnaireFilePicker].
 *
 * @param searchDirs The directories searched for questionnaire files.
 */
class AndroidQuestionnaireFilePicker(
    private val searchDirs: List<File> = listOf(File(".")),
) : QuestionnaireFilePicker {
    /**
     * Picks a questionnaire JSON file from the local app sandbox or external storage.
     *
     * @return A [Result] enclosing the JSON content or failure.
     */
    override suspend fun pickQuestionnaireFile(): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val candidateFile =
                    searchDirs
                        .flatMap { dir ->
                            val files = dir.listFiles()
                            if (files != null) files.toList() else emptyList()
                        }.firstOrNull { it.isFile && it.name.startsWith("questionnaire_") && it.name.endsWith(".json") }
                if (candidateFile != null) {
                    candidateFile.readText()
                } else {
                    error("No local questionnaire JSON file found in storage")
                }
            }
        }
}

/**
 * Creates an Android-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [AndroidQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = AndroidQuestionnaireFilePicker()
