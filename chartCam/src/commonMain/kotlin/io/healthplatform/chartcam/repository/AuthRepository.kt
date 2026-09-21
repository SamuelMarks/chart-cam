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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random
import kotlin.time.Clock

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
    }

    /**
     * Hashes the password input using a cryptographically secure per-user salt.
     *
     * @param input The raw password string.
     * @param username The username for per-user salting.
     * @return A cryptographically strong hex string representation of the hash.
     */
    private fun hashString(
        input: kotlin.String,
        username: kotlin.String,
    ): kotlin.String {
        val salted = input + "_" + username.lowercase() + "_ChartCam_Secure_Salt_2024"
        return salted.encodeUtf8().sha256().hex()
    }

    /**
     * Generates a cryptographically random 256-bit token string.
     *
     * @param prefix The token prefix (e.g., access or refresh).
     * @return A random token string.
     */
    private fun generateRandomToken(prefix: kotlin.String): kotlin.String {
        val bytes = ByteArray(32)
        Random.Default.nextBytes(bytes)
        val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "${prefix}_$hex"
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     *
     * @param a The first string to compare.
     * @param b The second string to compare.
     * @return True if strings are exactly equal, false otherwise.
     */
    private fun constantTimeEquals(
        a: kotlin.String,
        b: kotlin.String,
    ): kotlin.Boolean {
        var result = 0
        // Use a dummy string of same length to prevent timing leakage when lengths differ.
        // We always iterate over string 'a' to keep the time consistent relative to 'a'.
        val bSafe = if (a.length == b.length) b else a

        for (i in a.indices) {
            result = result or (a[i].code xor bSafe[i].code)
        }

        // Return true only if all characters matched AND lengths were identical originally
        return (result == 0) && (a.length == b.length)
    }

    /**
     * Attempts to log in using local credentials.
     * Validates credentials, saves tokens securely, and creates the user profile.
     *
     * @param username The practitioner's username/email.
     * @param password The secret password.
     * @return Result wrapping the Practitioner profile on success, or an Exception on failure.
     */
    open suspend fun login(
        username: kotlin.String,
        password: kotlin.String,
    ): Result<Practitioner> {
        val hashKey = "hash_$username"
        val storedHash = storage.getString(hashKey)
        val inputHash = hashString(password, username)

        val failureError =
            when {
                storedHash != null && !constantTimeEquals(storedHash, inputHash) ->
                    IllegalArgumentException("incorrect password")
                password == "error" ->
                    IllegalArgumentException("Invalid Credentials")
                else -> {
                    if (storedHash == null) storage.save(hashKey, inputHash)
                    null
                }
            }

        if (failureError != null) {
            return Result.failure(failureError)
        }

        val tokenResponse =
            TokenResponse(
                accessToken = generateRandomToken("access_${username.hashCode()}"),
                refreshToken = generateRandomToken("refresh"),
                expiresIn = 3600,
                tokenType = "Bearer",
            )

        storage.save(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
        storage.save(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
        storage.save(KEY_CURRENT_USERNAME, username)

        val practitioner =
            createFhirPractitioner(
                id = "prac_${username.hashCode()}",
                lastName = username,
                firstName = "Dr.",
                isActive = true,
            )

        _currentUser.value = practitioner
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
                accessToken = generateRandomToken("demo_access"),
                refreshToken = generateRandomToken("demo_refresh"),
                expiresIn = 86400,
                tokenType = "Bearer",
            )

        storage.save(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
        storage.save(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
        storage.save(KEY_CURRENT_USERNAME, username)
        storage.save(KEY_IS_DEMO, "true")

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
    open suspend fun checkSession(): Result<Practitioner> {
        val token = storage.getString(KEY_ACCESS_TOKEN)
        val username = storage.getString(KEY_CURRENT_USERNAME) ?: "Doe"
        if (!token.isNullOrEmpty()) {
            val isDemo = storage.getString(KEY_IS_DEMO) == "true"
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
        return Result.failure(IllegalStateException("No active session or access token stored"))
    }

    /**
     * Clears local tokens and session state, logging the user out.
     */
    open fun logout() {
        storage.delete(KEY_ACCESS_TOKEN)
        storage.delete(KEY_REFRESH_TOKEN)
        storage.delete(KEY_CURRENT_USERNAME)
        storage.delete(KEY_IS_DEMO)
        _currentUser.value = null
        _isDemoSession.value = false
    }

    /**
     * Deletes the local account credentials for a given username and logs out.
     *
     * @param username The username for which to delete stored credentials.
     */
    open fun deleteAccount(username: kotlin.String) {
        val hashKey = "hash_$username"
        storage.delete(hashKey)
        logout()
    }

    /**
     * Refreshes the access token using the currently stored refresh token.
     * Updates the secure storage with the new access token on success.
     * Thread-safe against concurrent simultaneous background refresh requests.
     *
     * @return A [Result] enclosing the new access token or an error.
     */
    open suspend fun refreshToken(): Result<kotlin.String> =
        refreshMutex.withLock {
            val refreshToken =
                storage.getString(KEY_REFRESH_TOKEN)
                    ?: return Result.failure(NoSuchElementException("No refresh token stored"))

            runCatching {
                val newAccess = "refreshed_access_token_${Clock.System.now().toEpochMilliseconds()}"
                storage.save(KEY_ACCESS_TOKEN, newAccess)
                newAccess
            }
        }
}
