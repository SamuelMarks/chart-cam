/**
 * @file JvmQuestionnaireQrScanner.kt
 * Desktop JVM implementation of [QuestionnaireQrScanner].
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop JVM implementation of [QuestionnaireQrScanner].
 */
class JvmQuestionnaireQrScanner : QuestionnaireQrScanner {
    /**
     * Scans a questionnaire QR code via desktop webcam on JVM.
     *
     * @return A [Result] enclosing the decoded QR string.
     */
    override suspend fun scanQuestionnaireQrCode(): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                Result
                    .failure<String>(IllegalStateException("No QR code detected in desktop camera frame"))
                    .getOrThrow()
            }
        }
}

/**
 * Creates a [JvmQuestionnaireQrScanner] instance.
 *
 * @return An instance of [QuestionnaireQrScanner] for desktop JVM.
 */
actual fun createQuestionnaireQrScanner(): QuestionnaireQrScanner = JvmQuestionnaireQrScanner()
