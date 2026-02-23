package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State definition for the Login Screen.
 *
 * @property isLoading Whether a network request is interacting.
 * @property errorMessage Localized error message if login fails.
 * @property isLoggedIn Flag indicating successful authentication.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

/**
 * ViewModel handling the business logic for the Login Screen.
 * Bridges the UI events to the [AuthRepository].
 *
 * @property authRepository The source of authentication truth.
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiates the login process.
     * Updates state to Loading -> Success/Error.
     *
     * @param username Input username.
     * @param password Input password.
     */
    fun login(username: String, password: String) {
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
                        errorMessage = result.exceptionOrNull()?.message ?: "Unknown Error"
                    ) 
                }
            }
        }
    }
}