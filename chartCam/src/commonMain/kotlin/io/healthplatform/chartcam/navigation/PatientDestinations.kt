/**
 * @file PatientDestinations.kt
 * Contains declarations for PatientDestinations.
 *
 * Defines the navigation graph builder extensions for patient-related destinations.
 */
@file:Suppress("MatchingDeclarationName")

package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.healthplatform.chartcam.ui.EncounterDetailActions
import io.healthplatform.chartcam.ui.EncounterDetailDependencies
import io.healthplatform.chartcam.ui.EncounterDetailScreen
import io.healthplatform.chartcam.ui.PatientDetailScreen
import io.healthplatform.chartcam.ui.PatientListActions
import io.healthplatform.chartcam.ui.PatientListDependencies
import io.healthplatform.chartcam.ui.PatientListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Callbacks for navigating from PatientDetail destinations.
 *
 * @property onBack Callback to navigate back.
 * @property onNewVisit Callback to navigate to create a new visit for the patient.
 * @property onVisitSelected Callback to navigate to a specific visit detail.
 */
data class PatientDetailNavActions(
    val onBack: () -> Unit,
    val onNewVisit: () -> Unit,
    val onVisitSelected: (String) -> Unit,
)

/**
 * Builds the actions for EncounterDetailScreen in new visit flow.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope for launching navigation tasks.
 * @param entry The navigation back stack entry.
 * @param patientId The ID of the patient.
 * @return EncounterDetailActions configured for the new visit destination.
 */
fun buildNewVisitActions(
    navController: NavHostController,
    scope: CoroutineScope,
    entry: NavBackStackEntry,
    patientId: String,
): EncounterDetailActions =
    EncounterDetailActions(
        onBack = { navController.popBackStack() },
        onTakePhotos = { qId, linkId ->
            scope.launch {
                navController.navigate(CaptureForPatientRoute(patientId, qId, linkId))
            }
        },
        onCreateNewQuestionnaire = {
            navController.navigate(
                QuestionnaireBuilderRoute(),
            )
        },
        onFinalized = {
            navController.popBackStack(PatientDetailRoute(patientId), inclusive = false)
        },
        onNewlyCreatedQuestionnaireHandled = {
            entry.savedStateHandle.remove<String>("createdQuestionnaireId")
        },
        onOpenDicomViewer = { path ->
            navController.navigate(DicomViewerRoute(filePath = path))
        },
    )

/**
 * Builds the actions for EncounterDetailScreen in past visit detail flow.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope for launching navigation tasks.
 * @param entry The navigation back stack entry.
 * @param patientId The ID of the patient.
 * @return EncounterDetailActions configured for the visit detail destination.
 */
fun buildVisitDetailActions(
    navController: NavHostController,
    scope: CoroutineScope,
    entry: NavBackStackEntry,
    patientId: String,
): EncounterDetailActions =
    EncounterDetailActions(
        onBack = { navController.popBackStack() },
        onTakePhotos = { qId, linkId ->
            scope.launch {
                navController.navigate(CaptureForPatientRoute(patientId, qId, linkId))
            }
        },
        onCreateNewQuestionnaire = {
            navController.navigate(
                QuestionnaireBuilderRoute(),
            )
        },
        onFinalized = {
            navController.popBackStack()
        },
        onNewlyCreatedQuestionnaireHandled = {
            entry.savedStateHandle.remove<String>("createdQuestionnaireId")
        },
        onOpenDicomViewer = { path ->
            navController.navigate(DicomViewerRoute(filePath = path))
        },
    )

/**
 * Builds the navigation actions for the patient list screen.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @return PatientListActions configured for the patient list destination.
 */
fun buildPatientListActions(
    navController: NavHostController,
    deps: AppDependencies,
): PatientListActions =
    PatientListActions(
        onPatientSelected = { patientId ->
            navController.navigate(PatientDetailRoute(patientId))
        },
        onNavigateToQuestionnaires = {
            navController.navigate(Routes.QUESTIONNAIRE_LIST)
        },
        onLogout = {
            deps.authRepository.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        },
    )

/**
 * Builds the navigation actions for the patient detail and patient visits screens.
 *
 * @param navController The navigation controller.
 * @param patientId The ID of the patient.
 * @return PatientDetailNavActions configured for the patient detail destinations.
 */
