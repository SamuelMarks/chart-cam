/**
 * @file FhirRepositoryJvmTest.kt
 * Contains declarations for FhirRepositoryJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test class for FhirRepository on JVM.
 */
class FhirRepositoryJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Sets up the test environment.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Tears down the test environment.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests practitioner CRUD operations.
     */
    @Test
    fun testPractitionerCrud() =
        runTest {
            val prac =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac_1"
                    }.build()

            repository.savePractitioner(prac)

            val fetched = repository.getPractitioner("prac_1")
            assertNotNull(fetched)
            assertEquals("prac_1", fetched.id)

            repository.deletePractitioner("prac_1")
            assertNull(repository.getPractitioner("prac_1"))
        }

    /**
     * Tests patient CRUD operations.
     */
    @Test
    fun testPatientCrud() =
        runTest {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "pat_1"
                    }.build()

            repository.savePatient(patient)

            val fetched = repository.getPatient("pat_1")
            assertNotNull(fetched)
            assertEquals("pat_1", fetched.id)

            val allPatients = repository.getAllPatients()
            assertEquals(1, allPatients.size)

            repository.deletePatient("pat_1")
            assertNull(repository.getPatient("pat_1"))
        }

    /**
     * Tests encounter CRUD operations.
     */
    @Test
    fun testEncounterCrud() =
        runTest {
            val encounter =
                Encounter
                    .Builder(
                        status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
                        `class` =
                            com.google.fhir.model.r4.Coding
                                .Builder(),
                    ).apply {
                        id = "enc_1"
                    }.build()

            repository.saveEncounter(encounter)

            val fetched = repository.getEncounter("enc_1")
            assertNotNull(fetched)
            assertEquals("enc_1", fetched.id)

            repository.updateEncounterStatus("enc_1", "finished", "all good")
            val updated = repository.getEncounter("enc_1")
            assertEquals(Encounter.EncounterStatus.Finished, updated?.status?.value)
            assertNotNull(updated?.text?.div?.value)

            repository.deleteEncounter("enc_1")
            assertNull(repository.getEncounter("enc_1"))
        }

    /**
     * Tests database failures are handled or thrown.
     */
    @Test
    fun testDatabaseFailuresHandledOrThrown() =
        runTest {
            val patient = Patient.Builder().apply { id = "pat_fail" }.build()
            driver.close() // Close DB to simulate failure

            var exceptionThrown = false
            try {
                repository.savePatient(patient)
            } catch (e: Exception) {
                exceptionThrown = true
            }
            kotlin.test.assertTrue(exceptionThrown, "Expected an exception when saving to a closed database")

            exceptionThrown = false
            try {
                repository.getPatient("pat_fail")
            } catch (e: Exception) {
                exceptionThrown = true
            }
            kotlin.test.assertTrue(exceptionThrown, "Expected an exception when reading from a closed database")
        }
}
