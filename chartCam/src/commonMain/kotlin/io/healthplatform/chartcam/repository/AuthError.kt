/**
 * @file AuthError.kt
 * Contains declarations for AuthError.kt.
 */
package io.healthplatform.chartcam.repository

/**
 * Represents authentication and session errors in the ChartCam application.
 *
 * @param message The detailed error message.
 * @param cause The optional underlying cause of the error.
 */
sealed class AuthError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Thrown when an invalid username or password is provided.
     */
    class InvalidCredentials(
        message: String = "Invalid Credentials",
    ) : AuthError(message)

    /**
     * Thrown when there is an issue with the local hardware enclave or cryptographic operations.
     */
    class CryptoError(
        message: String,
        cause: Throwable? = null,
    ) : AuthError(message, cause)

    /**
     * Thrown when a session operation is attempted without a valid, active session.
     */
    class NoActiveSession(
        message: String = "No active session",
    ) : AuthError(message)

    /**
     * Thrown when there is an error interacting with the secure storage.
     */
    class StorageError(
        message: String,
        cause: Throwable? = null,
    ) : AuthError(message, cause)
}
