/**
 * @file DemoDataSeederJvmTest.kt
 * Contains declarations for DemoDataSeederJvmTest.kt.
 *
 * JVM integration tests for [DemoDataSeeder] using an in-memory SQLite database.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Validates [DemoDataSeeder] operations against a live SQLite database.
 */
class DemoDataSeederJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Sets up in-memory SQLite database for testing.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Closes the database driver after testing.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Verifies that seeding inserts all synthetic clinical records and re-seeding is idempotent.
     */
    @Test
    fun testSeedDemoDataAndIdempotency() {
        runBlocking {
            DemoDataSeeder.seedDemoData(repository)

            // Assert patients exist
            val pediatricPatient = repository.getPatient(DemoDataSeeder.DEMO_PATIENT_PEDIATRIC_ID)
            assertNotNull(pediatricPatient)
            assertEquals(
                "Leo",
                pediatricPatient.name
                    .first()
                    .given
                    .first()
                    .value,
            )
            assertEquals(
                "Chen",
                pediatricPatient.name
                    .first()
                    .family
                    ?.value,
            )

            val adultPatient = repository.getPatient(DemoDataSeeder.DEMO_PATIENT_ADULT_ID)
            assertNotNull(adultPatient)
            assertEquals(
                "Sarah",
                adultPatient.name
                    .first()
                    .given
                    .first()
                    .value,
            )

            val geriatricPatient = repository.getPatient(DemoDataSeeder.DEMO_PATIENT_GERIATRIC_ID)
            assertNotNull(geriatricPatient)
            assertEquals(
                "Robert",
                geriatricPatient.name
                    .first()
                    .given
                    .first()
                    .value,
            )

            // Assert encounters exist
            val pediatricEncounter = repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_PEDIATRIC_ID)
            assertNotNull(pediatricEncounter)

            val adultEncounter = repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_ADULT_ID)
            assertNotNull(adultEncounter)

            val geriatricEncounter = repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_GERIATRIC_ID)
            assertNotNull(geriatricEncounter)

            // Assert QRs exist
            val pediatricQr = repository.getResource("QuestionnaireResponse", DemoDataSeeder.DEMO_QR_PEDIATRIC_ID)
            assertNotNull(pediatricQr)

            // Idempotent re-seed test
            DemoDataSeeder.seedDemoData(repository)
            val recheckedPatient = repository.getPatient(DemoDataSeeder.DEMO_PATIENT_PEDIATRIC_ID)
            assertNotNull(recheckedPatient)
        }
    }

    /**
     * Verifies that clearing demo data removes all synthetic records completely.
     */
    @Test
    fun testClearDemoData() {
        runBlocking {
            DemoDataSeeder.seedDemoData(repository)
            assertNotNull(repository.getPatient(DemoDataSeeder.DEMO_PATIENT_PEDIATRIC_ID))

            DemoDataSeeder.clearDemoData(repository)

            assertNull(repository.getPatient(DemoDataSeeder.DEMO_PATIENT_PEDIATRIC_ID))
            assertNull(repository.getPatient(DemoDataSeeder.DEMO_PATIENT_ADULT_ID))
            assertNull(repository.getPatient(DemoDataSeeder.DEMO_PATIENT_GERIATRIC_ID))

            assertNull(repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_PEDIATRIC_ID))
            assertNull(repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_ADULT_ID))
            assertNull(repository.getEncounter(DemoDataSeeder.DEMO_ENCOUNTER_GERIATRIC_ID))

            assertNull(repository.getResource("QuestionnaireResponse", DemoDataSeeder.DEMO_QR_PEDIATRIC_ID))
            assertNull(repository.getResource("QuestionnaireResponse", DemoDataSeeder.DEMO_QR_ADULT_ID))
            assertNull(repository.getResource("QuestionnaireResponse", DemoDataSeeder.DEMO_QR_GERIATRIC_ID))
        }
    }
}
