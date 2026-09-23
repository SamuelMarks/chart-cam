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
import io.healthplatform.chartcam.ui.getWidgetNameResource
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
import org.jetbrains.compose.resources.stringResource

/**
 * Callbacks for questionnaire builder destination actions.
 *
 * @property onBack Callback when user navigates back.
 * @property onSaved Callback when a questionnaire is saved.
 */
data class QuestionnaireBuilderNavActions(
    val onBack: () -> Unit,
    val onSaved: (String) -> Unit,
)

/**
 * Builds the actions for the questionnaire builder destination.
 *
 * @param navController The navigation controller.
 * @return QuestionnaireBuilderNavActions configured for the builder destination.
 */
fun buildQuestionnaireBuilderActions(
    navController: NavHostController,
): QuestionnaireBuilderNavActions =
    QuestionnaireBuilderNavActions(
        onBack = { navController.popBackStack() },
        onSaved = { savedId ->
            navController.previousBackStackEntry?.let { entry ->
                entry.savedStateHandle.set("createdQuestionnaireId", savedId)
            }
            navController.popBackStack()
        },
    )

/**
 * Callbacks for questionnaire list destination actions.
 *
 * @property onBack Callback when user navigates back.
 * @property onNavigateToBuilder Callback when navigating to questionnaire builder.
 */
data class QuestionnaireListNavActions(
    val onBack: () -> Unit,
    val onNavigateToBuilder: (String?) -> Unit,
)

/**
 * Builds the actions for the questionnaire list destination.
 *
 * @param navController The navigation controller.
 * @return QuestionnaireListNavActions configured for the list destination.
 */
fun buildQuestionnaireListActions(
    navController: NavHostController,
): QuestionnaireListNavActions =
    QuestionnaireListNavActions(
        onBack = { navController.popBackStack() },
        onNavigateToBuilder = { duplicateId ->
            navController.navigate(
                QuestionnaireBuilderRoute(duplicateFromId = duplicateId),
            )
        },
    )

/**
 * Resolves the copy title for a duplicated questionnaire using template strings.
 *
 * @param copyTemplate The primary copy format template.
 * @param copyFallbackTemplate The fallback copy format template.
 * @param title The original title being copied.
 * @return The formatted copy title string.
 */
fun resolveCopyTitle(
    copyTemplate: String,
    copyFallbackTemplate: String,
    title: String,
): String =
    if (copyTemplate.contains("%1\$s") || copyTemplate.contains("%s")) {
        copyTemplate.replace("%1\$s", title).replace("%s", title)
    } else {
        copyFallbackTemplate.replace("%1\$s", title).replace("%s", title)
    }

/**
 * Resolves the display label for a new item of a specific widget type.
 *
 * @param newWidgetTemplate The primary new widget format template.
 * @param newWidgetFallbackTemplate The fallback new widget format template.
 * @param newItemLabel The label for a generic new item.
 * @param widgetNames Mapping of widget types to localized names.
 * @param widgetType The widget type of the item being added.
 * @return The formatted new widget label string.
 */
fun resolveWidgetItemLabel(
    newWidgetTemplate: String,
    newWidgetFallbackTemplate: String,
    newItemLabel: String,
    widgetNames: Map<WidgetType, String>,
    widgetType: WidgetType,
): String {
    val widgetName = widgetNames[widgetType] ?: widgetType.name
    return if (newWidgetTemplate.contains("%1\$s") || newWidgetTemplate.contains("%s")) {
        newWidgetTemplate.replace("%1\$s", widgetName).replace("%s", widgetName)
    } else {
        newWidgetFallbackTemplate.replace("%1\$s", widgetName).replace("%2\$s", newItemLabel)
    }
}

/**
 * Resolves default option choices for supported widget types.
 *
 * @param fitzpatrickTypes Pre-localized Fitzpatrick skin phototype descriptions.
 * @param defaultSeverityOptions Pre-localized clinical severity options.
 * @param widgetType The widget type to retrieve default options for.
 * @return List of option strings for the given widget type.
 */
