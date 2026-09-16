/**
 * @file JvmQuestionnaireFilePicker.kt
 * Contains declarations for JvmQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop JVM implementation of [QuestionnaireFilePicker].
 *
 * @param searchDirs The directories searched for questionnaire files.
 */
class JvmQuestionnaireFilePicker(
    private val searchDirs: List<File> = listOf(File(".")),
) : QuestionnaireFilePicker {
    /**
     * Picks a questionnaire JSON file from the local file system.
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
                    error("No local questionnaire JSON file found in working directory")
                }
            }
        }
}

/**
 * Creates a JVM-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [JvmQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = JvmQuestionnaireFilePicker()
