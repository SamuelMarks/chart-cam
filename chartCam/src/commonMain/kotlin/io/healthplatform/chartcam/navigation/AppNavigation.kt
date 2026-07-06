package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.createSecureStorage
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.ui.CaptureScreen
import io.healthplatform.chartcam.ui.EncounterDetailScreen
import io.healthplatform.chartcam.ui.LoginScreen
import io.healthplatform.chartcam.ui.PatientDetailScreen
import io.healthplatform.chartcam.ui.PatientListScreen
import io.healthplatform.chartcam.ui.TriageScreen
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A manager class responsible for handling temporary photo session data during navigation.
 */
class PhotoSessionManager {
    /**
     * A private mutable state flow holding the pending photos as a map of photo IDs to file paths.
     */
    private val _pendingPhotos = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * A public read-only state flow representing the current pending photos.
     */
    val pendingPhotos = _pendingPhotos.asStateFlow()

    /**
     * Sets the pending photos for the current session.
     *
     * @param photos A map of photo IDs to their corresponding file paths.
     */
    fun setPhotos(photos: Map<String, String>) {
        _pendingPhotos.value = photos
    }

    /**
     * Retrieves the current pending photos and clears the session state.
     *
     * @return A map of photo IDs to their file paths that were previously set.
     */
    fun getAndClear(): Map<String, String> {
        val p = _pendingPhotos.value
        _pendingPhotos.value = emptyMap()
        return p
    }

    /**
     * Retrieves the current pending photos without clearing the session state.
     *
     * @return A map of photo IDs to their file paths.
     */
    fun get(): Map<String, String> = _pendingPhotos.value
}

