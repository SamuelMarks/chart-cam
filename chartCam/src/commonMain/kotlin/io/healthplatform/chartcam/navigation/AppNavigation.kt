/**
 * @file AppNavigation.kt
 * Contains declarations for AppNavigation.kt.
 *
 * Configures the primary navigation graph and dependency injection points for the ChartCam application.
 * This file maps semantic routes to Compose Multiplatform screens and handles session state
 * such as the [PhotoSessionManager].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.createSecureStorage
import io.healthplatform.chartcam.sync.SyncWorker

/**
 * The main entry point for the application's UI navigation graph.
 *
 * This composable sets up the [NavHost], instantiates necessary dependencies,
 * and defines the navigation routes and their corresponding composable screens.
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 */
@Composable
fun AppNavigation() {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    SetupBrowserHistory(navController)

    val client = remember { NetworkClient.create() }
    val storage = remember { createSecureStorage() }
    val authRepository = remember { AuthRepository(client, storage) }

    val dbFactory = remember { DatabaseDriverFactory() }
    val driver = remember { dbFactory.createDriver() }
    val fhirRepository = remember { FhirRepository(driver) }
    val questionnaireRepository = remember { QuestionnaireRepository(fhirRepository) }

    val fileStorage = remember { createFileStorage() }
    val exportImportService = remember { ExportImportService(fhirRepository.database, fileStorage) }

    val syncWorker = remember { SyncWorker(fhirRepository, client) }

    val photoSessionManager = remember { PhotoSessionManager() }

    val dependencies =
        remember {
            AppDependencies(
                authRepository = authRepository,
                fhirRepository = fhirRepository,
                questionnaireRepository = questionnaireRepository,
                exportImportService = exportImportService,
                syncWorker = syncWorker,
                photoSessionManager = photoSessionManager,
            )
        }

    val user by authRepository.currentUser.collectAsState()
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()

    LaunchedEffect(Unit) {
        authRepository.checkSession()
        io.healthplatform.chartcam.initDatabase(driver)
        questionnaireRepository.loadDefaultForms()
    }

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        loginDestination(navController, dependencies, currentLang)
        captureDestination(navController, dependencies, currentLang)
        captureForPatientDestination(navController, dependencies, currentLang)
        triageDestination(navController, dependencies, currentLang)
        newVisitDestination(navController, scope, dependencies, currentLang)
        visitDetailDestination(navController, scope, dependencies, currentLang)
        patientListDestination(navController, dependencies, currentLang)
        patientDetailDestination(navController, dependencies, currentLang)
        patientVisitsDestination(navController, dependencies, currentLang)
        questionnaireBuilderDestination(navController, dependencies, currentLang)
        questionnaireListDestination(navController, dependencies, currentLang)
    }
}
