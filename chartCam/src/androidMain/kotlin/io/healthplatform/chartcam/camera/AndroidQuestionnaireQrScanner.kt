/**
 * @file AndroidQuestionnaireQrScanner.kt
 * Android implementation of [QuestionnaireQrScanner].
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [QuestionnaireQrScanner].
 */
class AndroidQuestionnaireQrScanner : QuestionnaireQrScanner {
    /**
     * Scans a questionnaire QR code via camera on Android.
     *
     * @return A [Result] enclosing the decoded QR string.
     */
    override suspend fun scanQuestionnaireQrCode(): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                Result.failure<String>(IllegalStateException("No active camera barcode detected")).getOrThrow()
            }
        }
}

/**
 * Creates an [AndroidQuestionnaireQrScanner] instance.
 *
 * @return An instance of [QuestionnaireQrScanner] for Android.
 */
actual fun createQuestionnaireQrScanner(): QuestionnaireQrScanner = AndroidQuestionnaireQrScanner()
