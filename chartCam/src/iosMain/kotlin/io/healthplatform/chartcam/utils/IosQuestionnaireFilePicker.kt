/**
 * @file IosQuestionnaireFilePicker.kt
 * Contains declarations for IosQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS implementation of [QuestionnaireFilePicker].
 */
class IosQuestionnaireFilePicker : QuestionnaireFilePicker {
    /**
     * Reads a questionnaire JSON file from the application Documents directory.
     *
     * @return A [Result] enclosing the file contents or failure.
     */
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override suspend fun pickQuestionnaireFile(): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
                val docDir = paths.firstOrNull() as? String ?: error("Documents directory unavailable")
                val fileManager = NSFileManager.defaultManager
                val contents = fileManager.contentsOfDirectoryAtPath(docDir, null)
                val jsonFile =
                    contents
                        ?.mapNotNull { it as? String }
                        ?.firstOrNull { it.startsWith("questionnaire_") && it.endsWith(".json") }
                        ?: error("No questionnaire file found in Documents directory")

                val fullPath = "$docDir/$jsonFile"
                val content = NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null)
                content ?: error("Failed to read questionnaire file contents")
            }
        }
}

/**
 * Creates an iOS-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [IosQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = IosQuestionnaireFilePicker()