/**
 * The main entry point for the application's UI navigation graph.
 *
 * This composable sets up the [NavHost], instantiates necessary dependencies,
 * and defines the navigation routes and their corresponding composable screens.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    SetupBrowserHistory(navController)

    val client = remember { NetworkClient.create() }
    val storage = remember { createSecureStorage() }
    val authRepository = remember { AuthRepository(client, storage) }

    val dbFactory = remember { DatabaseDriverFactory() }
    val driver = remember { dbFactory.createDriver() }
    val fhirRepository = remember { FhirRepository(driver) }
    val questionnaireRepository = remember { QuestionnaireRepository() }

    val fileStorage = remember { createFileStorage() }
    val exportImportService = remember { ExportImportService(fhirRepository.database, fileStorage) }

    val syncManager = remember { SyncManager(fhirRepository, client, fileStorage) }

    val photoSessionManager = remember { PhotoSessionManager() }

    val user by authRepository.currentUser.collectAsState()
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()

    LaunchedEffect(Unit) {
        authRepository.checkSession()
        io.healthplatform.chartcam.initDatabase(driver)
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            val viewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel { LoginViewModel(authRepository) }
            LaunchedEffect(user) {
                if (user != null) {
                    navController.navigate(Routes.PATIENT_LIST) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            }
            androidx.compose.runtime.key(currentLang) {
                LoginScreen(viewModel = viewModel, onLoginSuccess = {
                    navController.navigate(Routes.PATIENT_LIST) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
        }

        composable(Routes.CAPTURE) {
            androidx.compose.runtime.key(currentLang) {
                CaptureScreen(
                    questionnaireId = "std-form",
                    questionnaireRepository = questionnaireRepository,
                    onFinished = { outputPathsMap ->
                        if (outputPathsMap.isEmpty()) {
                            navController.navigate(Routes.PATIENT_LIST)
                        } else {
                            photoSessionManager.setPhotos(outputPathsMap)
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

        composable<CaptureForPatientRoute> { entry ->
            val route = entry.toRoute<CaptureForPatientRoute>()
            val patientId = route.patientId
            androidx.compose.runtime.key(currentLang) {
                CaptureScreen(
                    questionnaireId = route.questionnaireId ?: "std-form",
                    questionnaireRepository = questionnaireRepository,
                    onFinished = { outputPathsMap ->
                        if (outputPathsMap.isEmpty()) {
                            navController.popBackStack()
                        } else {
                            photoSessionManager.setPhotos(outputPathsMap)
                            navController.popBackStack()
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                )
            }
        }

        composable<TriageRoute> { entry ->
            androidx.compose.runtime.key(currentLang) {
                TriageScreen(
                    capturedPhotoPaths = photoSessionManager.get(),
                    fhirRepository = fhirRepository,
                    onProceedToEncounter = { patientId, photos ->
                        navController.navigate(NewVisitRoute(patientId))
                    },
                )
            }
        }

        composable<NewVisitRoute> { entry ->
            val route = entry.toRoute<NewVisitRoute>()
            val patientId = route.patientId

            androidx.compose.runtime.key(currentLang) {
                EncounterDetailScreen(
                    patientId = patientId,
                    visitId = "new",
                    photoSessionManager = photoSessionManager,
                    fhirRepository = fhirRepository,
                    authRepository = authRepository,
                    syncManager = syncManager,
                    questionnaireRepository = questionnaireRepository,
                    onBack = { navController.popBackStack() },
                    onTakePhotos = { qId ->
                        navController.navigate(CaptureForPatientRoute(patientId, qId))
                    },
                    onFinalized = {
                        navController.navigate(PatientDetailRoute(patientId)) {
                            popUpTo(PatientDetailRoute(patientId)) { inclusive = true }
                        }
                    },
                    onVisitCreated = { newId ->
                        navController.navigate(VisitDetailRoute(patientId, newId)) {
                            popUpTo<NewVisitRoute> { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<VisitDetailRoute> { entry ->
            val route = entry.toRoute<VisitDetailRoute>()
            val patientId = route.patientId
            val visitId = route.visitId

            androidx.compose.runtime.key(currentLang) {
                EncounterDetailScreen(
                    patientId = patientId,
                    visitId = visitId,
                    photoSessionManager = photoSessionManager,
                    fhirRepository = fhirRepository,
                    authRepository = authRepository,
                    syncManager = syncManager,
                    questionnaireRepository = questionnaireRepository,
                    onBack = { navController.popBackStack() },
                    onTakePhotos = { qId ->
                        navController.navigate(CaptureForPatientRoute(patientId, qId))
                    },
                    onFinalized = {
                        navController.navigate(PatientDetailRoute(patientId)) {
                            popUpTo(PatientDetailRoute(patientId)) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(Routes.PATIENT_LIST) {
            androidx.compose.runtime.key(currentLang) {
                PatientListScreen(
                    authRepository = authRepository,
                    fhirRepository = fhirRepository,
                    exportImportService = exportImportService,
                    onPatientSelected = { patientId ->
                        navController.navigate(PatientDetailRoute(patientId))
                    },
                    onLogout = {
                        authRepository.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<PatientDetailRoute> { entry ->
            val route = entry.toRoute<PatientDetailRoute>()
            val patientId = route.patientId
            androidx.compose.runtime.key(currentLang) {
                PatientDetailScreen(
                    patientId = patientId,
                    fhirRepository = fhirRepository,
                    onBack = { navController.popBackStack() },
                    onNewVisit = {
                        navController.navigate(NewVisitRoute(patientId))
                    },
                    onVisitSelected = { visitId ->
                        navController.navigate(VisitDetailRoute(patientId, visitId))
                    },
                )
            }
        }

        composable<PatientVisitsRoute> { entry ->
            val route = entry.toRoute<PatientVisitsRoute>()
            val patientId = route.patientId
            androidx.compose.runtime.key(currentLang) {
                PatientDetailScreen(
                    patientId = patientId,
                    fhirRepository = fhirRepository,
                    onBack = { navController.popBackStack() },
                    onNewVisit = {
                        navController.navigate(NewVisitRoute(patientId))
                    },
                    onVisitSelected = { visitId ->
                        navController.navigate(VisitDetailRoute(patientId, visitId))
                    },
                )
            }
        }
    }
}
