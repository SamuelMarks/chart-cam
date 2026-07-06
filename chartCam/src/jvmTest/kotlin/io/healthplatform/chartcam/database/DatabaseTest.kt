package io.healthplatform.chartcam.database

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseTest {
    private lateinit var driverFactory: DatabaseDriverFactory
    private val dbFile = File("chartcam_desktop.db")

    @Before
    fun setup() {
        if (dbFile.exists()) {
            dbFile.delete()
        }
        driverFactory = DatabaseDriverFactory()
    }

    @After
    fun tearDown() {
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun testDatabaseCreationAndQuery() =
        runTest {
            // Create driver (should create DB file and schema)
            val driver = driverFactory.createDriver()
            assertNotNull(driver)
            assertTrue(dbFile.exists())

            // Initialize Database
            val database = ChartCamDatabase(driver)
            val queries = database.chartCamQueries

            // Insert a practitioner
            queries.insertPractitioner(
                id = "prac_1",
                family = "Smith",
                given = "John",
                active = true,
                serializedResource = "{}",
            )

            // Retrieve the practitioner
            val practitioners = queries.getAllPractitioners().executeAsList()
            assertEquals(1, practitioners.size)

            val p = practitioners.first()
            assertEquals("prac_1", p.id)
            assertEquals("Smith", p.family)
            assertEquals("John", p.given)
            assertEquals(true, p.active)
            assertEquals("{}", p.serializedResource)

            // Insert a patient
            queries.insertPatient(
                id = "pat_1",
                family = "Doe",
                given = "Jane",
                birthDate = "1990-01-01",
                mrn = "12345",
                gender = "female",
                managingOrganization = null,
                serializedResource = "{}",
            )

            val patients = queries.getAllPatients().executeAsList()
            assertEquals(1, patients.size)
            assertEquals("pat_1", patients.first().id)

            driver.close()
        }
}
