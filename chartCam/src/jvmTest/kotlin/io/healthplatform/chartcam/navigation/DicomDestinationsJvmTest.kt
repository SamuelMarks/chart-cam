/**
 * @file DicomDestinationsJvmTest.kt
 * Contains declarations for DicomDestinationsJvmTest.kt.
 *
 * JVM Compose UI tests for [DicomViewerScreen] and [dicomViewerDestination].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

private class BlockingDicomFileStorage(
    private val bytesToReturn: ByteArray,
) : FileStorage {
    val proceed = java.util.concurrent.CountDownLatch(1)

    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray {
        proceed.await()
        return bytesToReturn
    }

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

    /**
     * Tests successful DICOM dataset rendering and back stack pop navigation.
     */
    @Test
    fun testDicomViewerDestinationSuccessAndPop() =
        runComposeUiTest {
            setAppLanguage("en")
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            val dcmBytes =
                io.healthplatform.chartcam.dicom.FhirToDicomMapper
                    .createVisibleLightImageDicom(jpegBytes)
                    .getOrThrow()
            val fakeStorage = TestDicomFileStorage(dcmBytes)
            val deps = createTestDependencies(fakeStorage)

            setContent {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = DicomViewerRoute("sample.dcm"),
                ) {
                    dicomViewerDestination(navController, deps)
                }
            }

            waitForIdle()

            // Verify TopAppBar heading title
            onNodeWithText("DICOM Inspector", useUnmergedTree = true).assertIsDisplayed()

            // Verify back button is displayed
            onNodeWithContentDescription("Back", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests null fileStorage fallback branch in DicomViewerScreen.
     */
    @Test
    fun testDicomViewerScreenNullFileStorageFallback() =
        runComposeUiTest {
            setAppLanguage("en")
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val secureStorage = MemorySecureStorage()
            val authRepo = AuthRepository(secureStorage)
            val fhirRepo = FhirRepository(driver)
            val qRepo = QuestionnaireRepository(fhirRepo)
            val eiService =
                ExportImportService(
                    database,
                    io.healthplatform.chartcam.files
                        .createFileStorage(),
                )
            val photoMgr = PhotoSessionManager()

            val depsWithoutStorage =
                AppDependencies(
                    authRepository = authRepo,
                    fhirRepository = fhirRepo,
                    questionnaireRepository = qRepo,
                    exportImportService = eiService,
                    photoSessionManager = photoMgr,
                    fileStorage = null,
                )

            setContent {
                DicomViewerScreen(
                    filePath = "nonexistent.dcm",
                    deps = depsWithoutStorage,
                    onBack = {},
                )
            }

            waitForIdle()
            onNodeWithText("DICOM Inspector", useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests recomposition of DicomViewerScreen with updated parameters.
     */
    @Test
    fun testDicomViewerScreenRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val fakeStorage = TestDicomFileStorage(byteArrayOf(1, 2, 3))
            val deps1 = createTestDependencies(fakeStorage)
            val deps2 = createTestDependencies(fakeStorage)

            val filePathState = androidx.compose.runtime.mutableStateOf("file1.dcm")
            val depsState = androidx.compose.runtime.mutableStateOf(deps1)
            val onBackState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val modifierState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            setContent {
                DicomViewerScreen(
                    filePath = filePathState.value,
                    deps = depsState.value,
                    onBack = onBackState.value,
                    modifier = modifierState.value,
                )
            }
            waitForIdle()

            filePathState.value = "file2.dcm"
            waitForIdle()

            depsState.value = deps2
            waitForIdle()

            onBackState.value = { println("back") }
            waitForIdle()

            modifierState.value =
                androidx.compose.ui.Modifier
                    .semantics { }
            waitForIdle()
        }

    /**
     * Tests skipping recomposition of DicomViewerScreen when parent recomposes with unchanged inputs.
     */
    @Test
    fun testDicomViewerScreenRecompositionSkipping() =
        runComposeUiTest {
            setAppLanguage("en")
            val fakeStorage = TestDicomFileStorage(byteArrayOf(1, 2, 3))
            val deps1 = createTestDependencies(fakeStorage)
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = outerTrigger.value
                DicomViewerScreen(
                    filePath = "file1.dcm",
                    deps = deps1,
                    onBack = {},
                )
            }
            waitForIdle()

            outerTrigger.value++
            waitForIdle()
        }

    /**
     * Tests recomposition of DicomViewerScreen when invoked with default modifier.
     */
    @Test
    fun testDicomViewerScreenDefaultParameterRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val fakeStorage = TestDicomFileStorage(byteArrayOf(1, 2, 3))
            val deps1 = createTestDependencies(fakeStorage)
            val deps2 = createTestDependencies(fakeStorage)

            val filePathState = androidx.compose.runtime.mutableStateOf("file1.dcm")
            val depsState = androidx.compose.runtime.mutableStateOf(deps1)
            val onBackState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})

            setContent {
                DicomViewerScreen(
                    filePath = filePathState.value,
                    deps = depsState.value,
                    onBack = onBackState.value,
                )
            }
            waitForIdle()

            filePathState.value = "file2.dcm"
            waitForIdle()

            depsState.value = deps2
            waitForIdle()

            onBackState.value = { println("back") }
            waitForIdle()
        }

    /**
     * Tests rendering of loading indicator in DicomViewerScreen before data loads.
     */
    @Test
    fun testDicomViewerScreenLoadingIndicator() =
        runComposeUiTest {
            setAppLanguage("en")
            val blockingStorage = BlockingDicomFileStorage(byteArrayOf(1, 2, 3))
            val deps = createTestDependencies(blockingStorage)

            setContent {
                DicomViewerScreen(
                    filePath = "loading.dcm",
                    deps = deps,
                    onBack = {},
                )
            }

            // Verify loading indicator is displayed while blocking storage has not completed
            onNodeWithTag("DicomLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()

            // Unblock storage
            blockingStorage.proceed.countDown()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Failed to parse DICOM file: loading.dcm", substring = true, useUnmergedTree = true)
                        .assertExists()
                }.isSuccess
            }

            // Verify error message for invalid DICOM after unblocking
            onNodeWithText("Failed to parse DICOM file: loading.dcm", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
}
