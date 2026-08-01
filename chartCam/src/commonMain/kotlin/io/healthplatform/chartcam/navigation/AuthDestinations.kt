/**
 * @file AuthDestinations.kt
 * Contains declarations for AuthDestinations.
 *
 * Defines the navigation graph builder extensions for authentication destinations.
 */
package io.healthplatform.chartcam.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.healthplatform.chartcam.ui.LoginScreen
import io.healthplatform.chartcam.viewmodel.LoginViewModel

/**
 * Registers the login destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.loginDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable(Routes.LOGIN) {
        val viewModel =
            androidx.lifecycle.viewmodel.compose
                .viewModel { LoginViewModel(deps.authRepository) }
        androidx.compose.runtime.key(currentLang) {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {
                navController.navigate(Routes.PATIENT_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
    }
}
