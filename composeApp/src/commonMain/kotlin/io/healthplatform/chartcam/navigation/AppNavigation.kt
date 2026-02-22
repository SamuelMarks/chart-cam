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
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.createSecureStorage
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.sync.SyncManager
import io.healthplatform.chartcam.ui.CaptureScreen
import io.healthplatform.chartcam.ui.EncounterDetailScreen
import io.healthplatform.chartcam.ui.LoginScreen
import io.healthplatform.chartcam.ui.PatientDetailScreen
import io.healthplatform.chartcam.ui.PatientListScreen
import io.healthplatform.chartcam.ui.TriageScreen
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import io.healthplatform.chartcam.database.DatabaseDriverFactory

class PhotoSessionManager {
    var pendingPhotos: Map<String, String> = emptyMap()
    
    fun setPhotos(photos: Map<String, String>) {
        pendingPhotos = photos
    }
    
    fun getAndClear(): Map<String, String> {
        val p = pendingPhotos
        pendingPhotos = emptyMap()
        return p
    }

    fun get(): Map<String, String> {
        return pendingPhotos
    }
}

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
    
    LaunchedEffect(Unit) {
        authRepository.checkSession()
        io.healthplatform.chartcam.initDatabase(driver)
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        
        composable(Routes.LOGIN) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { LoginViewModel(authRepository) }
            LaunchedEffect(user) {
                if (user != null) {
                    navController.navigate(Routes.PATIENT_LIST) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            }
            LoginScreen(viewModel = viewModel, onLoginSuccess = {
                navController.navigate(Routes.PATIENT_LIST) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }

        composable(Routes.CAPTURE) {
            CaptureScreen(
                questionnaireId = "std-form",
                questionnaireRepository = questionnaireRepository,
                onFinished = { outputPathsMap ->
                    if (outputPathsMap.isEmpty()) navController.navigate(Routes.PATIENT_LIST)
                    else {
                        photoSessionManager.setPhotos(outputPathsMap)
                        navController.navigate(TriageRoute)
                    }
                },
                onCancel = {
                    navController.navigate(Routes.PATIENT_LIST) {
                        popUpTo(Routes.CAPTURE) { inclusive = true }
                    }
                }
            )
        }

        composable<CaptureForPatientRoute> { entry ->
            val route = entry.toRoute<CaptureForPatientRoute>()
            val patientId = route.patientId
            CaptureScreen(
                questionnaireId = route.questionnaireId ?: "std-form",
                questionnaireRepository = questionnaireRepository,
                onFinished = { outputPathsMap ->
                    if (outputPathsMap.isEmpty()) {
                        navController.popBackStack()
                    } else {
                        photoSessionManager.setPhotos(outputPathsMap)
                        navController.navigate(VisitDetailRoute(patientId, "new")) {
                            popUpTo(PatientDetailRoute(patientId))
                        }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable<TriageRoute> { entry ->
            TriageScreen(
                capturedPhotoPaths = photoSessionManager.get(),
                fhirRepository = fhirRepository,
                onProceedToEncounter = { patientId, photos ->
                    navController.navigate(VisitDetailRoute(patientId, "new"))
                }
            )
        }
        
        composable<VisitDetailRoute> { entry ->
            val route = entry.toRoute<VisitDetailRoute>()
            val patientId = route.patientId
            val visitId = route.visitId
            
            EncounterDetailScreen(
                patientId = patientId,
                visitId = visitId,
                photosMap = photoSessionManager.getAndClear(),
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
                }
            )
        }
        
        composable(Routes.PATIENT_LIST) {
            PatientListScreen(
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
                }
            )
        }

        composable<PatientDetailRoute> { entry ->
            val route = entry.toRoute<PatientDetailRoute>()
            val patientId = route.patientId
            PatientDetailScreen(
                patientId = patientId,
                fhirRepository = fhirRepository,
                onBack = { navController.popBackStack() },
                onNewVisit = {
                    navController.navigate(VisitDetailRoute(patientId, "new"))
                },
                onVisitSelected = { visitId ->
                    navController.navigate(VisitDetailRoute(patientId, visitId))
                }
            )
        }

        composable<PatientVisitsRoute> { entry ->
            val route = entry.toRoute<PatientVisitsRoute>()
            val patientId = route.patientId
            PatientDetailScreen(
                patientId = patientId,
                fhirRepository = fhirRepository,
                onBack = { navController.popBackStack() },
                onNewVisit = {
                    navController.navigate(VisitDetailRoute(patientId, "new"))
                },
                onVisitSelected = { visitId ->
                    navController.navigate(VisitDetailRoute(patientId, visitId))
                }
            )
        }
    }
}
