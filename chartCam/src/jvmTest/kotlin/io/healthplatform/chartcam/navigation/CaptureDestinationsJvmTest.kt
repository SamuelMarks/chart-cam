/**
 * @file CaptureDestinationsJvmTest.kt
 * Contains declarations for CaptureDestinationsJvmTest.kt.
 *
 * JVM Compose UI tests for [captureDestination], [captureForPatientDestination], and [triageDestination].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.setAppLanguage
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.ohs.fhir.model.r4.String as FhirString

private class CaptureMemorySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

private class DummyTestFileStorage : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

private fun createTestDependencies(
    fileStorage: FileStorage? = DummyTestFileStorage(),
    questionnaireRepo: QuestionnaireRepository? = null,
): AppDependencies {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ChartCamDatabase.Schema.synchronous().create(driver)
    val database = ChartCamDatabase(driver)
    val secureStorage = CaptureMemorySecureStorage()
    val authRepo = AuthRepository(secureStorage)
    val fhirRepo = FhirRepository(driver)
    val qRepo = questionnaireRepo ?: QuestionnaireRepository(fhirRepo)
    val eiService = ExportImportService(database, fileStorage ?: DummyTestFileStorage())
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
 * JVM test suite verifying navigation graph bindings for capture and triage destinations.
 */
