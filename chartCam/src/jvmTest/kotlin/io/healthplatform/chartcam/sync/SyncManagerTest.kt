/**
 * Contains unit tests for the [SyncManager] class, verifying patient history fetching logic.
 */
package io.healthplatform.chartcam.sync

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.String
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.FhirRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates synchronization logic, specifically focusing on fetching FHIR patient history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {
    /**
     * The in-memory SQLite driver used for testing database interactions.
     */
    private lateinit var driver: JdbcSqliteDriver

    /**
     * The repository responsible for persisting FHIR data.
     */
    private lateinit var fhirRepository: FhirRepository

    /**
     * A mock implementation of [FileStorage] used to avoid actual file system I/O.
     */
    private val mockFileStorage =
        object : FileStorage {
            /**
             * Simulates saving an image.
             *
             * @param fileName The name of the file.
             * @param bytes The image byte data.
             * @return A mock string path representing the saved file.
             */
            override fun saveImage(
                fileName: String,
                bytes: ByteArray,
            ): String = "mock_path.jpg"

            /**
             * Simulates reading an image.
             *
             * @param path The path to read from.
             * @return A dummy [ByteArray].
             */
            override fun readImage(path: String): ByteArray = ByteArray(10)

            /**
             * Simulates clearing the file cache.
             */
            override fun clearCache() {}
        }

    /**
     * Sets up the test database schema and initializes the [FhirRepository].
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        fhirRepository = FhirRepository(driver)
    }

    /**
     * Closes the test database connection after testing.
     */
    @After
    fun teardown() {
        driver.close()
    }

    /**
     * Tests that a successful network response containing a patient bundle is processed and saved properly.
     */
    @Test
    fun `fetchPatientHistory successfully fetches and saves incoming bundle`() =
        runTest {
            val jsonPayload =
                """
                {
                  "resourceType": "Bundle",
                  "type": "searchset",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "id": "pat-123",
                        "name": [ { "family": "Smith", "given": ["John"] } ]
                      }
                    }
                  ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = jsonPayload,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient = HttpClient(mockEngine)
            val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)

            val success = syncManager.fetchPatientHistory("pat-123")
            assertTrue(success)

            val savedPatient = fhirRepository.getPatient("pat-123")
            assertTrue(savedPatient != null)
        }

    /**
     * Tests that a network error correctly results in a failure status.
     */
    @Test
    fun `fetchPatientHistory returns false on server error`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                    )
                }

            val httpClient = HttpClient(mockEngine)
            val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)

            val success = syncManager.fetchPatientHistory("pat-123")
            assertFalse(success)
        }

    /**
     * Tests that syncEncounter correctly uploads a bundle and handles success.
     */
    @Test
    fun `syncEncounter uploads encounter and associated data successfully`() =
        runTest {
            // Setup mock data
            val patientId = "pat-sync-1"
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = patientId
                    }.build()

            val encounterId = "enc-sync-1"
            val encounter =
                Encounter
                    .Builder()
                    .apply {
                        id = encounterId
                        subject =
                            Reference
                                .Builder()
                                .apply {
                                    value = String.Builder().setValue("Patient/$patientId").build()
                                }.build()
                    }.build()

            fhirRepository.savePatient(patient)
            fhirRepository.saveEncounter(encounter)

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient = HttpClient(mockEngine)
            val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)

            val success = syncManager.syncEncounter(encounterId)
            assertTrue(success)
        }

    /**
     * Tests that syncEncounter fails gracefully if encounter doesn't exist.
     */
    @Test
    fun `syncEncounter fails if encounter not found`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respondOk()
                }

            val httpClient = HttpClient(mockEngine)
            val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)

            val success = syncManager.syncEncounter("missing-enc")
            assertFalse(success)
        }
}
