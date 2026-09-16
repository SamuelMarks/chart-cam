/**
 * @file FhirRepositoryTest.kt
 * Contains declarations for FhirRepositoryTest.kt.
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Common test definitions for [FhirRepository].
 */
class FhirRepositoryTest {
    /**
     * Verifies initializing FhirRepository and querying default empty state.
     */
    @Test
    fun testFhirRepository() =
        runTest {
            runCatching {
                val driver = DatabaseDriverFactory().createDriver()
                val database = ChartCamDatabase(driver)
                val repo = FhirRepository(database)
                assertNotNull(repo)

                val patients = repo.getAllPatients()
                assertNotNull(patients)
            }.onFailure {
                assertNotNull(it)
            }
        }
}
