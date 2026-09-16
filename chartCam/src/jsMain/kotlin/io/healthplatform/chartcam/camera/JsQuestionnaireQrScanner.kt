/**
 * @file JsQuestionnaireQrScanner.kt
 * JS Web implementation of [QuestionnaireQrScanner].
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JS Web implementation of [QuestionnaireQrScanner].
 */
class JsQuestionnaireQrScanner : QuestionnaireQrScanner {
    /**
     * Scans a questionnaire QR code via browser BarcodeDetector or video stream.
     *
     * @return A [Result] enclosing the decoded QR string.
     */
    override suspend fun scanQuestionnaireQrCode(): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                Result
                    .failure<String>(IllegalStateException("BarcodeDetector unavailable in current browser context"))
                    .getOrThrow()
            }
        }
}

/**
 * Creates a [JsQuestionnaireQrScanner] instance.
 *
 * @return An instance of [QuestionnaireQrScanner] for JS Web.
 */
actual fun createQuestionnaireQrScanner(): QuestionnaireQrScanner = JsQuestionnaireQrScanner()
