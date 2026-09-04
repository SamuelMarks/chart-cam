/**
 * @file LoginViewModelTest.kt
 * Contains declarations for LoginViewModelTest.kt.
 *
 * Comprehensive tests for [LoginViewModel].
 *
 * Verifies that the login functionality works as expected, handling
 * both success and failure cases by simulating repository behaviors.
 */
package io.healthplatform.chartcam.viewmodel

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.incorrect_password
import chartcam.chartcam.generated.resources.invalid_credentials
import chartcam.chartcam.generated.resources.unknown_error
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
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
            authRepository = AuthRepository(mockStorage)

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
            authRepository = AuthRepository(mockStorage)

            val viewModel = LoginViewModel(authRepository)

            // Act: Login with "error" password (triggers exception in AuthRepository)
            viewModel.login("user", "error")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            assertFalse(viewModel.uiState.value.isLoggedIn)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(Res.string.invalid_credentials, viewModel.uiState.value.errorMessage)
        }

    /**
     * Tests the scenario where the login operation fails with an incorrect password error specifically.
     */
    @Test
    fun testLoginFailure_IncorrectPassword() =
        runTest {
            authRepository = AuthRepository(mockStorage)

            authRepository.login("user", "correct")

            val viewModel = LoginViewModel(authRepository)

            viewModel.login("user", "wrong")
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoggedIn)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(Res.string.incorrect_password, viewModel.uiState.value.errorMessage)
        }

    /**
     * Tests the scenario where the login operation fails with an unknown error.
     */
    @Test
    fun testLoginFailure_UnknownError() =
        runTest {
            val throwingRepo =
                object : AuthRepository(mockStorage) {
                    /**
                     * Override login to always throw an unknown exception.
                     *
                     * @param username The username.
                     * @param password The password.
                     * @return A Result containing the failure.
                     */
                    override suspend fun login(
                        username: String,
                        password: String,
                    ): Result<com.google.fhir.model.r4.Practitioner> = Result.failure(Exception("Some unknown error"))
                }
            val viewModel = LoginViewModel(throwingRepo)

            viewModel.login("user", "pass")
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoggedIn)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(chartcam.chartcam.generated.resources.Res.string.unknown_error, viewModel.uiState.value.errorMessage)
        }
}
