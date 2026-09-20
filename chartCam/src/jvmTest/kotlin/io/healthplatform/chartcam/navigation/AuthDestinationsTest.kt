/**
 * @file AuthDestinationsTest.kt
 * Unit and compose tests for AuthDestinations navigation graph extensions.
 */

package io.healthplatform.chartcam.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.BiometricSecurityManager
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.TAG_DEMO_BUTTON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * In-memory [SecureStorage] implementation for testing.
 */
private class TestSecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

/**
 * In-memory [FileStorage] stub for testing.
 */
private class TestFileStorage : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = "/test/$fileName"

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

/**
 * Test suite validating [loginDestination] navigation registration.
 */
class AuthDestinationsTest {
    /**
     * Verifies that loginDestination renders the LoginScreen and navigates on login success.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLoginDestinationRendersAndNavigates() =
        runComposeUiTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)

            val storage = TestSecureStorage()
            val auth = AuthRepository(storage)
            val frepo = FhirRepository(driver)
            val prepo = QuestionnaireRepository(frepo)
            val files = TestFileStorage()
            val expservice = ExportImportService(database, files)
            val photos = PhotoSessionManager()
            val bio = BiometricSecurityManager(storage)

            val deps =
                AppDependencies(
                    authRepository = auth,
                    fhirRepository = frepo,
                    questionnaireRepository = prepo,
                    exportImportService = expservice,
                    photoSessionManager = photos,
                    fileStorage = files,
                    biometricSecurityManager = bio,
                )

            var navHostController: NavHostController? = null

            setContent {
                val navController = rememberNavController()
                navHostController = navController
                NavHost(navController = navController, startDestination = Routes.LOGIN) {
                    loginDestination(
                        navController = navController,
                        deps = deps,
                        currentLang = "en",
                    )
                    composable(Routes.PATIENT_LIST) {
                        Text("PatientListDestination")
                    }
                }
            }
            waitForIdle()
            assertNotNull(deps)

            onNodeWithTag(TAG_DEMO_BUTTON).performClick()
            waitForIdle()

            assertEquals(Routes.PATIENT_LIST, navHostController?.currentDestination?.route)
        }
}
