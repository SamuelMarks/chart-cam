/**
 * @file QuestionnaireDestinations.kt
 * Contains declarations for QuestionnaireDestinations.
 *
 * Defines the navigation graph builder extensions for questionnaire destinations.
 */
package io.healthplatform.chartcam.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.healthplatform.chartcam.ui.QuestionnaireListScreen
import kotlinx.coroutines.CoroutineScope

/**
 * Registers the questionnaire builder destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.questionnaireBuilderDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<QuestionnaireBuilderRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<QuestionnaireBuilderRoute>()
        val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(key = route.duplicateFromId ?: "new") {
                io.healthplatform.chartcam.viewmodel
                    .QuestionnaireBuilderViewModel(deps.questionnaireRepository, route.duplicateFromId)
            }
        androidx.compose.runtime.key(currentLang) {
            io.healthplatform.chartcam.ui.QuestionnaireBuilderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("createdQuestionnaireId", savedId)
                    navController.popBackStack()
                },
            )
        }
    }
}

/**
 * Registers the questionnaire list destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.questionnaireListDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable(Routes.QUESTIONNAIRE_LIST) {
        androidx.compose.runtime.key(currentLang) {
            QuestionnaireListScreen(
                questionnaireRepository = deps.questionnaireRepository,
                onBack = { navController.popBackStack() },
                onNavigateToBuilder = { duplicateId ->
                    navController.navigate(
                        QuestionnaireBuilderRoute(duplicateFromId = duplicateId),
                    )
                },
            )
        }
    }
}
