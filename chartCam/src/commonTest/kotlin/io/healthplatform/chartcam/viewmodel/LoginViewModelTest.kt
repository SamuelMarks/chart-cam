/**
 * @file LoginViewModelTest.kt
 * Contains declarations for LoginViewModelTest.kt.
 *
 * Comprehensive tests for [LoginViewModel].
 *
 * Verifies that the login functionality works as expected, handling
 * both success and failure cases by simulating network and repository behaviors.
 */
package io.healthplatform.chartcam.viewmodel

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.invalid_credentials
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

/**
 * Test class for the [LoginViewModel].
 *
 * Provides automated tests for UI state changes based on authentication responses.
 */
class LoginViewModelTest {
    /**
     * Dispatcher used to control the execution of coroutines in tests.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * The repository responsible for authenticating users, to be mocked/stubbed in tests.
     */
    private lateinit var authRepository: AuthRepository

    /**
     * The mocked secure storage instance used by the [AuthRepository] during testing.
     */
    private lateinit var mockStorage: MockStorage

    /**
     * A simple Map-based implementation of [SecureStorage] used exclusively for testing.
     */
    class MockStorage : SecureStorage {
        /**
         * In-memory map holding the stored string values.
         */
        val data = mutableMapOf<String, String>()

        /**
         * Saves a string value to the mock storage.
         *
         * @param key The key under which the value should be saved.
         * @param value The value to be saved.
         */
        override fun save(
            key: String,
            value: String,
        ) {
            data[key] = value
        }

        /**
         * Retrieves a string value from the mock storage.
         *
         * @param key The key of the value to retrieve.
         * @return The saved string value, or null if the key does not exist.
         */
        override fun getString(key: String): String? = data[key]

        /**
         * Deletes a string value from the mock storage.
         *
         * @param key The key of the value to delete.
         */
        override fun delete(key: String) {
            data.remove(key)
        }
    }

    /**
     * Sets up the test environment.
     *
     * Replaces the main coroutine dispatcher with a test dispatcher and
     * initializes the mock storage instance.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockStorage = MockStorage()
    }

    /**
     * Tears down the test environment.
     *
     * Resets the main coroutine dispatcher back to the original state.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests the scenario where the login operation succeeds.
     *
     * Verifies that the view model transitions to a logged-in state without errors
     * when the authentication repository provides a valid token response.
     */
    @Test
    fun testLoginSuccess() =
        runTest {
            // Mock a successful OAuth response
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            ByteReadChannel(
                                """{"accessToken":"token","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}""",
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
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

    /**
     * Tests the scenario where the login operation fails.
     *
     * Verifies that the view model updates its UI state with an appropriate error message
     * and correctly reflects that the user is not logged in when authentication fails.
     */
    @Test
    fun testLoginFailure() =
        runTest {
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
            assertEquals(Res.string.invalid_credentials, viewModel.uiState.value.errorMessage)
        }
}
