/**
 * @file CaptureDestinations.kt
 * Contains declarations for CaptureDestinations.
 *
 * Defines the navigation graph builder extensions for photo capture and triage destinations.
 */
package io.healthplatform.chartcam.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.healthplatform.chartcam.ui.CaptureScreen
import io.healthplatform.chartcam.ui.TriageScreen
import kotlinx.coroutines.CoroutineScope

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
        androidx.compose.runtime.key(currentLang) {
            CaptureScreen(
                questionnaireId = "std-form",
                questionnaireRepository = deps.questionnaireRepository,
                onFinished = { outputPathsMap ->
                    if (outputPathsMap.isEmpty()) {
                        navController.navigate(Routes.PATIENT_LIST)
                    } else {
                        deps.photoSessionManager.setPhotos(outputPathsMap)
                        navController.navigate(TriageRoute)
                    }
                },
                onCancel = {
                    navController.navigate(Routes.PATIENT_LIST) {
                        popUpTo(Routes.CAPTURE) { inclusive = true }
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
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.captureForPatientDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<CaptureForPatientRoute> { entry ->
        val route = entry.toRoute<CaptureForPatientRoute>()
        androidx.compose.runtime.key(currentLang) {
            CaptureScreen(
                questionnaireId = route.questionnaireId ?: "std-form",
                linkId = route.linkId,
                questionnaireRepository = deps.questionnaireRepository,
                onFinished = { outputPathsMap ->
                    if (outputPathsMap.isNotEmpty()) {
                        deps.photoSessionManager.setPhotos(outputPathsMap)
                    }
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }
    }
}

/**
 * Registers the triage destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param scope The coroutine scope.
 * @param deps The application dependencies.
 * @param currentLang The current application language.
 */
fun NavGraphBuilder.triageDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    deps: AppDependencies,
    currentLang: String,
) {
    composable<TriageRoute> {
        androidx.compose.runtime.key(currentLang) {
            TriageScreen(
                capturedPhotoPaths = deps.photoSessionManager.get(),
                fhirRepository = deps.fhirRepository,
                onProceedToEncounter = { patientId, _ ->
                    navController.navigate(NewVisitRoute(patientId))
                },
            )
        }
    }
}
