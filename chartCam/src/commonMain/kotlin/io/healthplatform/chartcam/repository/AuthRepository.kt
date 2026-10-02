/**
 * @file AuthRepository.kt
 * Contains declarations for AuthRepository.kt.
 *
 * Contains the AuthRepository which handles user authentication, session management,
 * and secure credential storage.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.models.TokenResponse
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.utils.CryptoService
import io.healthplatform.chartcam.utils.UUID
import io.healthplatform.chartcam.utils.decryptCatching
import io.healthplatform.chartcam.utils.encryptCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository responsible for user authentication and session management.
 * Authentication flows rely on locally hashed credentials and managed session tokens.
 * Functions return Result failure if operations are performed without valid session state.
 * Contains logic for local credential verification and storing the Practitioner context.
 *
 * @param storage The SecureStorage implementation used to store sensitive tokens and credentials.
 */
open class AuthRepository(
    /**
     * Secure storage backend for persisting access tokens, refresh tokens, and password hashes.
     */
    private val storage: SecureStorage,
) {
    private val _currentUser = MutableStateFlow<Practitioner?>(null)
    private val _isDemoSession = MutableStateFlow(false)
    private val refreshMutex = Mutex()
    private val cryptoService = CryptoService()

    /**
     * Observable stream of the currently logged-in practitioner.
     */
    open val currentUser: StateFlow<Practitioner?> = _currentUser.asStateFlow()

    /**
     * Observable stream indicating whether the current active session is in demo mode.
     */
    open val isDemoSession: StateFlow<kotlin.Boolean> = _isDemoSession.asStateFlow()

    /**
     * Constants used by the AuthRepository for storage keys.
     */
    companion object {
        /** Key used for storing the OAuth2 access token. */
        private const val KEY_ACCESS_TOKEN = "access_token"

        /** Key used for storing the OAuth2 refresh token. */
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        /** Key used for storing the current authenticated user's username. */
        const val KEY_CURRENT_USERNAME = "current_username"

        /** Key used for storing whether the session is a demo session. */
        const val KEY_IS_DEMO = "is_demo_session"

        /** Username associated with the one-click demo practitioner. */
        const val DEMO_USERNAME = "DemoClinician"

        /** Fixed identifier for the demo practitioner profile. */
        const val DEMO_PRACTITIONER_ID = "prac_demo_user"

        /** Static validation payload used to verify correct key derivation on login. */
        private const val AUTH_VALIDATION_PAYLOAD = "CHARTCAM_AUTH_VALIDATION"
    }

    /**
     * Attempts to log in using local credentials.
     * Validates credentials using Argon2id key derivation via CryptoService.
     * Saves tokens securely and creates the user profile.
     *
     * @param username The practitioner's username/email.
     * @param password The secret password.
     * @return Result wrapping the Practitioner profile on success, or an AuthError on failure.
     */
    @Suppress("MaxLineLength", "ReturnCount")
    open suspend fun login(
        username: kotlin.String,
        password: kotlin.String,
    ): Result<Practitioner> {
        val authKey = "auth_payload_$username"

        val storedPayload =
            runCatching {
                storage.getString(authKey)
            }.getOrElse { e ->
                return Result.failure(AuthError.StorageError("Failed to read storage", e))
            }

        if (storedPayload == null) {
            // First time login for this user, establish their credential validation payload
            val encryptedResult = cryptoService.encryptCatching(AUTH_VALIDATION_PAYLOAD, password)
            if (encryptedResult.isFailure) {
                return Result.failure(AuthError.CryptoError("Failed to derive key or encrypt payload", encryptedResult.exceptionOrNull()))
            }
            runCatching {
                storage.save(authKey, encryptedResult.getOrThrow())
            }.onFailure { e ->
                return Result.failure(AuthError.StorageError("Failed to save credentials to storage", e))
            }
        } else {
            // Existing user, attempt decryption to validate password
            val decryptedResult = cryptoService.decryptCatching(storedPayload, password)
            if (decryptedResult.isFailure || decryptedResult.getOrNull() != AUTH_VALIDATION_PAYLOAD) {
                return Result.failure(AuthError.InvalidCredentials())
            }
        }

        val tokenResponse =
            TokenResponse(
                accessToken = "access_${UUID.randomUUID()}",
                refreshToken = "refresh_${UUID.randomUUID()}",
                expiresIn = 3600,
                tokenType = "Bearer",
            )

        runCatching {
            storage.save(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            storage.save(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            storage.save(KEY_CURRENT_USERNAME, username)
        }.onFailure { e ->
            return Result.failure(AuthError.StorageError("Failed to save session tokens", e))
        }

        val practitioner =
            createFhirPractitioner(
                id = "prac_${username.hashCode()}",
                lastName = username,
                firstName = "Dr.",
                isActive = true,
            )

        _currentUser.value = practitioner
        _isDemoSession.value = false
        return Result.success(practitioner)
    }

    /**
     * Authenticates a pre-configured synthetic demo practitioner without requiring credential entry.
     * Sets [isDemoSession] to true and persists demo tokens in secure storage.
     *
     * @return Result wrapping the demo [Practitioner] profile.
     */

    open suspend fun loginAsDemo(): Result<Practitioner> {
        val username = DEMO_USERNAME
        val tokenResponse =
            TokenResponse(
                accessToken = "demo_access_${UUID.randomUUID()}",
                refreshToken = "demo_refresh_${UUID.randomUUID()}",
                expiresIn = 86400,
                tokenType = "Bearer",
            )

        runCatching {
            storage.save(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            storage.save(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            storage.save(KEY_CURRENT_USERNAME, username)
            storage.save(KEY_IS_DEMO, "true")
        }.onFailure { e ->
            return Result.failure(AuthError.StorageError("Failed to save demo tokens", e))
        }

        val practitioner =
            createFhirPractitioner(
                id = DEMO_PRACTITIONER_ID,
                lastName = "Clinician",
                firstName = "Dr. Demo",
                isActive = true,
            )

        _currentUser.value = practitioner
        _isDemoSession.value = true
        return Result.success(practitioner)
    }

    /**
     * Checks if a valid token exists in storage and restores the session if it does.
     * Sets the [currentUser] flow with the restored profile.
     *
     * @return A [Result] enclosing the restored [Practitioner] or an error if no active session exists.
     */
    @Suppress("ReturnCount")
    open suspend fun checkSession(): Result<Practitioner> {
        val token =
            runCatching {
                storage.getString(KEY_ACCESS_TOKEN)
            }.getOrElse { e ->
                return Result.failure(AuthError.StorageError("Failed to read session token", e))
            }

        val username =
            runCatching {
                storage.getString(KEY_CURRENT_USERNAME) ?: "Doe"
            }.getOrElse { e ->
                return Result.failure(AuthError.StorageError("Failed to read username", e))
            }

        if (!token.isNullOrEmpty()) {
            val isDemo =
                runCatching {
                    storage.getString(KEY_IS_DEMO) == "true"
                }.getOrDefault(false)

            _isDemoSession.value = isDemo
            val pracId = if (isDemo) DEMO_PRACTITIONER_ID else "prac_${username.hashCode()}"
            val familyName = if (isDemo) "Clinician" else username
            val givenName = if (isDemo) "Dr. Demo" else "Dr."

            val practitioner =
                createFhirPractitioner(
                    id = pracId,
                    lastName = familyName,
                    firstName = givenName,
                    isActive = true,
                )
            _currentUser.value = practitioner
            return Result.success(practitioner)
        }
        _isDemoSession.value = false
        return Result.failure(AuthError.NoActiveSession())
    }

    /**
     * Clears local tokens and session state, logging the user out.
     */

    open fun logout() {
        runCatching {
            storage.delete(KEY_ACCESS_TOKEN)
            storage.delete(KEY_REFRESH_TOKEN)
            storage.delete(KEY_CURRENT_USERNAME)
            storage.delete(KEY_IS_DEMO)
        }
        _currentUser.value = null
        _isDemoSession.value = false
    }

    /**
     * Deletes the local account credentials for a given username and logs out.
     *
     * @param username The username for which to delete stored credentials.
     */

    open fun deleteAccount(username: kotlin.String) {
        val authKey = "auth_payload_$username"
        runCatching {
            storage.delete(authKey)
        }
        logout()
    }

    /**
     * Refreshes the local session using the currently stored refresh token.
     * Updates the secure storage with a new local access token on success.
     * Thread-safe against concurrent simultaneous background refresh requests.
     *
     * @return A [Result] enclosing the new access token or an error.
     */

    open suspend fun refreshToken(): Result<kotlin.String> =
        refreshMutex.withLock {
            val refreshToken =
                runCatching {
                    storage.getString(KEY_REFRESH_TOKEN)
                }.getOrElse { e ->
                    return Result.failure(AuthError.StorageError("Failed to read refresh token", e))
                }

            if (refreshToken.isNullOrEmpty()) {
                return Result.failure(AuthError.NoActiveSession("No refresh token stored"))
            }

            return runCatching {
                val newAccess = "refreshed_access_${UUID.randomUUID()}"
                storage.save(KEY_ACCESS_TOKEN, newAccess)
                newAccess
            }.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(AuthError.StorageError("Failed to save new access token", it)) },
            )
        }
}
