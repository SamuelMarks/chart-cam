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
import io.healthplatform.chartcam.storage.BiometricAuthResult
import io.healthplatform.chartcam.storage.BiometricHardwareStatus
import io.healthplatform.chartcam.storage.BiometricSecurityManager
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
 * @param isDemoLoading Whether demo mode initialization is actively occurring.
 * @param isTutorialVisible Whether the onboarding workflow tutorial overlay is active.
 * @param errorMessage Localized error message if login fails, or null if there is no error.
 * @param isLoggedIn Flag indicating successful authentication.
 * @param isBiometricAvailable Flag indicating whether biometric authentication is available on device.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isDemoLoading: Boolean = false,
    val isTutorialVisible: Boolean = false,
    val errorMessage: StringResource? = null,
    val isLoggedIn: Boolean = false,
    val isBiometricAvailable: Boolean = false,
)

/**
 * ViewModel handling the business logic for the Login Screen.
 * Bridges the UI events to the [AuthRepository].
 *
 * @param authRepository The source of authentication truth and login operations.
 * @param biometricSecurityManager Optional biometric security manager for biometric unlocking.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val biometricSecurityManager: BiometricSecurityManager? = null,
) : ViewModel() {
    /**
     * Internal mutable state flow for the login UI state.
     */
    private val _uiState =
        MutableStateFlow(
            LoginUiState(
                isBiometricAvailable =
                    if (biometricSecurityManager != null) {
                        biometricSecurityManager.isHardwareBackedKeystore() ||
                            biometricSecurityManager.checkKeystoreAvailability() == BiometricHardwareStatus.AVAILABLE
                    } else {
                        false
                    },
            ),
        )

    /**
     * Public immutable state flow for the login UI state.
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Authenticates using biometrics, unlocking stored clinician credentials or falling back to demo mode.
     *
     * @param simulateSuccess Whether to simulate success. Defaults to actual hardware availability.
     * @param onSuccess Optional callback invoked when biometric login succeeds.
     * @return A [Result] enclosing the biometric authentication outcome.
     */
    fun authenticateWithBiometrics(
        simulateSuccess: Boolean =
            if (biometricSecurityManager != null) {
                biometricSecurityManager.checkKeystoreAvailability() ==
                    io.healthplatform.chartcam.storage.BiometricHardwareStatus.AVAILABLE
            } else {
                false
            },
        onSuccess: (() -> Unit)? = null,
    ): Result<BiometricAuthResult> =
        runCatching {
            val bioManager =
                biometricSecurityManager
                    ?: return@runCatching BiometricAuthResult.HardwareError("Biometrics unconfigured")
            val authOutcome = bioManager.authenticate(simulateSuccess = simulateSuccess)
            if (authOutcome is BiometricAuthResult.Success) {
                viewModelScope.launch {
                    val sessionResult = authRepository.checkSession()
                    val result =
                        if (sessionResult.isSuccess) {
                            sessionResult
                        } else {
                            authRepository.loginAsDemo()
                        }
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isLoggedIn = true) }
                        if (onSuccess != null) {
                            onSuccess()
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = Res.string.invalid_credentials)
                }
            }
            authOutcome
        }

    /**
     * Executes authentic native biometric prompt authentication without simulation bypass.
     *
     * @param onSuccess Optional callback invoked when biometric login succeeds.
     * @return A [Result] enclosing the biometric authentication outcome.
     */
    suspend fun authenticateWithPrompt(onSuccess: (() -> Unit)? = null): Result<BiometricAuthResult> =
        runCatching {
            val bioManager =
                biometricSecurityManager
                    ?: return@runCatching BiometricAuthResult.HardwareError("Biometrics unconfigured")
            val authOutcome = bioManager.authenticatePrompt()
            if (authOutcome is BiometricAuthResult.Success) {
                val sessionResult = authRepository.checkSession()
                val result =
                    if (sessionResult.isSuccess) {
                        sessionResult
                    } else {
                        authRepository.loginAsDemo()
                    }
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoggedIn = true) }
                    if (onSuccess != null) {
                        onSuccess()
                    }
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = Res.string.invalid_credentials)
                }
            }
            authOutcome
        }

    /**
     * Controls the visibility of the onboarding workflow tutorial dialog or carousel.
     *
     * @param show True to present the tutorial, false to dismiss it.
     */
    fun showTutorial(show: Boolean) {
        _uiState.update { it.copy(isTutorialVisible = show) }
    }

    /**
     * Initiates immediate one-click authentication in demo mode using synthetic clinical data.
     *
     * @param onSuccess Optional callback invoked when demo authentication succeeds.
     */
    fun onDemoLoginClicked(onSuccess: (() -> Unit)? = null) {
        _uiState.update { it.copy(isDemoLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.loginAsDemo()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isDemoLoading = false, isLoggedIn = true)
                }
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isDemoLoading = false,
                        errorMessage = Res.string.unknown_error,
                    )
                }
            }
        }
    }

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

            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, isLoggedIn = true)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                when (error.message) {
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
