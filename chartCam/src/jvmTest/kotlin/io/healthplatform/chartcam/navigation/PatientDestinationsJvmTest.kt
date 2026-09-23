/**
 * @file PatientDestinationsJvmTest.kt
 * Contains declarations for PatientDestinationsJvmTest.kt.
 *
 * JVM Compose UI tests for patient-related navigation destinations in [PatientDestinations.kt].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.EncounterDetailActions
import io.healthplatform.chartcam.ui.PatientListActions
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class PatientTestMemoryStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

private class PatientTestFileStorage : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

private fun createTestDependencies(): AppDependencies {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ChartCamDatabase.Schema.synchronous().create(driver)
    val database = ChartCamDatabase(driver)
    val secureStorage = PatientTestMemoryStorage()
    val authRepo = AuthRepository(secureStorage)
    val fhirRepo = FhirRepository(driver)
    val qRepo = QuestionnaireRepository(fhirRepo)
    val fileStorage = PatientTestFileStorage()
    val eiService = ExportImportService(database, fileStorage)
    val photoMgr = PhotoSessionManager()

    return AppDependencies(
        authRepository = authRepo,
        fhirRepository = fhirRepo,
        questionnaireRepository = qRepo,
        exportImportService = eiService,
        photoSessionManager = photoMgr,
        fileStorage = fileStorage,
    )
}

/**
 * JVM test suite verifying [PatientDestinations.kt] navigation routes, action factories, and Composable content.
 */
@OptIn(ExperimentalTestApi::class)
class PatientDestinationsJvmTest {
    /**
     * Verifies all callbacks built by [buildNewVisitActions].
     */
    @Test
    fun testBuildNewVisitActions() =
        runComposeUiTest {
            var capturedActions: EncounterDetailActions? = null
            var capturedEntry: androidx.navigation.NavBackStackEntry? = null

            setContent {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "root") {
                    composable("root") { Text("Root") }
                    composable<PatientDetailRoute> { Text("PatientDetail") }
                    composable<NewVisitRoute> { entry ->
                        capturedEntry = entry
                        capturedActions = buildNewVisitActions(navController, scope, entry, "pat-123")
                        Text("NewVisitScreen")
                    }
                    composable<CaptureForPatientRoute> { Text("CaptureForPatient") }
                    composable<QuestionnaireBuilderRoute> { Text("Builder") }
                    composable<DicomViewerRoute> { Text("Dicom") }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(PatientDetailRoute("pat-123"))
                    navController.navigate(NewVisitRoute("pat-123"))
                }
            }

