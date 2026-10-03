/**
 * @file QuestionnaireQrScanner.kt
 * Declarations for air-gapped QR code questionnaire scanner.
 */
package io.healthplatform.chartcam.camera

/**
 * Errors that can occur during QR code scanning operations.
 */
sealed class QrScannerError(
    message: String,
) : Throwable(message) {
    /** QR scanning is unavailable on this platform. */
    data object ScannerUnavailable : QrScannerError("QR scanning is unavailable on this platform")

    /** QR code parsing or reading failed. */
    data class ReadFailed(
        override val message: String,
    ) : QrScannerError(message)
}

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
