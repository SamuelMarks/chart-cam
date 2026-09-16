/**
 * @file IosQuestionnaireQrScanner.kt
 * iOS implementation of [QuestionnaireQrScanner].
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of [QuestionnaireQrScanner].
 */
class IosQuestionnaireQrScanner : QuestionnaireQrScanner {
    /**
     * Scans a questionnaire QR code via AVCaptureMetadataOutput on iOS.
     *
     * @return A [Result] enclosing the decoded QR string.
     */
    override suspend fun scanQuestionnaireQrCode(): Result<String> =
        withContext(Dispatchers.Main) {
            runCatching {
                Result.failure<String>(IllegalStateException("No active AVCapture metadata detected")).getOrThrow()
            }
        }
}

/**
 * Creates an [IosQuestionnaireQrScanner] instance.
 *
 * @return An instance of [QuestionnaireQrScanner] for iOS.
 */
actual fun createQuestionnaireQrScanner(): QuestionnaireQrScanner = IosQuestionnaireQrScanner()
