/**
 * @file AuthRepositoryJvmTest.kt
 * Contains declarations for AuthRepositoryJvmTest.kt.
 *
 * Contains tests for [AuthRepository], validating authentication logic against mock network and storage.
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates the [AuthRepository] logic using Mock Networking and Mock Storage.
 */
class AuthRepositoryJvmTest {
    /**
     * A mock implementation of [SecureStorage] used for testing.
     * Stores data in a simple in-memory [MutableMap].
     */
    class MockStorage : SecureStorage {
        /**
         * The in-memory map storing all saved key-value pairs.
         */
        val map = mutableMapOf<String, String>()

        /**
         * Saves a key-value pair to the mock storage.
         *
         * @param key The key under which to save the value.
         * @param value The value to save.
         */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /**
         * Retrieves a string value from the mock storage by its key.
         *
         * @param key The key of the value to retrieve.
         * @return The stored string value, or null if the key does not exist.
         */
        override fun getString(key: String): String? = map[key]

        /**
         * Deletes a key-value pair from the mock storage.
         *
         * @param key The key to delete.
         */
        override fun delete(key: String) {
            map.remove(key)
        }

        var throwOnSave = false
    }

    /**
     * Tests that an exception during refreshToken save returns false.
     */
    @Test
    fun testTokenRefreshException() =
        runTest {
            val storage =
                object : SecureStorage {
                    val map = mutableMapOf<String, String>()

                    override fun save(
                        key: String,
                        value: String,
                    ): Unit = throw IllegalArgumentException("Storage error")

                    override fun getString(key: String): String? = "dummy_refresh_token"

                    override fun delete(key: String) {}
                }
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            val success = repo.refreshToken()

            assertFalse(success)
        }

    /**
     * Tests a successful login scenario, ensuring tokens are saved and the user is set.
     */
    @Test
    fun testLoginSuccess() =
        runTest {
            // Arrange
            val mockEngine =
                MockEngine {
                    respond(
                        content = ByteReadChannel("""{"accessToken":"123","refreshToken":"456","expiresIn":3600,"tokenType":"Bearer"}"""),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = NetworkClient.create(mockEngine)
            val storage = MockStorage()
            val repo = AuthRepository(client, storage)

            // Act
            val result = repo.login("dr_house", "password123")

            // Assert
            assertTrue(result.isSuccess)
            assertNotNull(storage.getString("access_token"))
            assertEquals(
                "dr_house",
                repo.currentUser.value
                    ?.name
                    ?.first()
                    ?.familyName,
            )
        }

    /**
     * Tests a login failure scenario due to an error password, ensuring no tokens are saved.
     */
    @Test
    fun testLoginFailure() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            // Act (using "error" password to trigger exception in current logic)
            val result = repo.login("dr_house", "error")

            // Assert
            assertTrue(result.isFailure)
            assertEquals(null, storage.getString("access_token"))
        }

    /**
     * Tests logging in with an incorrect password after a successful login sets the password.
     */
    @Test
    fun testIncorrectPassword() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            // First login sets the password
            repo.login("dr_house", "password123")

            // Second login with incorrect password fails
            val result = repo.login("dr_house", "wrong")
            assertTrue(result.isFailure)
            assertEquals("incorrect password", result.exceptionOrNull()?.message)
        }

    /**
     * Tests logging in with an incorrect password of the same length after a successful login sets the password.
     */
    @Test
    fun testIncorrectPasswordSameLength() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            repo.login("dr_house", "password123")
            val result = repo.login("dr_house", "password321")
            assertTrue(result.isFailure)
            assertEquals("incorrect password", result.exceptionOrNull()?.message)
        }

    /**
     * Tests checking the session validity before and after logging in and logging out.
     */
    @Test
    fun testCheckSession() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            assertFalse(repo.checkSession())

            repo.login("dr_house", "password123")
            assertTrue(repo.checkSession())
            assertEquals(
                "dr_house",
                repo.currentUser.value
                    ?.name
                    ?.first()
                    ?.familyName,
            )

            repo.logout()
            assertFalse(repo.checkSession())
        }

    /**
     * Tests checking the session validity when an access token exists but the user is not set.
     */
    @Test
    fun testCheckSessionNoUsername() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            storage.save("access_token", "token")

            assertTrue(repo.checkSession())
            assertEquals(
                "Doe",
                repo.currentUser.value
                    ?.name
                    ?.first()
                    ?.familyName,
            )
        }

    /**
     * Tests refreshing the authentication token successfully.
     */
    @Test
    fun testTokenRefresh() =
        runTest {
            val storage = MockStorage()
            storage.save("refresh_token", "existing_refresh_token")

            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            val success = repo.refreshToken()

            assertTrue(success)
            assertNotNull(storage.getString("access_token"))
        }

    /**
     * Tests failing to refresh the authentication token when no refresh token exists.
     */
    @Test
    fun testTokenRefreshFailure() =
        runTest {
            val storage = MockStorage()
            val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

            val success = repo.refreshToken()

            assertFalse(success)
        }
}
