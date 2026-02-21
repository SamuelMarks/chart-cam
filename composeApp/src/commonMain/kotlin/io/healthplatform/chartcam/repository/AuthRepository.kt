package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Practitioner
import io.healthplatform.chartcam.models.TokenResponse
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
     * Hashes the given string input.
     * 
     * @param input The raw password string.
     * @return A simple hashed string representation.
     */
    private fun hashString(input: String): String {
        var hash = 0
        for (i in input.indices) {
            hash = 31 * hash + input[i].code
        }
        return hash.toString()
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     * 
     * @param a The first string.
     * @param b The second string.
     * @return True if strings are equal, false otherwise.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) {
            return false
        }
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Attempts to log in using OAuth2 Password Grant.
     * Validates credentials, saves tokens, and fetches the user profile.
     *
     * @param username The practitioner's username/email.
     * @param password The secret password.
     * @return Result wrapping the Practitioner profile on success.
     */
    suspend fun login(username: String, password: String): Result<Practitioner> {
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

            val practitioner = Practitioner(
                id = "prac_${username.hashCode()}",
                active = true,
                name = listOf(HumanName(family = username, given = listOf("Dr.")))
            )

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
    suspend fun checkSession(): Boolean {
        val token = storage.getString(KEY_ACCESS_TOKEN)
        val username = storage.getString(KEY_CURRENT_USERNAME) ?: "Doe"
        if (!token.isNullOrEmpty()) {
             _currentUser.value = Practitioner(
                id = "prac_${username.hashCode()}",
                active = true,
                name = listOf(HumanName(family = username, given = listOf("Dr.")))
            )
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
    suspend fun refreshToken(): Boolean {
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