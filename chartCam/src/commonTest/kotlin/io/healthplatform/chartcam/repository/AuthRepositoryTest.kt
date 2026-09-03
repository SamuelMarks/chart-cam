/**
 * @file AuthRepositoryTest.kt
 * Contains tests for [AuthRepository].
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * An in-memory implementation of [SecureStorage] for testing.
 */
class InMemorySecureStorage : SecureStorage {
    private val data = mutableMapOf<String, String>()

    override fun save(
        key: String,
        value: String,
    ) {
        data[key] = value
    }

    override fun getString(key: String): String? = data[key]

    override fun delete(key: String) {
        data.remove(key)
    }

    /**
     * Helper to verify if storage is empty.
     * @return true if empty.
     */
    fun isEmpty(): Boolean = data.isEmpty()
}

/**
 * Test class for validating [AuthRepository] operations, primarily focusing on session security.
 */
class AuthRepositoryTest {
    /**
     * Verifies that [AuthRepository.logout] securely clears all sensitive session tokens
     * and resets the current user state to null.
     */
    @Test
    fun testSecureClearingOfSensitiveDataOnLogout() =
        runTest {
            val mockEngine = MockEngine { _ -> respondOk() }
            val httpClient = HttpClient(mockEngine)
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(httpClient, secureStorage)

            // Given a user logs in
            val username = "testuser"
            val password = "securepassword"
            val result = repository.login(username, password)
            assertEquals(true, result.isSuccess, "Login should succeed")

            // Assert tokens and user state exist
            assertEquals("testuser", secureStorage.getString(AuthRepository.KEY_CURRENT_USERNAME))
            assertEquals(true, secureStorage.getString("access_token") != null)
            assertEquals(true, repository.currentUser.value != null)

            // When user logs out
            repository.logout()

            // Then all sensitive data should be cleared from storage
            assertNull(secureStorage.getString(AuthRepository.KEY_CURRENT_USERNAME), "Username should be removed")
            assertNull(secureStorage.getString("access_token"), "Access token should be removed")
            assertNull(secureStorage.getString("refresh_token"), "Refresh token should be removed")

            // And current user state should be reset
            assertNull(repository.currentUser.value, "Current user state should be null")
        }
}
