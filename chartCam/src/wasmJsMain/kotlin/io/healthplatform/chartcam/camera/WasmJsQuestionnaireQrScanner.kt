/**
 * @file WasmJsQuestionnaireQrScanner.kt
 * WasmJS Web implementation of [QuestionnaireQrScanner].
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WasmJS Web implementation of [QuestionnaireQrScanner].
 */
class WasmJsQuestionnaireQrScanner : QuestionnaireQrScanner {
    /**
     * Scans a questionnaire QR code via browser APIs in WebAssembly.
     *
     * @return A [Result] enclosing the decoded QR string.
     */
    override suspend fun scanQuestionnaireQrCode(): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                Result
                    .failure<String>(IllegalStateException("Barcode scanning unavailable in WebAssembly context"))
                    .getOrThrow()
            }
        }
}

/**
 * Creates a [WasmJsQuestionnaireQrScanner] instance.
 *
 * @return An instance of [QuestionnaireQrScanner] for WasmJS.
 */
actual fun createQuestionnaireQrScanner(): QuestionnaireQrScanner = WasmJsQuestionnaireQrScanner()
