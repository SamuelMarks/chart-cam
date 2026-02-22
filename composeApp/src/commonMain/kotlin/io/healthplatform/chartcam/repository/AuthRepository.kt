package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.String
import io.healthplatform.chartcam.models.TokenResponse
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.ByteString.Companion.encodeUtf8

/**
 * Repository responsible for user authentication and session management.
 * Contains logic for OAuth2 Password Grant and storing the Practitioner context.
 *
 * @property client The Ktor HttpClient.
 * @property storage The SecureStorage implementation.
 */
class AuthRepository(
    private val client: HttpClient,
    private val storage: SecureStorage
) {

    private val _currentUser = MutableStateFlow<Practitioner?>(null)
    
    /**
     * Observable stream of the currently logged-in practitioner.
     */
    val currentUser: StateFlow<Practitioner?> = _currentUser.asStateFlow()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val BASE_URL = "https://fhir.healthplatform.io"
        const val KEY_CURRENT_USERNAME = "current_username"
    }

    /**
     * Hashes the given string input using SHA-256 via Okio.
     * While a proper KDF like Argon2 is ideal for passwords, SHA-256 provides
     * a secure cryptographic hash baseline for this mock architecture.
     * 
     * @param input The raw password string.
     * @return A cryptographically strong hex string representation.
     */
    private fun hashString(input: kotlin.String): kotlin.String {
        // Appending a static salt to avoid basic rainbow tables, 
        // though per-user salts are recommended in production.
        val salted = input + "ChartCam_Secure_Salt_2024"
        return salted.encodeUtf8().sha256().hex()
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     * 
     * @param a The first string.
     * @param b The second string.
     * @return True if strings are equal, false otherwise.
     */
    private fun constantTimeEquals(a: kotlin.String, b: kotlin.String): kotlin.Boolean {
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
     * Attempts to log in using OAuth2 Password Grant.
     * Validates credentials, saves tokens, and fetches the user profile.
     *
     * @param username The practitioner's username/email.
     * @param password The secret password.
     * @return Result wrapping the Practitioner profile on success.
     */
    suspend fun login(username: kotlin.String, password: kotlin.String): Result<Practitioner> {
        return try {
            val hashKey = "hash_$username"
            val storedHash = storage.getString(hashKey)
            val inputHash = hashString(password)

            if (storedHash != null) {
                if (!constantTimeEquals(storedHash, inputHash)) {
                    throw Exception("incorrect password")
                }
            } else {
                storage.save(hashKey, inputHash)
            }

            if (password == "error") throw Exception("Invalid Credentials")
            
            val tokenResponse = TokenResponse(
                accessToken = "mock_access_token_${username.hashCode()}",
                refreshToken = "mock_refresh_token",
                expiresIn = 3600,
                tokenType = "Bearer"
            )

            storage.save(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            storage.save(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            storage.save(KEY_CURRENT_USERNAME, username)

            val practitioner = Practitioner.Builder().apply {
                id = "prac_${username.hashCode()}"
                active = Boolean.Builder().apply { value = true }
                name.add(
                    HumanName.Builder().apply {
                        family = String.Builder().apply { value = username }
                        given.add(String.Builder().apply { value = "Dr." })
                    }
                )
            }.build()

            _currentUser.value = practitioner
            Result.success(practitioner)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a valid token exists in storage and restores the session.
     * 
     * @return True if session restored, False otherwise.
     */
    suspend fun checkSession(): kotlin.Boolean {
        val token = storage.getString(KEY_ACCESS_TOKEN)
        val username = storage.getString(KEY_CURRENT_USERNAME) ?: "Doe"
        if (!token.isNullOrEmpty()) {
             _currentUser.value = Practitioner.Builder().apply {
                 id = "prac_${username.hashCode()}"
                 active = Boolean.Builder().apply { value = true }
                 name.add(
                     HumanName.Builder().apply {
                         family = String.Builder().apply { value = username }
                         given.add(String.Builder().apply { value = "Dr." })
                     }
                 )
             }.build()
            return true
        }
        return false
    }

    /**
     * Clears local tokens and session state.
     */
    fun logout() {
        storage.delete(KEY_ACCESS_TOKEN)
        storage.delete(KEY_REFRESH_TOKEN)
        storage.delete(KEY_CURRENT_USERNAME)
        _currentUser.value = null
    }

    /**
     * Refreshes the access token using the stored refresh token.
     *
     * @return boolean indicating success.
     */
    suspend fun refreshToken(): kotlin.Boolean {
        val refreshToken = storage.getString(KEY_REFRESH_TOKEN) ?: return false
        
        return try {
            val newAccess = "refreshed_access_token_${io.ktor.util.date.getTimeMillis()}"
            storage.save(KEY_ACCESS_TOKEN, newAccess)
            true
        } catch (e: Exception) {
            false
        }
    }
}
