package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.models.familyName
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Validates the AuthRepository logic using Mock Networking and Mock Storage.
 */
class AuthRepositoryTest {

    // --- Mocks ---
    class MockStorage : SecureStorage {
        val map = mutableMapOf<String, String>()
        override fun save(key: String, value: String) { map[key] = value }
        override fun getString(key: String): String? { return map[key] }
        override fun delete(key: String) { map.remove(key) }
    }

    // --- Tests ---
    @Test
    fun testLoginSuccess() = runTest {
        // Arrange
        val mockEngine = MockEngine { 
            respond(
                content = ByteReadChannel("""{"accessToken":"123","refreshToken":"456","expiresIn":3600,"tokenType":"Bearer"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
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
        assertEquals("dr_house", repo.currentUser.value?.name?.first()?.familyName)
    }

    @Test
    fun testLoginFailure() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

        // Act (using "error" password to trigger exception in current logic)
        val result = repo.login("dr_house", "error")

        // Assert
        assertTrue(result.isFailure)
        assertEquals(null, storage.getString("access_token"))
    }

    @Test
    fun testIncorrectPassword() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

        // First login sets the password
        repo.login("dr_house", "password123")

        // Second login with incorrect password fails
        val result = repo.login("dr_house", "wrong")
        assertTrue(result.isFailure)
        assertEquals("incorrect password", result.exceptionOrNull()?.message)
    }

    @Test
    fun testIncorrectPasswordSameLength() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

        repo.login("dr_house", "password123")
        val result = repo.login("dr_house", "password321")
        assertTrue(result.isFailure)
        assertEquals("incorrect password", result.exceptionOrNull()?.message)
    }

    @Test
    fun testCheckSession() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)

        assertFalse(repo.checkSession())

        repo.login("dr_house", "password123")
        assertTrue(repo.checkSession())
        assertEquals("dr_house", repo.currentUser.value?.name?.first()?.familyName)

        repo.logout()
        assertFalse(repo.checkSession())
    }

    @Test
    fun testCheckSessionNoUsername() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)
        
        storage.save("access_token", "token")
        
        assertTrue(repo.checkSession())
        assertEquals("Doe", repo.currentUser.value?.name?.first()?.familyName)
    }
    
    @Test
    fun testTokenRefresh() = runTest {
        val storage = MockStorage()
        storage.save("refresh_token", "existing_refresh_token")
        
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)
        
        val success = repo.refreshToken()
        
        assertTrue(success)
        assertNotNull(storage.getString("access_token"))
    }

    @Test
    fun testTokenRefreshFailure() = runTest {
        val storage = MockStorage()
        val repo = AuthRepository(NetworkClient.create(MockEngine { respond("") }), storage)
        
        val success = repo.refreshToken()
        
        assertFalse(success)
    }
}
