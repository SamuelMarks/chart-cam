/**
 * @file AuthRepositoryTest.kt
 * Contains declarations for AuthRepositoryTest.kt.
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the [AuthRepository] login flow and state management.
 */
class AuthRepositoryTest {
    /**
     * Mock of [SecureStorage] to be used in auth tests.
     */
    class MockSecureStorage : SecureStorage {
        /** Internal storage. */
        val map = mutableMapOf<String, String>()

        /** Save value logic. */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /** Read value logic. */
        override fun getString(key: String): String? = map[key]

        /** Delete value logic. */
        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Test successful login and stored hash validation.
     */
    @Test
    fun testLoginSuccess() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            val result = repo.login("testuser", "password123")
            assertTrue(result.isSuccess)

            val user = result.getOrNull()
            assertNotNull(user)
            assertEquals(
                "testuser",
                user.name
                    .first()
                    .family
                    ?.value,
            )

            // Login again to hit the stored hash branch
            val result2 = repo.login("testuser", "password123")
            assertTrue(result2.isSuccess)
        }

    /**
     * Test incorrect password handling.
     */
    @Test
    fun testLoginWrongPassword() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            repo.login("testuser", "password123")
            val result = repo.login("testuser", "wrongpassword")

            assertTrue(result.isFailure)
            assertEquals("incorrect password", result.exceptionOrNull()?.message)
        }

    /**
     * Test specific keyword login failure behavior.
     */
    @Test
    fun testLoginErrorKeyword() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            val result = repo.login("testuser", "error")
            assertTrue(result.isFailure)
            assertEquals("Invalid Credentials", result.exceptionOrNull()?.message)
        }

    /**
     * Test session validation checks.
     */
    @Test
    fun testCheckSession() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            assertFalse(repo.checkSession())

            repo.login("testuser", "password123")
            assertTrue(repo.checkSession())
            assertNotNull(repo.currentUser.value)
        }

    /**
     * Test logout and account deletion mechanics.
     */
    @Test
    fun testLogoutAndDeleteAccount() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            repo.login("testuser", "password123")
            assertNotNull(storage.getString(AuthRepository.KEY_CURRENT_USERNAME))

            repo.logout()
            assertNull(repo.currentUser.value)
            assertNull(storage.getString(AuthRepository.KEY_CURRENT_USERNAME))

            repo.login("testuser", "password123")
            repo.deleteAccount("testuser")
            assertNull(repo.currentUser.value)
            assertNull(storage.getString("hash_testuser"))
        }

    /**
     * Test refresh token operation.
     */
    @Test
    fun testRefreshToken() =
        runTest {
            val client = HttpClient(MockEngine { respondOk("") })
            val storage = MockSecureStorage()
            val repo = AuthRepository(client, storage)

            assertFalse(repo.refreshToken())

            repo.login("testuser", "password123")
            val oldAccess = storage.getString("access_token")

            assertTrue(repo.refreshToken())
            val newAccess = storage.getString("access_token")

            kotlin.test.assertNotEquals(oldAccess, newAccess)
        }
}