fun resolveDefaultOptions(
    fitzpatrickTypes: List<String>,
    defaultSeverityOptions: List<String>,
    widgetType: WidgetType,
): List<String> =
    when (widgetType) {
        WidgetType.FITZPATRICK_PALETTE -> fitzpatrickTypes
        WidgetType.SEGMENTED_TILES -> defaultSeverityOptions
        else -> emptyList()
    }

/**
 * Factory function creating a [QuestionnaireBuilderViewModel] for the builder route.
 *
 * @param repository The questionnaire repository.
 * @param duplicateFromId The optional ID of questionnaire to duplicate.
 * @param copyTemplate The primary copy format template.
 * @param copyFallbackTemplate The fallback copy format template.
 * @param newItemLabel The default label for new items.
 * @param newWidgetTemplate The primary new widget format template.
 * @param newWidgetFallbackTemplate The fallback new widget format template.
 * @param unknownLabel The fallback unknown label.
 * @param widgetNames Mapping of widget types to localized names.
 * @param fitzpatrickTypes Pre-localized Fitzpatrick skin phototypes.
 * @param defaultSeverityOptions Pre-localized clinical severity options.
 * @return The configured QuestionnaireBuilderViewModel.
 */
fun createQuestionnaireBuilderViewModel(
    repository: io.healthplatform.chartcam.repository.QuestionnaireRepository,
    duplicateFromId: String?,
    copyTemplate: String,
    copyFallbackTemplate: String,
    newItemLabel: String,
    newWidgetTemplate: String,
    newWidgetFallbackTemplate: String,
    unknownLabel: String,
    widgetNames: Map<WidgetType, String>,
    fitzpatrickTypes: List<String>,
    defaultSeverityOptions: List<String>,
): QuestionnaireBuilderViewModel =
    QuestionnaireBuilderViewModel(
        repository = repository,
        duplicateFromId = duplicateFromId,
        copyTitleResolver = { title ->
            resolveCopyTitle(copyTemplate, copyFallbackTemplate, title)
        },
        defaultItemLabelResolver = { newItemLabel },
        widgetItemLabelResolver = { widgetType ->
            resolveWidgetItemLabel(
                newWidgetTemplate,
                newWidgetFallbackTemplate,
                newItemLabel,
                widgetNames,
                widgetType,
            )
        },
        unknownTitleResolver = { unknownLabel },
        defaultOptionsResolver = { widgetType ->
            resolveDefaultOptions(fitzpatrickTypes, defaultSeverityOptions, widgetType)
        },
    )

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
        val actions = buildQuestionnaireBuilderActions(navController)
        QuestionnaireBuilderRouteScreen(
            route = route,
            deps = deps,
            currentLang = currentLang,
            onBack = actions.onBack,
            onSaved = actions.onSaved,
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
fun QuestionnaireBuilderRouteScreen(
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
        WidgetType.entries.associateWith {
            stringResource(
                getWidgetNameResource(it),
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
            createQuestionnaireBuilderViewModel(
                repository = deps.questionnaireRepository,
                duplicateFromId = route.duplicateFromId,
                copyTemplate = copyTemplate,
                copyFallbackTemplate = copyFallbackTemplate,
                newItemLabel = newItemLabel,
                newWidgetTemplate = newWidgetTemplate,
                newWidgetFallbackTemplate = newWidgetFallbackTemplate,
                unknownLabel = unknownLabel,
                widgetNames = widgetNames,
                fitzpatrickTypes = fitzpatrickTypes,
                defaultSeverityOptions = defaultSeverityOptions,
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
        val actions = buildQuestionnaireListActions(navController)
        androidx.compose.runtime.key(currentLang) {
            QuestionnaireListScreen(
                questionnaireRepository = deps.questionnaireRepository,
                onBack = actions.onBack,
                onNavigateToBuilder = actions.onNavigateToBuilder,
            )
        }
    }
}
