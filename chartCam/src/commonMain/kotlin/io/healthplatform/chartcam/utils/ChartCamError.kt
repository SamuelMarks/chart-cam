/**
 * @file ChartCamError.kt
 * Defines the unified error contract and sealed hierarchy for railway-oriented [Result] error percolation
 * across all layers of the ChartCam application.
 */
package io.healthplatform.chartcam.utils

/**
 * Common marker interface implemented by all domain, storage, cryptographic, hardware, and validation
 * exceptions in the ChartCam application across all packages.
 */
interface ChartCamDomainError {
    /**
     * Human-readable diagnostic description of the failure.
     */
    val message: String?
}

/**
 * Root sealed exception hierarchy for core domain, storage, cryptographic, hardware, and validation
 * failures in ChartCam. All [Result] failures percolated across domain and repository boundaries
 * implement [ChartCamDomainError] or extend [ChartCamError].
 *
 * @param message Human-readable diagnostic description of the failure.
 * @param cause Optional underlying cause throwable.
 */
sealed class ChartCamError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause),
    ChartCamDomainError {
    /**
     * Error indicating a local file or database storage failure.
     *
     * @param message Description of the storage failure.
     * @param cause Optional underlying cause.
     */
    class StorageFailure(
        message: String,
        cause: Throwable? = null,
    ) : ChartCamError(message, cause)

    /**
     * Error indicating a cryptographic key derivation, encryption, or decryption failure.
     *
     * @param message Description of the cryptographic failure.
     * @param cause Optional underlying cause.
     */
    class CryptoFailure(
        message: String,
        cause: Throwable? = null,
    ) : ChartCamError(message, cause)

    /**
     * Error indicating a FHIR resource or SDC questionnaire validation failure.
     *
     * @param message Description of the validation failure.
     * @param cause Optional underlying cause.
     */
    class ValidationFailure(
        message: String,
        cause: Throwable? = null,
    ) : ChartCamError(message, cause)

    /**
     * Error indicating a hardware camera, audio recorder, or sensor failure.
     *
     * @param message Description of the hardware failure.
     * @param cause Optional underlying cause.
     */
    class HardwareFailure(
        message: String,
        cause: Throwable? = null,
    ) : ChartCamError(message, cause)

    /**
     * Error indicating an invalid domain state or capacity limit violation.
     *
     * @param message Description of the state violation.
     * @param cause Optional underlying cause.
     */
    class StateViolation(
        message: String,
        cause: Throwable? = null,
    ) : ChartCamError(message, cause)
}