fun buildPatientDetailActions(
    navController: NavHostController,
    patientId: String,
): PatientDetailNavActions =
    PatientDetailNavActions(
        onBack = { navController.popBackStack() },
        onNewVisit = { navController.navigate(NewVisitRoute(patientId)) },
        onVisitSelected = { visitId ->
            navController.navigate(VisitDetailRoute(patientId, visitId))
        },
    )

/**
 * Registers the new visit destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.newVisitDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<NewVisitRoute> { entry ->
        val route = entry.toRoute<NewVisitRoute>()
        val patientId = route.patientId
        val newlyCreatedQuestionnaireId by entry.savedStateHandle
            .getStateFlow<String?>("createdQuestionnaireId", null)
            .collectAsState()

        androidx.compose.runtime.key(currentLang) {
            EncounterDetailScreen(
                patientId = patientId,
                visitId = "new",
                dependencies =
                    EncounterDetailDependencies(
                        photoSessionManager = deps.photoSessionManager,
                        fhirRepository = deps.fhirRepository,
                        authRepository = deps.authRepository,
                        questionnaireRepository = deps.questionnaireRepository,
                    ),
                actions = buildNewVisitActions(navController, scope, entry, patientId),
                newlyCreatedQuestionnaireId = newlyCreatedQuestionnaireId,
            )
        }
    }
}

/**
 * Registers the patient list destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.patientListDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable(Routes.PATIENT_LIST) {
        androidx.compose.runtime.key(currentLang) {
            PatientListScreen(
                dependencies =
                    PatientListDependencies(
                        fhirRepository = deps.fhirRepository,
                        exportImportService = deps.exportImportService,
                        authRepository = deps.authRepository,
                    ),
                actions = buildPatientListActions(navController, deps),
            )
        }
    }
}

/**
 * Registers the patient detail destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.patientDetailDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<PatientDetailRoute> { entry ->
        val route = entry.toRoute<PatientDetailRoute>()
        val patientId = route.patientId
        val actions = buildPatientDetailActions(navController, patientId)
        val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(key = patientId) {
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(
                        fhirRepository = deps.fhirRepository,
                        fileStorage = deps.fileStorage,
                    ).apply {
                        loadPatientData(patientId)
                    }
            }
        androidx.compose.runtime.key(currentLang) {
            PatientDetailScreen(
                viewModel = viewModel,
                onBack = actions.onBack,
                onNewVisit = actions.onNewVisit,
                onVisitSelected = actions.onVisitSelected,
            )
        }
    }
}

/**
 * Registers the patient visits destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.patientVisitsDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<PatientVisitsRoute> { entry ->
        val route = entry.toRoute<PatientVisitsRoute>()
        val patientId = route.patientId
        val actions = buildPatientDetailActions(navController, patientId)
        val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(key = patientId) {
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(
                        fhirRepository = deps.fhirRepository,
                        fileStorage = deps.fileStorage,
                    ).apply {
                        loadPatientData(patientId)
                    }
            }
        androidx.compose.runtime.key(currentLang) {
            PatientDetailScreen(
                viewModel = viewModel,
                onBack = actions.onBack,
                onNewVisit = actions.onNewVisit,
                onVisitSelected = actions.onVisitSelected,
            )
        }
    }
}

/**
 * Registers the visit detail destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.visitDetailDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<VisitDetailRoute> { entry ->
        val route = entry.toRoute<VisitDetailRoute>()
        val patientId = route.patientId
        val visitId = route.visitId
        val newlyCreatedQuestionnaireId by entry.savedStateHandle
            .getStateFlow<String?>("createdQuestionnaireId", null)
            .collectAsState()

        androidx.compose.runtime.key(currentLang) {
            EncounterDetailScreen(
                patientId = patientId,
                visitId = visitId,
                dependencies =
                    EncounterDetailDependencies(
                        photoSessionManager = deps.photoSessionManager,
                        fhirRepository = deps.fhirRepository,
                        authRepository = deps.authRepository,
                        questionnaireRepository = deps.questionnaireRepository,
                    ),
                actions = buildVisitDetailActions(navController, scope, entry, patientId),
                newlyCreatedQuestionnaireId = newlyCreatedQuestionnaireId,
            )
        }
    }
}
