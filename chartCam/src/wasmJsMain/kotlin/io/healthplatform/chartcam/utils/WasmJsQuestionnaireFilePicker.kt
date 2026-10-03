/**
 * @file WasmJsQuestionnaireFilePicker.kt
 * Contains declarations for WasmJsQuestionnaireFilePicker.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.utils

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PICK_FILE_JS =
    "(onSuccess, onCancel, onError) => { " +
        "const input = document.createElement('input'); " +
        "input.type = 'file'; " +
        "input.accept = '.json,application/json'; " +
        "let handled = false; " +
        "input.onchange = (e) => { " +
        "  if(handled) return; " +
        "  handled = true; " +
        "  const file = e.target.files[0]; " +
        "  if(file) { " +
        "    const reader = new FileReader(); " +
        "    reader.onload = (re) => { onSuccess(re.target.result); }; " +
        "    reader.onerror = () => { onError(); }; " +
        "    reader.readAsText(file); " +
        "  } else { " +
        "    onCancel(); " +
        "  } " +
        "}; " +
        "const onFocus = () => { " +
        "  window.setTimeout(() => { " +
        "    if(!handled && input.files.length === 0) { " +
        "      handled = true; " +
        "      onCancel(); " +
        "    } " +
        "    window.removeEventListener('focus', onFocus); " +
        "  }, 500); " +
        "}; " +
        "window.addEventListener('focus', onFocus); " +
        "input.click(); " +
        "}"

@JsFun(PICK_FILE_JS)
private external fun pickFileJs(
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit,
)

@JsFun("() => window._chartcam_is_test === true")
private external fun isChartCamTest(): Boolean

/**
 * WebAssembly (WasmJs) implementation of [QuestionnaireFilePicker].
 */
class WasmJsQuestionnaireFilePicker : QuestionnaireFilePicker {
    /**
     * Reads a questionnaire JSON string via an HTML file input dialog.
     *
     * @return A [Result] enclosing the JSON string content or failure.
     */
    override suspend fun pickQuestionnaireFile(): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val isTest = isChartCamTest()
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

            var resumed = false
            pickFileJs(
                onSuccess = { content ->
                    if (!resumed) {
                        resumed = true
                        continuation.resume(Result.success(content))
                    }
                },
                onCancel = {
                    if (!resumed) {
                        resumed = true
                        continuation.resume(Result.failure(FilePickerError.UserCancelled))
                    }
                },
                onError = {
                    if (!resumed) {
                        resumed = true
                        continuation.resume(Result.failure(FilePickerError.ReadFailed))
                    }
                },
            )
        }
}

/**
 * Creates a WasmJs-specific [QuestionnaireFilePicker].
 *
 * @return An instance of [WasmJsQuestionnaireFilePicker].
 */
actual fun createQuestionnaireFilePicker(): QuestionnaireFilePicker = WasmJsQuestionnaireFilePicker()
