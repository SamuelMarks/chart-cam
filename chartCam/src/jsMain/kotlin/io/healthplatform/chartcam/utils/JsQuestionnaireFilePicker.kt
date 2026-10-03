/**
 * @file JsQuestionnaireFilePicker.kt
 * Contains declarations for JsQuestionnaireFilePicker.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import kotlin.coroutines.resume

/**
 * JS Web implementation of [QuestionnaireFilePicker].
 */
class JsQuestionnaireFilePicker : QuestionnaireFilePicker {
    /**
     * Dynamically creates an HTML file input to prompt the user for a JSON file.
     *
     * @return A [Result] enclosing the JSON string content or failure.
     */
    override suspend fun pickQuestionnaireFile(): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val isTest =
                kotlinx.browser.window
                    .asDynamic()
                    ._chartcam_is_test == true
            if (isTest) {
                val mockContent =
                    kotlinx.browser.window.localStorage
                        .getItem("chartcam_imported_questionnaire")
                if (mockContent != null) {
                    continuation.resume(Result.success(mockContent))
                } else {
                    continuation.resume(Result.failure(FilePickerError.UserCancelled))
                }
                return@suspendCancellableCoroutine
            }

            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = ".json,application/json"

            var hasResponded = false

            input.addEventListener("change", { _ ->
                if (hasResponded) return@addEventListener
                val file = input.files?.item(0)
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = { e ->
                        if (!hasResponded) {
                            hasResponded = true
                            val content = e.target.asDynamic().result as String
                            continuation.resume(Result.success(content))
                        }
                    }
                    reader.onerror = { _ ->
                        if (!hasResponded) {
                            hasResponded = true
                            continuation.resume(Result.failure(FilePickerError.ReadFailed))
                        }
                    }
                    reader.readAsText(file)
                } else {
                    hasResponded = true
                    continuation.resume(Result.failure(FilePickerError.UserCancelled))
                }
            })

            // To detect cancellation, it's a bit tricky in browsers, but we'll try focus event on window
            val windowFocusListener =
                object : org.w3c.dom.events.EventListener {
                    /**
                     * Handles the focus event to detect cancellation.
                     *
                     * @param event The focus event.
                     */
                    override fun handleEvent(event: Event) {
                        kotlinx.browser.window.setTimeout({
                            if (!hasResponded && input.files?.length == 0) {
                                hasResponded = true
                                continuation.resume(Result.failure(FilePickerError.UserCancelled))
                            }
                            kotlinx.browser.window.removeEventListener("focus", this)
                        }, 500)
                    }
                }
            kotlinx.browser.window.addEventListener("focus", windowFocusListener)

            input.click()

            continuation.invokeOnCancellation {
                if (!hasResponded) {
                    hasResponded = true
                    kotlinx.browser.window.removeEventListener("focus", windowFocusListener)
                }
            }
        }
}

/**
 * Creates a JS-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [JsQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = JsQuestionnaireFilePicker()
