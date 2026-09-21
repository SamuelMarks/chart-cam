/**
 * @file FhirRepositoryResultTest.kt
 * Unit tests verifying Result-wrapped mutations and query operations in [FhirRepository].
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates that [FhirRepository] operations returning [Result] execute cleanly and encapsulate errors.
 */
class FhirRepositoryResultTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: ChartCamDatabase
    private lateinit var repository: FhirRepository

    /**
     * Initializes in-memory database and repository.
     */
    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        database = ChartCamDatabase(driver)
        repository = FhirRepository(database)
    }

    /**
     * Closes driver after tests.
     */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /**
     * Verifies that saving and deleting patients via Result-returning APIs succeed.
     */
    @Test
    fun testPatientResultMutationsAndQueries() =
        runTest {
            val patient =
                createFhirPatient(
                    id = "p-res-1",
                    firstName = "Alice",
                    lastName = "Smith",
                    dob = LocalDate(1985, 5, 20),
                    mrnValue = "MRN-RES-1",
                )

            val saveResult = repository.savePatient(patient)
            assertTrue(saveResult.isSuccess, "Saving patient should return Result.success")

            val queryResult = repository.getPatientCatching("p-res-1")
            assertTrue(queryResult.isSuccess, "Querying patient should return Result.success")
            assertNotNull(queryResult.getOrNull())
            assertEquals("p-res-1", queryResult.getOrNull()?.id)

            val allPatientsResult = repository.getAllPatientsCatching(showAll = true)
            assertTrue(allPatientsResult.isSuccess)
            assertEquals(1, allPatientsResult.getOrNull()?.size)

            val searchPatientsResult = repository.searchPatientsCatching("Smith")
            assertTrue(searchPatientsResult.isSuccess)
            assertEquals(1, searchPatientsResult.getOrNull()?.size)

            val deleteResult = repository.deletePatient("p-res-1")
            assertTrue(deleteResult.isSuccess, "Deleting patient should return Result.success")

            val afterDelete = repository.getPatientCatching("p-res-1")
            assertTrue(afterDelete.isSuccess)
            assertEquals(null, afterDelete.getOrNull())

            // Test deletePatient failure percolation with failing repo
            val failingRepo =
                object : FhirRepository(database) {
                    override suspend fun getEncountersForPatientCatching(
                        patientId: String,
                    ): Result<List<dev.ohs.fhir.model.r4.Encounter>> =
                        Result.failure(IllegalStateException("Encounter lookup error"))
                }
            val failingDeleteRes = failingRepo.deletePatient("p-fail")
            assertTrue(failingDeleteRes.isFailure)
        }

    /**
     * Verifies encounter mutations returning Result.
     */
    @Test
    fun testEncounterResultMutations() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc-res-1",
                    patientId = "p-res-1",
                    practitionerId = "prac-1",
                    dateStr = "2026-09-14T10:00:00Z",
                )

            val saveResult = repository.saveEncounter(encounter)
            assertTrue(saveResult.isSuccess)

            val resourceResult = repository.getResourceCatching("Encounter", "enc-res-1")
            assertTrue(resourceResult.isSuccess)
            assertNotNull(resourceResult.getOrNull())

            val deleteResult = repository.deleteEncounter("enc-res-1")
            assertTrue(deleteResult.isSuccess)
        }
}
