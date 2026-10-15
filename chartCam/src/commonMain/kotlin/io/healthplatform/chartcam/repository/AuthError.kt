/**
 * @file AuthError.kt
 * Contains declarations for AuthError.kt.
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.utils.ChartCamDomainError

/**
 * Represents authentication and session errors in the ChartCam application.
 *
 * @param message The detailed error message.
 * @param cause The optional underlying cause of the error.
 */
sealed class AuthError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause),
    ChartCamDomainError {
    /**
     * Thrown when an invalid username or password is provided.
     *
     * @param message Human-readable description of the invalid credentials failure.
     */
    class InvalidCredentials(
        message: String = "Invalid Credentials",
    ) : AuthError(message)

    /**
     * Thrown when there is an issue with the local hardware enclave or cryptographic operations.
     *
     * @param message Human-readable description of the cryptographic error.
     * @param cause Optional underlying throwable.
     */
    class CryptoError(
        message: String,
        cause: Throwable? = null,
    ) : AuthError(message, cause)

    /**
     * Thrown when a session operation is attempted without a valid, active session.
     *
     * @param message Human-readable description of the missing session failure.
     */
    class NoActiveSession(
        message: String = "No active session",
    ) : AuthError(message)

    /**
     * Thrown when there is an error interacting with the secure storage.
     *
     * @param message Human-readable description of the storage error.
     * @param cause Optional underlying throwable.
     */
    class StorageError(
        message: String,
        cause: Throwable? = null,
    ) : AuthError(message, cause)
}