@OptIn(ExperimentalTestApi::class)
class CaptureDestinationsJvmTest {
    /**
     * Tests captureDestination navigating to patient list when finished with empty photos.
     */
    @Test
    fun testCaptureDestinationFinishedEmpty() =
        runComposeUiTest {
            setAppLanguage("en")
            val mockCam = Mockito.mock(com.github.sarxos.webcam.Webcam::class.java)
            io.healthplatform.chartcam.camera.JvmPermissionManager.defaultWebcamSupplier = { listOf(mockCam) }
            val mockRepo = Mockito.mock(QuestionnaireRepository::class.java)
            Mockito.`when`(mockRepo.getQuestionnaire("std-form")).thenReturn(
                Questionnaire(
                    id = "std-form",
                    status = Enumeration(value = PublicationStatus.Active),
                    item = emptyList(),
                ),
            )
            val deps = createTestDependencies(questionnaireRepo = mockRepo)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.CAPTURE,
                ) {
                    captureDestination(navController, deps, "en")
                    composable(Routes.PATIENT_LIST) {
                        androidx.compose.material3.Text("Destination: Patient List")
                    }
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Destination: Patient List", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Destination: Patient List", useUnmergedTree = true).assertIsDisplayed()
            io.healthplatform.chartcam.camera.JvmPermissionManager.defaultWebcamSupplier = null
        }

    /**
     * Tests captureDestination cancel button navigation back to patient list.
     */
    @Test
    fun testCaptureDestinationCancel() =
        runComposeUiTest {
            setAppLanguage("en")
            val photoItem =
                Questionnaire.Item(
                    linkId = FhirString(value = "photo1"),
                    text = FhirString(value = "Facial Photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                )
            val mockRepo = Mockito.mock(QuestionnaireRepository::class.java)
            Mockito.`when`(mockRepo.getQuestionnaire("std-form")).thenReturn(
                Questionnaire(
                    id = "std-form",
                    status = Enumeration(value = PublicationStatus.Active),
                    item = listOf(photoItem),
                ),
            )
            val deps = createTestDependencies(questionnaireRepo = mockRepo)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.CAPTURE,
                ) {
                    captureDestination(navController, deps, "en")
                    composable(Routes.PATIENT_LIST) {
                        androidx.compose.material3.Text("Destination: Patient List")
                    }
                }
            }

            waitForIdle()
            onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Destination: Patient List", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Destination: Patient List", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests captureForPatientDestination with null questionnaireId and cancel callback.
     */
    @Test
    fun testCaptureForPatientDestinationCancelWithNullQuestionnaireId() =
        runComposeUiTest {
            setAppLanguage("en")
            val photoItem =
                Questionnaire.Item(
                    linkId = FhirString(value = "step1"),
                    text = FhirString(value = "Wound Photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                )
            val mockRepo = Mockito.mock(QuestionnaireRepository::class.java)
            Mockito.`when`(mockRepo.getQuestionnaire("std-form")).thenReturn(
                Questionnaire(
                    id = "std-form",
                    status = Enumeration(value = PublicationStatus.Active),
                    item = listOf(photoItem),
                ),
            )
            val deps = createTestDependencies(questionnaireRepo = mockRepo)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "root",
                ) {
                    composable("root") {
                        androidx.compose.material3.Text("Root Screen")
                    }
                    captureForPatientDestination(navController, deps, "en")
                }
                navController.navigate(
                    CaptureForPatientRoute(
                        patientId = "pat-1",
                        questionnaireId = null,
                        linkId = null,
                    ),
                )
            }

            waitForIdle()
            onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Root Screen", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Root Screen", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests captureForPatientDestination with explicit questionnaireId and empty finish callback.
     */
    @Test
    fun testCaptureForPatientDestinationWithCustomQuestionnaireId() =
        runComposeUiTest {
            setAppLanguage("en")
            val mockCam = Mockito.mock(com.github.sarxos.webcam.Webcam::class.java)
            io.healthplatform.chartcam.camera.JvmPermissionManager.defaultWebcamSupplier = { listOf(mockCam) }
            val mockRepo = Mockito.mock(QuestionnaireRepository::class.java)
            Mockito.`when`(mockRepo.getQuestionnaire("custom-q")).thenReturn(
                Questionnaire(
                    id = "custom-q",
                    status = Enumeration(value = PublicationStatus.Active),
                    item = emptyList(),
                ),
            )
            val deps = createTestDependencies(questionnaireRepo = mockRepo)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "root",
                ) {
                    composable("root") {
                        androidx.compose.material3.Text("Root Screen")
                    }
                    captureForPatientDestination(navController, deps, "en")
                }
                navController.navigate(
                    CaptureForPatientRoute(
                        patientId = "pat-1",
                        questionnaireId = "custom-q",
                        linkId = "link1",
                    ),
                )
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Root Screen", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Root Screen", useUnmergedTree = true).assertIsDisplayed()
            io.healthplatform.chartcam.camera.JvmPermissionManager.defaultWebcamSupplier = null
        }

    /**
     * Tests triageDestination rendering and onBack callback.
     */
    @Test
    fun testTriageDestinationWithFileStorageAndBack() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies(fileStorage = DummyTestFileStorage())

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "root",
                ) {
                    composable("root") {
                        androidx.compose.material3.Text("Root Screen")
                    }
                    triageDestination(navController, deps, "en")
                }
                navController.navigate(TriageRoute)
            }

            waitForIdle()
            onNodeWithText("Triage: Select Patient", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Root Screen", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Root Screen", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests triageDestination with null fileStorage fallback branch.
     */
    @Test
    fun testTriageDestinationNullFileStorageFallback() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies(fileStorage = null)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = TriageRoute,
                ) {
                    triageDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            onNodeWithText("Triage: Select Patient", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests triageDestination onProceedToEncounter navigation.
     */
    @Test
    fun testTriageDestinationProceedToEncounter() =
        runComposeUiTest {
            setAppLanguage("en")
            val patient =
                io.healthplatform.chartcam.models.createFhirPatient(
                    id = "p-100",
                    firstName = "John",
                    lastName = "Doe",
                    dob = kotlinx.datetime.LocalDate(1990, 1, 1),
                    mrnValue = "MRN-100",
                )
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val fhirRepo = FhirRepository(driver)
            kotlinx.coroutines.runBlocking { fhirRepo.savePatient(patient) }

            val deps = createTestDependencies(fileStorage = DummyTestFileStorage()).copy(fhirRepository = fhirRepo)
            deps.photoSessionManager.setPhotos(mapOf("photo1" to "path1.jpg"))

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = TriageRoute,
                ) {
                    triageDestination(navController, deps, "en")
                    composable<NewVisitRoute> {
                        androidx.compose.material3.Text("New Visit Destination")
                    }
                }
            }

            waitForIdle()
            onNode(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            ).performTextInput("Doe")
            waitForIdle()

            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Doe, John", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onAllNodesWithText("Doe, John", useUnmergedTree = true)[0].performClick()
            waitForIdle()

            onAllNodesWithText("Doe, John", useUnmergedTree = true)[0].performClick()
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("New Visit Destination", useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("New Visit Destination", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies all branches of [handleCaptureFinished] for both standard and patient flows.
     */
    @Test
    fun testHandleCaptureFinishedCombinations() =
        runComposeUiTest {
            val photoMgr = PhotoSessionManager()

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "start",
                ) {
                    composable("start") { androidx.compose.material3.Text("Start") }
                    composable(Routes.PATIENT_LIST) { androidx.compose.material3.Text("PatientList") }
                    composable<TriageRoute> { androidx.compose.material3.Text("Triage") }
                }

                // 1. Standard flow - empty map
                handleCaptureFinished(
                    outputPathsMap = emptyMap(),
                    navController = navController,
                    photoSessionManager = photoMgr,
                    isPatientFlow = false,
                )
            }
            waitForIdle()
            onNodeWithText("PatientList", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies standard flow with non-empty map navigating to TriageRoute.
     */
    @Test
    fun testHandleCaptureFinishedStandardWithPhotos() =
        runComposeUiTest {
            val photoMgr = PhotoSessionManager()

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "start",
                ) {
                    composable("start") { androidx.compose.material3.Text("Start") }
                    composable<TriageRoute> { androidx.compose.material3.Text("Triage") }
                }

                handleCaptureFinished(
                    outputPathsMap = mapOf("step1" to "file1.png"),
                    navController = navController,
                    photoSessionManager = photoMgr,
                    isPatientFlow = false,
                )
            }
            waitForIdle()
            onNodeWithText("Triage", useUnmergedTree = true).assertIsDisplayed()
            assertEquals("file1.png", photoMgr.get()["step1"])
        }

    /**
     * Verifies patient flow with empty and non-empty maps popping back stack.
     */
    @Test
    fun testHandleCaptureFinishedPatientFlow() =
        runComposeUiTest {
            val photoMgr = PhotoSessionManager()

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "root",
                ) {
                    composable("root") { androidx.compose.material3.Text("Root") }
                    composable("sub") { androidx.compose.material3.Text("Sub") }
                }

                navController.navigate("sub")
                // Patient flow with non-empty map
                handleCaptureFinished(
                    outputPathsMap = mapOf("step2" to "file2.png"),
                    navController = navController,
                    photoSessionManager = photoMgr,
                    isPatientFlow = true,
                )
            }
            waitForIdle()
            onNodeWithText("Root", useUnmergedTree = true).assertIsDisplayed()
            assertEquals("file2.png", photoMgr.get()["step2"])
        }

    /**
     * Verifies patient flow with empty map without setting photos.
     */
    @Test
    fun testHandleCaptureFinishedPatientFlowEmpty() =
        runComposeUiTest {
            val photoMgr = PhotoSessionManager()

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "root",
                ) {
                    composable("root") { androidx.compose.material3.Text("Root") }
                    composable("sub") { androidx.compose.material3.Text("Sub") }
                }

                navController.navigate("sub")
                // Patient flow with empty map
                handleCaptureFinished(
                    outputPathsMap = emptyMap(),
                    navController = navController,
                    photoSessionManager = photoMgr,
                    isPatientFlow = true,
                )
            }
            waitForIdle()
            onNodeWithText("Root", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies default isPatientFlow parameter in [handleCaptureFinished].
     */
    @Test
    fun testHandleCaptureFinishedDefaultParameter() =
        runComposeUiTest {
            val photoMgr = PhotoSessionManager()

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "start",
                ) {
                    composable("start") { androidx.compose.material3.Text("Start") }
                    composable(Routes.PATIENT_LIST) { androidx.compose.material3.Text("PatientList") }
                }

                handleCaptureFinished(
                    outputPathsMap = emptyMap(),
                    navController = navController,
                    photoSessionManager = photoMgr,
                )
            }
            waitForIdle()
            onNodeWithText("PatientList", useUnmergedTree = true).assertIsDisplayed()
        }
}
