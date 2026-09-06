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
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.new_item
import chartcam.chartcam.generated.resources.new_widget_item
import chartcam.chartcam.generated.resources.unknown
import chartcam.chartcam.generated.resources.unknown_copy
import io.healthplatform.chartcam.ui.QuestionnaireListScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Registers the questionnaire builder destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.questionnaireBuilderDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<QuestionnaireBuilderRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<QuestionnaireBuilderRoute>()
        val copyTemplate = stringResource(Res.string.unknown_copy)
        val newItemLabel = stringResource(Res.string.new_item)
        val newWidgetTemplate = stringResource(Res.string.new_widget_item)
        val unknownLabel = stringResource(Res.string.unknown)
        val widgetNames =
            io.healthplatform.chartcam.viewmodel.WidgetType.entries.associateWith {
                stringResource(
                    io.healthplatform.chartcam.ui
                        .getWidgetNameResource(it),
                )
            }
        val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(key = route.duplicateFromId ?: "new") {
                io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel(
                    repository = deps.questionnaireRepository,
                    duplicateFromId = route.duplicateFromId,
                    copyTitleResolver = { title ->
                        if (copyTemplate.contains("%1\$s") || copyTemplate.contains("%s")) {
                            copyTemplate.replace("%1\$s", title).replace("%s", title)
                        } else {
                            "$title (Copy)"
                        }
                    },
                    defaultItemLabelResolver = { newItemLabel },
                    widgetItemLabelResolver = { widgetType ->
                        val widgetName = widgetNames[widgetType] ?: widgetType.name
                        if (newWidgetTemplate.contains("%1\$s") || newWidgetTemplate.contains("%s")) {
                            newWidgetTemplate.replace("%1\$s", widgetName).replace("%s", widgetName)
                        } else {
                            "$widgetName - $newItemLabel"
                        }
                    },
                    unknownTitleResolver = { unknownLabel },
                )
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
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.questionnaireListDestination(
    navController: NavHostController,
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
