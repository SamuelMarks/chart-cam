@file:Suppress("ForbiddenException")
/**
 * @file AuthRepositoryTest.kt
 * Contains tests for [AuthRepository].
 */

package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.storage.SecureStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An in-memory implementation of [SecureStorage] for testing.
 */
class InMemorySecureStorage : SecureStorage {
    private val data = mutableMapOf<String, String>()

    var shouldFailGetString = false
    var shouldFailSave = false

    override fun save(
        key: String,
        value: String,
    ) {
        if (shouldFailSave) throw RuntimeException("Simulated save failure")
        data[key] = value
    }

    override fun getString(key: String): String? {
        if (shouldFailGetString) throw RuntimeException("Simulated read failure")
        return data[key]
    }

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
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            // Given a user logs in
            val username = "testuser"
            val password = "securepassword"
            val result = repository.login(username, password)
            assertTrue(result.isSuccess, "Login should succeed")

            // Assert tokens and user state exist
            assertEquals("testuser", secureStorage.getString(AuthRepository.KEY_CURRENT_USERNAME))
            assertTrue(secureStorage.getString("access_token") != null)
            assertTrue(repository.currentUser.value != null)

            // When user logs out
            repository.logout()

            // Then all sensitive data should be cleared from storage
            assertNull(secureStorage.getString(AuthRepository.KEY_CURRENT_USERNAME), "Username should be removed")
            assertNull(secureStorage.getString("access_token"), "Access token should be removed")
            assertNull(secureStorage.getString("refresh_token"), "Refresh token should be removed")

            // And current user state should be reset
            assertNull(repository.currentUser.value, "Current user state should be null")
        }

    @Test
    fun testLoginInvalidCredentials() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            // First login sets the password
            repository.login("user", "pass1")

            // Second login with wrong password should fail
            val result = repository.login("user", "pass2")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AuthError.InvalidCredentials)
        }

    @Test
    fun testLoginStorageErrors() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            secureStorage.shouldFailGetString = true
            val resultRead = repository.login("user", "pass")
            assertTrue(resultRead.isFailure)
            assertTrue(resultRead.exceptionOrNull() is AuthError.StorageError)

            secureStorage.shouldFailGetString = false
            secureStorage.shouldFailSave = true
            val resultSave = repository.login("user", "pass")
            assertTrue(resultSave.isFailure)
            assertTrue(resultSave.exceptionOrNull() is AuthError.StorageError)
        }

    @Test
    fun testLoginSaveTokensFailure() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            // Let it save auth payload but fail on tokens
            // To do this we override save just for tokens
            val failingStorage =
                object : SecureStorage {
                    val data = mutableMapOf<String, String>()

                    override fun save(key: String, value: String) {
                        if (key == "access_token") throw RuntimeException("Failed token")
                        data[key] = value
                    }

                    override fun getString(key: String): String? = data[key]

                    override fun delete(key: String) {}
                }
            val failingRepo = AuthRepository(failingStorage)
            val result = failingRepo.login("user", "pass")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AuthError.StorageError)
        }

    @Test
    fun testLoginAsDemo() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            val result = repository.loginAsDemo()
            assertTrue(result.isSuccess)
            assertTrue(repository.isDemoSession.value)
            assertEquals(AuthRepository.DEMO_USERNAME, secureStorage.getString(AuthRepository.KEY_CURRENT_USERNAME))

            secureStorage.shouldFailSave = true
            val resultFail = repository.loginAsDemo()
            assertTrue(resultFail.isFailure)
            assertTrue(resultFail.exceptionOrNull() is AuthError.StorageError)
        }

    @Test
    fun testCheckSession() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            // Check with empty storage
            assertTrue(repository.checkSession().isFailure)

            // Check with valid standard session
            repository.login("user", "pass")
            val sessionResult = repository.checkSession()
            assertTrue(sessionResult.isSuccess)
            assertFalse(repository.isDemoSession.value)
            assertEquals(
                "user",
                sessionResult
                    .getOrThrow()
                    .name
                    ?.firstOrNull()
                    ?.family
                    ?.value ?: "",
            )

            // Check with valid demo session
            repository.logout()
            repository.loginAsDemo()
            val demoResult = repository.checkSession()
            assertTrue(demoResult.isSuccess)
            assertTrue(repository.isDemoSession.value)
            assertEquals(
                "Clinician",
                demoResult
                    .getOrThrow()
                    .name
                    ?.firstOrNull()
                    ?.family
                    ?.value ?: "",
            )
        }

    @Test
    fun testCheckSessionErrors() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            secureStorage.shouldFailGetString = true
            val result = repository.checkSession()
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AuthError.StorageError)

            secureStorage.shouldFailGetString = false
            // Simulate missing username
            secureStorage.save("access_token", "token")

            val failingUsernameStorage =
                object : SecureStorage {
                    override fun save(key: String, value: String) {}

                    override fun getString(key: String): String? {
                        if (key == "current_username") error("fail")
                        return "token"
                    }

                    override fun delete(key: String) {}
                }
            val failingRepo = AuthRepository(failingUsernameStorage)
            assertTrue(failingRepo.checkSession().isFailure)

            // Simulate failure when reading is_demo_session
            val failingIsDemoStorage =
                object : SecureStorage {
                    override fun save(key: String, value: String) {}

                    override fun getString(key: String): String? {
                        if (key == "is_demo_session") error("fail")
                        return "token" // Valid token and username
                    }

                    override fun delete(key: String) {}
                }
            val failingIsDemoRepo = AuthRepository(failingIsDemoStorage)
            // checkSession should succeed and fallback to isDemo = false
            val demoFailResult = failingIsDemoRepo.checkSession()
            assertTrue(demoFailResult.isSuccess)
            assertFalse(failingIsDemoRepo.isDemoSession.value)
        }

    @Test
    fun testRefreshToken() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            // No session
            assertTrue(repository.refreshToken().isFailure)

            // Valid session
            repository.login("user", "pass")
            val refreshResult = repository.refreshToken()
            assertTrue(refreshResult.isSuccess)

            // Fail to save new token
            val failingStorage =
                object : SecureStorage {
                    override fun save(key: String, value: String): Unit = error("fail")

                    override fun getString(key: String): String? = "token"

                    override fun delete(key: String) {}
                }
            assertTrue(AuthRepository(failingStorage).refreshToken().isFailure)

            // Fail to read refresh token
            secureStorage.shouldFailGetString = true
            assertTrue(repository.refreshToken().isFailure)
        }

    @Test
    fun testDeleteAccount() =
        runTest {
            val secureStorage = InMemorySecureStorage()
            val repository = AuthRepository(secureStorage)

            repository.login("user", "pass")
            assertTrue(secureStorage.getString("auth_payload_user") != null)
            repository.deleteAccount("user")
            assertNull(secureStorage.getString("auth_payload_user"))
            assertNull(repository.currentUser.value)
        }
}