            waitForIdle()
            runOnIdle {
                val actions = checkNotNull(capturedActions)
                val entry = checkNotNull(capturedEntry)

                entry.savedStateHandle.set("createdQuestionnaireId", "q-new")
                assertEquals("q-new", entry.savedStateHandle.get<String>("createdQuestionnaireId"))

                actions.onNewlyCreatedQuestionnaireHandled()
                assertNull(entry.savedStateHandle.get<String>("createdQuestionnaireId"))

                actions.onTakePhotos("q-1", "link-1")
                actions.onCreateNewQuestionnaire()
                actions.onOpenDicomViewer?.invoke("path/to/dicom.dcm")
                actions.onFinalized()
                actions.onBack()
            }
            waitForIdle()
        }

    /**
     * Verifies all callbacks built by [buildVisitDetailActions].
     */
    @Test
    fun testBuildVisitDetailActions() =
        runComposeUiTest {
            var capturedActions: EncounterDetailActions? = null
            var capturedEntry: androidx.navigation.NavBackStackEntry? = null

            setContent {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "root") {
                    composable("root") { Text("Root") }
                    composable<VisitDetailRoute> { entry ->
                        capturedEntry = entry
                        capturedActions = buildVisitDetailActions(navController, scope, entry, "pat-456")
                        Text("VisitDetailScreen")
                    }
                    composable<CaptureForPatientRoute> { Text("CaptureForPatient") }
                    composable<QuestionnaireBuilderRoute> { Text("Builder") }
                    composable<DicomViewerRoute> { Text("Dicom") }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(VisitDetailRoute("pat-456", "vis-1"))
                }
            }

            waitForIdle()
            runOnIdle {
                val actions = checkNotNull(capturedActions)
                val entry = checkNotNull(capturedEntry)

                entry.savedStateHandle.set("createdQuestionnaireId", "q-created")
                assertEquals("q-created", entry.savedStateHandle.get<String>("createdQuestionnaireId"))

                actions.onNewlyCreatedQuestionnaireHandled()
                assertNull(entry.savedStateHandle.get<String>("createdQuestionnaireId"))

                actions.onTakePhotos("q-2", "link-2")
                actions.onCreateNewQuestionnaire()
                actions.onOpenDicomViewer?.invoke("file.dcm")
                actions.onFinalized()
                actions.onBack()
            }
            waitForIdle()
        }

    /**
     * Verifies all callbacks built by [buildPatientListActions].
     */
    @Test
    fun testBuildPatientListActions() =
        runComposeUiTest {
            val deps = createTestDependencies()
            deps.authRepository.login("dr_test", "password123")
            var capturedActions: PatientListActions? = null

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.PATIENT_LIST) {
                    composable(Routes.PATIENT_LIST) {
                        capturedActions = buildPatientListActions(navController, deps)
                        Text("PatientListScreen")
                    }
                    composable<PatientDetailRoute> { Text("PatientDetail") }
                    composable(Routes.QUESTIONNAIRE_LIST) { Text("Questionnaires") }
                    composable(Routes.LOGIN) { Text("Login") }
                }
            }

            waitForIdle()
            runOnIdle {
                val actions = checkNotNull(capturedActions)
                actions.onPatientSelected("pat-789")
                actions.onNavigateToQuestionnaires()
                actions.onLogout()
            }
            waitForIdle()
            assertNull(deps.authRepository.currentUser.value)
        }

    /**
     * Verifies all callbacks built by [buildPatientDetailActions].
     */
    @Test
    fun testBuildPatientDetailActions() =
        runComposeUiTest {
            var capturedActions: PatientDetailNavActions? = null

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "root") {
                    composable("root") { Text("Root") }
                    composable<PatientDetailRoute> {
                        capturedActions = buildPatientDetailActions(navController, "pat-111")
                        Text("PatientDetailScreen")
                    }
                    composable<NewVisitRoute> { Text("NewVisit") }
                    composable<VisitDetailRoute> { Text("VisitDetail") }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(PatientDetailRoute("pat-111"))
                }
            }

            waitForIdle()
            runOnIdle {
                val actions = checkNotNull(capturedActions)
                actions.onNewVisit()
                actions.onVisitSelected("vis-99")
                actions.onBack()
            }
            waitForIdle()
        }

    /**
     * Verifies [newVisitDestination] in NavHost rendering [EncounterDetailScreen].
     */
    @Test
    fun testNewVisitDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            deps.authRepository.login("dr_test", "password123")
            val patient =
                createFhirPatient(
                    id = "pat-new-visit",
                    firstName = "Alice",
                    lastName = "Smith",
                    dob = LocalDate(1985, 5, 20),
                    mrnValue = "MRN-ALICE",
                )
            runBlocking { deps.fhirRepository.savePatient(patient) }

            setContent {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = NewVisitRoute("pat-new-visit")) {
                    newVisitDestination(navController, scope, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Smith", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Smith", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [visitDetailDestination] in NavHost rendering [EncounterDetailScreen].
     */
    @Test
    fun testVisitDetailDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            deps.authRepository.login("dr_test", "password123")
            val patient =
                createFhirPatient(
                    id = "pat-visit-detail",
                    firstName = "Bob",
                    lastName = "Jones",
                    dob = LocalDate(1975, 3, 15),
                    mrnValue = "MRN-BOB",
                )
            val enc =
                io.healthplatform.chartcam.models.createFhirEncounter(
                    id = "vis-101",
                    patientId = "pat-visit-detail",
                    practitionerId = "dr_test",
                    dateStr = "2026-09-14T10:00:00Z",
                )
            runBlocking {
                deps.fhirRepository.savePatient(patient)
                deps.fhirRepository.saveEncounter(enc)
            }

            setContent {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = VisitDetailRoute("pat-visit-detail", "vis-101")) {
                    visitDetailDestination(navController, scope, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Jones", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Jones", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [patientListDestination] in NavHost rendering [PatientListScreen].
     */
    @Test
    fun testPatientListDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.PATIENT_LIST) {
                    patientListDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Patient Directory", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Patient Directory", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [patientDetailDestination] in NavHost rendering [PatientDetailScreen].
     */
    @Test
    fun testPatientDetailDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            val patient =
                createFhirPatient(
                    id = "pat-detail-nav",
                    firstName = "Charlie",
                    lastName = "Brown",
                    dob = LocalDate(1960, 10, 10),
                    mrnValue = "MRN-CHARLIE",
                )
            runBlocking { deps.fhirRepository.savePatient(patient) }

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = PatientDetailRoute("pat-detail-nav")) {
                    patientDetailDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Brown", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Brown", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [patientVisitsDestination] in NavHost rendering [PatientDetailScreen].
     */
    @Test
    fun testPatientVisitsDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            val patient =
                createFhirPatient(
                    id = "pat-visits-nav",
                    firstName = "Diana",
                    lastName = "Prince",
                    dob = LocalDate(1982, 7, 24),
                    mrnValue = "MRN-DIANA",
                )
            runBlocking { deps.fhirRepository.savePatient(patient) }

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = PatientVisitsRoute("pat-visits-nav")) {
                    patientVisitsDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Prince", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Prince", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }
}
