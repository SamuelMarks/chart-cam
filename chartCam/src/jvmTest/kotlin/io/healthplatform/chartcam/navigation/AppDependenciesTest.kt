/**
 * @file AppDependenciesTest.kt
 * Unit tests verifying AppDependencies initialization, property access, and copy semantics.
 */

package io.healthplatform.chartcam.navigation

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class DummySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

private class DummyFileStorage : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = "/dummy/$fileName"

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

/**
 * Tests evaluating [AppDependencies] construction and default property values.
 */
class AppDependenciesTest {
    /**
     * Verifies default null values for optional properties and field integrity.
     */
    @Test
    fun testAppDependenciesConstruction() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val database = ChartCamDatabase(driver)

        val storage = DummySecureStorage()
        val authRepo = AuthRepository(storage)
        val fhirRepo = FhirRepository(driver)
        val qRepo = QuestionnaireRepository(fhirRepo)
        val files = DummyFileStorage()
        val eiService = ExportImportService(database, files)
        val photoMgr = PhotoSessionManager()

        val deps =
            AppDependencies(
                authRepository = authRepo,
                fhirRepository = fhirRepo,
                questionnaireRepository = qRepo,
                exportImportService = eiService,
                photoSessionManager = photoMgr,
            )

        assertEquals(authRepo, deps.authRepository)
        assertEquals(fhirRepo, deps.fhirRepository)
        assertEquals(qRepo, deps.questionnaireRepository)
        assertEquals(eiService, deps.exportImportService)
        assertEquals(photoMgr, deps.photoSessionManager)
        assertNull(deps.fileStorage)
        assertNull(deps.biometricSecurityManager)

        val copy = deps.copy(fileStorage = files, biometricSecurityManager = BiometricSecurityManager(storage))
        assertEquals(files, copy.fileStorage)
        assertNotNull(copy.biometricSecurityManager)
        assertEquals(copy, copy.copy())
        assertEquals(copy.hashCode(), copy.copy().hashCode())
        assertNotNull(copy.toString())
    }
}
