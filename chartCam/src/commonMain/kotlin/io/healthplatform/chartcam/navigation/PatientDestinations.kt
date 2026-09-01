/**
 * @file PatientDestinations.kt
 * Contains declarations for PatientDestinations.
 *
 * Defines the navigation graph builder extensions for patient-related destinations.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.healthplatform.chartcam.ui.EncounterDetailScreen
import io.healthplatform.chartcam.ui.PatientDetailScreen
import io.healthplatform.chartcam.ui.PatientListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                    io.healthplatform.chartcam.ui.EncounterDetailDependencies(
                        photoSessionManager = deps.photoSessionManager,
                        fhirRepository = deps.fhirRepository,
                        authRepository = deps.authRepository,
                        questionnaireRepository = deps.questionnaireRepository,
                    ),
                actions =
                    io.healthplatform.chartcam.ui.EncounterDetailActions(
                        onBack = { navController.popBackStack() },
                        onTakePhotos = { qId, linkId ->
                            scope.launch {
                                navController.navigate(CaptureForPatientRoute(patientId, qId, linkId))
                            }
                        },
                        onCreateNewQuestionnaire = {
                            navController.navigate(
                                io.healthplatform.chartcam.navigation
                                    .QuestionnaireBuilderRoute(),
                            )
                        },
                        onFinalized = {
                            navController.popBackStack(PatientDetailRoute(patientId), inclusive = false)
                        },
                        onNewlyCreatedQuestionnaireHandled = {
                            entry.savedStateHandle.remove<String>("createdQuestionnaireId")
                        },
                    ),
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
                    io.healthplatform.chartcam.ui.PatientListDependencies(
                        fhirRepository = deps.fhirRepository,
                        exportImportService = deps.exportImportService,
                        authRepository = deps.authRepository,
                    ),
                actions =
                    io.healthplatform.chartcam.ui.PatientListActions(
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
                    ),
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
        androidx.compose.runtime.key(currentLang) {
            PatientDetailScreen(
                patientId = patientId,
                fhirRepository = deps.fhirRepository,
                onBack = { navController.popBackStack() },
                onNewVisit = { navController.navigate(NewVisitRoute(patientId)) },
                onVisitSelected = { visitId ->
                    navController.navigate(VisitDetailRoute(patientId, visitId))
                },
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
        androidx.compose.runtime.key(currentLang) {
            PatientDetailScreen(
                patientId = patientId,
                fhirRepository = deps.fhirRepository,
                onBack = { navController.popBackStack() },
                onNewVisit = { navController.navigate(NewVisitRoute(patientId)) },
                onVisitSelected = { visitId ->
                    navController.navigate(VisitDetailRoute(patientId, visitId))
                },
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
                    io.healthplatform.chartcam.ui.EncounterDetailDependencies(
                        photoSessionManager = deps.photoSessionManager,
                        fhirRepository = deps.fhirRepository,
                        authRepository = deps.authRepository,
                        questionnaireRepository = deps.questionnaireRepository,
                    ),
                actions =
                    io.healthplatform.chartcam.ui.EncounterDetailActions(
                        onBack = { navController.popBackStack() },
                        onTakePhotos = { qId, linkId ->
                            scope.launch {
                                navController.navigate(CaptureForPatientRoute(patientId, qId, linkId))
                            }
                        },
                        onCreateNewQuestionnaire = {
                            navController.navigate(
                                io.healthplatform.chartcam.navigation
                                    .QuestionnaireBuilderRoute(),
                            )
                        },
                        onFinalized = {
                            navController.popBackStack()
                        },
                        onNewlyCreatedQuestionnaireHandled = {
                            entry.savedStateHandle.remove<String>("createdQuestionnaireId")
                        },
                    ),
                newlyCreatedQuestionnaireId = newlyCreatedQuestionnaireId,
            )
        }
    }
}
