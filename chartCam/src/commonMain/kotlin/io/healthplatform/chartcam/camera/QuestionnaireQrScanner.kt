/**
 * @file QuestionnaireQrScanner.kt
 * Declarations for air-gapped QR code questionnaire scanner.
 */
package io.healthplatform.chartcam.camera

/**
 * Multiplatform interface for scanning QR code questionnaire payloads via camera stream.
 */
interface QuestionnaireQrScanner {
    /**
     * Initiates a camera capture scan for a questionnaire QR code payload or chunk.
     *
     * @return A [Result] enclosing the scanned QR text payload, or failure.
     */
    suspend fun scanQuestionnaireQrCode(): Result<String>
}

/**
 * Creates a platform-specific [QuestionnaireQrScanner] instance.
 *
 * @return The platform-specific [QuestionnaireQrScanner].
 */
expect fun createQuestionnaireQrScanner(): QuestionnaireQrScanner
