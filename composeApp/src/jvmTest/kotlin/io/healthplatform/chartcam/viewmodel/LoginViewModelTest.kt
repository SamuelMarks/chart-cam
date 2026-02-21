package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var mockStorage: MockStorage

    // Simple Map-based mock storage
    class MockStorage : SecureStorage {
        val data = mutableMapOf<String, String>()
        override fun save(key: String, value: String) { data[key] = value }
        override fun getString(key: String): String? = data[key]
        override fun delete(key: String) { data.remove(key) }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockStorage = MockStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoginSuccess() = runTest {
        // Mock a successful OAuth response
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel("""{"accessToken":"token","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = NetworkClient.create(mockEngine)
        authRepository = AuthRepository(client, mockStorage)

        val viewModel = LoginViewModel(authRepository)

        // Initial State
        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)

        // Act: Login with valid credentials
        viewModel.login("user", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testLoginFailure() = runTest {
        // Engine not actually called due to AuthRepository "error" password simulation logic in prompt
        val client = NetworkClient.create(MockEngine { respond("OK") })
        authRepository = AuthRepository(client, mockStorage)

        val viewModel = LoginViewModel(authRepository)

        // Act: Login with "error" password (triggers exception in AuthRepository)
        viewModel.login("user", "error")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Invalid Credentials", viewModel.uiState.value.errorMessage)
    }
}