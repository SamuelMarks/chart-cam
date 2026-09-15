/**
 * @file QuestionnaireDestinations.kt
 * Registers navigation destinations related to questionnaires.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.copy_title_fallback_format
import chartcam.chartcam.generated.resources.fitzpatrick_type_1
import chartcam.chartcam.generated.resources.fitzpatrick_type_2
import chartcam.chartcam.generated.resources.fitzpatrick_type_3
import chartcam.chartcam.generated.resources.fitzpatrick_type_4
import chartcam.chartcam.generated.resources.fitzpatrick_type_5
import chartcam.chartcam.generated.resources.fitzpatrick_type_6
import chartcam.chartcam.generated.resources.new_item
import chartcam.chartcam.generated.resources.new_widget_item
import chartcam.chartcam.generated.resources.new_widget_item_fallback_format
import chartcam.chartcam.generated.resources.severity_mild
import chartcam.chartcam.generated.resources.severity_moderate
import chartcam.chartcam.generated.resources.severity_severe
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
        QuestionnaireBuilderRouteScreen(
            route = route,
            deps = deps,
            currentLang = currentLang,
            onBack = { navController.popBackStack() },
            onSaved = { savedId ->
                navController.previousBackStackEntry?.savedStateHandle?.set("createdQuestionnaireId", savedId)
                navController.popBackStack()
            },
        )
    }
}

/**
 * Composable screen container for QuestionnaireBuilder destination.
 *
 * @param route The builder destination route parameters.
 * @param deps The application dependencies.
 * @param currentLang The active UI language code.
 * @param onBack Callback when navigating back.
 * @param onSaved Callback when a questionnaire is successfully saved.
 */
@Composable
private fun QuestionnaireBuilderRouteScreen(
    route: QuestionnaireBuilderRoute,
    deps: AppDependencies,
    currentLang: String,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val copyTemplate = stringResource(Res.string.unknown_copy)
    val copyFallbackTemplate = stringResource(Res.string.copy_title_fallback_format)
    val newItemLabel = stringResource(Res.string.new_item)
    val newWidgetTemplate = stringResource(Res.string.new_widget_item)
    val newWidgetFallbackTemplate = stringResource(Res.string.new_widget_item_fallback_format)
    val unknownLabel = stringResource(Res.string.unknown)
    val widgetNames =
        io.healthplatform.chartcam.viewmodel.WidgetType.entries.associateWith {
            stringResource(
                io.healthplatform.chartcam.ui
                    .getWidgetNameResource(it),
            )
        }
    val fitzpatrickTypes =
        listOf(
            stringResource(Res.string.fitzpatrick_type_1),
            stringResource(Res.string.fitzpatrick_type_2),
            stringResource(Res.string.fitzpatrick_type_3),
            stringResource(Res.string.fitzpatrick_type_4),
            stringResource(Res.string.fitzpatrick_type_5),
            stringResource(Res.string.fitzpatrick_type_6),
        )
    val defaultSeverityOptions =
        listOf(
            stringResource(Res.string.severity_mild),
            stringResource(Res.string.severity_moderate),
            stringResource(Res.string.severity_severe),
        )
    val viewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = route.duplicateFromId ?: "new") {
            io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel(
                repository = deps.questionnaireRepository,
                duplicateFromId = route.duplicateFromId,
                copyTitleResolver = { title ->
                    if (copyTemplate.contains("%1\$s") || copyTemplate.contains("%s")) {
                        copyTemplate.replace("%1\$s", title).replace("%s", title)
                    } else {
                        copyFallbackTemplate.replace("%1\$s", title).replace("%s", title)
                    }
                },
                defaultItemLabelResolver = { newItemLabel },
                widgetItemLabelResolver = { widgetType ->
                    val widgetName = widgetNames[widgetType] ?: widgetType.name
                    if (newWidgetTemplate.contains("%1\$s") || newWidgetTemplate.contains("%s")) {
                        newWidgetTemplate.replace("%1\$s", widgetName).replace("%s", widgetName)
                    } else {
                        newWidgetFallbackTemplate.replace("%1\$s", widgetName).replace("%2\$s", newItemLabel)
                    }
                },
                unknownTitleResolver = { unknownLabel },
                defaultOptionsResolver = { widgetType ->
                    when (widgetType) {
                        io.healthplatform.chartcam.viewmodel.WidgetType.FITZPATRICK_PALETTE -> fitzpatrickTypes
                        io.healthplatform.chartcam.viewmodel.WidgetType.SEGMENTED_TILES -> defaultSeverityOptions
                        else -> emptyList()
                    }
                },
            )
        }
    androidx.compose.runtime.key(currentLang) {
        io.healthplatform.chartcam.ui.QuestionnaireBuilderScreen(
            viewModel = viewModel,
            onBack = onBack,
            onSaved = onSaved,
        )
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
