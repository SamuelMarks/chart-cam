/**
 * @file LoginViewModel.kt
 * Contains declarations for LoginViewModel.kt.
 *
 * ViewModel and UI state definition for the Login Screen.
 * Provides the state and business logic for practitioner authentication.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.incorrect_password
import chartcam.chartcam.generated.resources.invalid_credentials
import chartcam.chartcam.generated.resources.unknown_error
import io.healthplatform.chartcam.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * UI State definition for the Login Screen.
 *
 * @param isLoading Whether login processing is actively occurring.
 * @param errorMessage Localized error message if login fails, or null if there is no error.
 * @param isLoggedIn Flag indicating successful authentication.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,
    val isLoggedIn: Boolean = false,
)

/**
 * ViewModel handling the business logic for the Login Screen.
 * Bridges the UI events to the [AuthRepository].
 *
 * @param authRepository The source of authentication truth and login operations.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    /**
     * Internal mutable state flow for the login UI state.
     */
    private val _uiState = MutableStateFlow(LoginUiState())

    /**
     * Public immutable state flow for the login UI state.
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiates the login process.
     * Updates state to Loading and then either to Success or Error based on the outcome.
     *
     * @param username Input username.
     * @param password Input password.
     */
    fun login(
        username: String,
        password: String,
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.login(username, password)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            when (result.exceptionOrNull()?.message) {
                                "incorrect password" -> Res.string.incorrect_password
                                "Invalid Credentials" -> Res.string.invalid_credentials
                                else -> Res.string.unknown_error
                            },
                    )
                }
            }
        }
    }
}
