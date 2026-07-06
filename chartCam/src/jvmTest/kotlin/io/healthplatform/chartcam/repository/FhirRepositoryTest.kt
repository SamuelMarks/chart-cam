package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.*
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FhirRepositoryTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    @After
    fun tearDown() {
        driver.close()
    }

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

    @Test
    fun testEncounterCrud() =
        runTest {
            val encounter =
                Encounter
                    .Builder(
                        status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
                        `class` = Coding.Builder(),
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
}
