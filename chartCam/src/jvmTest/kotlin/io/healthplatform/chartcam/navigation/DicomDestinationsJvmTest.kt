/**
 * @file DicomDestinationsJvmTest.kt
 * Contains declarations for DicomDestinationsJvmTest.kt.
 *
 * JVM Compose UI tests for [DicomViewerScreen] and [dicomViewerDestination].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test
import kotlin.test.assertTrue

private class MemorySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

private class TestDicomFileStorage(
    private val bytesToReturn: ByteArray,
) : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray = bytesToReturn

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

private fun createTestDependencies(fileStorage: FileStorage): AppDependencies {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ChartCamDatabase.Schema.synchronous().create(driver)
    val database = ChartCamDatabase(driver)
    val secureStorage = MemorySecureStorage()
    val authRepo = AuthRepository(secureStorage)
    val fhirRepo = FhirRepository(driver)
    val qRepo = QuestionnaireRepository(fhirRepo)
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
 * JVM test suite for DicomDestinations.
 */
@OptIn(ExperimentalTestApi::class)
class DicomDestinationsJvmTest {
    /**
     * Tests error rendering when DICOM file decoding fails in destination navigation.
     */
    @Test
    fun testDicomViewerDestinationErrorRendering() =
        runComposeUiTest {
            setAppLanguage("en")
            val fakeStorage = TestDicomFileStorage(byteArrayOf(1, 2, 3))
            val deps = createTestDependencies(fakeStorage)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = DicomViewerRoute("corrupted.dcm"),
                ) {
                    dicomViewerDestination(navController, deps)
                }
            }

            waitForIdle()

            // Verify TopAppBar heading title
            onNodeWithText("DICOM Inspector", useUnmergedTree = true).assertIsDisplayed()

            // Verify error message for corrupt file
            onNodeWithText("Failed to parse DICOM file: corrupted.dcm", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()

            // Verify back button is accessible
            onNodeWithContentDescription("Back", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests back button navigation pop callback in DicomViewerScreen.
     */
    @Test
    fun testDicomViewerScreenBackCallback() =
        runComposeUiTest {
            setAppLanguage("en")
            var backClicked = false
            val fakeStorage = TestDicomFileStorage(ByteArray(0))
            val deps = createTestDependencies(fakeStorage)

            setContent {
                DicomViewerScreen(
                    filePath = "empty.dcm",
                    deps = deps,
                    onBack = { backClicked = true },
                )
            }

            waitForIdle()
            onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
            waitForIdle()
            assertTrue(backClicked)
        }
}
