/**
 * @file CaptureDestinations.kt
 * Contains declarations for CaptureDestinations.
 *
 * Defines the navigation graph builder extensions for photo capture and triage destinations.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.healthplatform.chartcam.ui.CaptureScreen
import io.healthplatform.chartcam.ui.TriageScreen
import kotlinx.coroutines.launch

/**
 * Handles completion of photo capture session across capture destinations.
 *
 * @param outputPathsMap Mapping of questionnaire linkIds to captured image file paths.
 * @param navController The navigation controller.
 * @param photoSessionManager Session manager for captured photos.
 * @param isPatientFlow True if capture was initiated from a patient record, false otherwise.
 */
fun handleCaptureFinished(
    outputPathsMap: Map<String, String>,
    navController: NavHostController,
    photoSessionManager: PhotoSessionManager,
    isPatientFlow: Boolean = false,
) {
    if (isPatientFlow) {
        if (outputPathsMap.isNotEmpty()) {
            photoSessionManager.setPhotos(outputPathsMap)
        }
        navController.popBackStack()
    } else {
        if (outputPathsMap.isEmpty()) {
            navController.navigate(Routes.PATIENT_LIST)
        } else {
            photoSessionManager.setPhotos(outputPathsMap)
            navController.navigate(TriageRoute)
        }
    }
}

/**
 * Registers the capture destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.captureDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable(Routes.CAPTURE) {
        val scope = rememberCoroutineScope()
        key(currentLang) {
            CaptureScreen(
                questionnaireId = "std-form",
                questionnaireRepository = deps.questionnaireRepository,
                onFinished = { outputPathsMap ->
                    scope.launch {
                        handleCaptureFinished(
                            outputPathsMap = outputPathsMap,
                            navController = navController,
                            photoSessionManager = deps.photoSessionManager,
                            isPatientFlow = false,
                        )
                    }
                },
                onCancel = {
                    scope.launch {
                        navController.navigate(Routes.PATIENT_LIST) {
                            popUpTo(Routes.CAPTURE) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

/**
 * Registers the capture for patient destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.captureForPatientDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<CaptureForPatientRoute> { entry ->
        val route = entry.toRoute<CaptureForPatientRoute>()
        val scope = rememberCoroutineScope()
        key(currentLang) {
            CaptureScreen(
                questionnaireId = route.questionnaireId ?: "std-form",
                linkId = route.linkId,
                questionnaireRepository = deps.questionnaireRepository,
                onFinished = { outputPathsMap ->
                    scope.launch {
                        handleCaptureFinished(
                            outputPathsMap = outputPathsMap,
                            navController = navController,
                            photoSessionManager = deps.photoSessionManager,
                            isPatientFlow = true,
                        )
                    }
                },
                onCancel = {
                    scope.launch {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}

/**
 * Registers the triage destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.triageDestination(
    navController: NavHostController,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<TriageRoute> {
        val scope = rememberCoroutineScope()
        val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel {
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(deps.fhirRepository)
                    .apply {
                        setPaths(deps.photoSessionManager.get())
                    }
            }
        key(currentLang) {
            TriageScreen(
                viewModel = viewModel,
                onProceedToEncounter = { patientId, _ ->
                    scope.launch {
                        navController.navigate(NewVisitRoute(patientId))
                    }
                },
                onBack = {
                    scope.launch {
                        navController.popBackStack()
                    }
                },
                fileStorage =
                    deps.fileStorage ?: io.healthplatform.chartcam.files
                        .createFileStorage(),
            )
        }
    }
}
