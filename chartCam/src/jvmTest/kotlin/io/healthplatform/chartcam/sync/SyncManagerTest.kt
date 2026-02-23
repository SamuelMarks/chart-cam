package io.healthplatform.chartcam.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
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
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var fhirRepository: FhirRepository
    
    private val mockFileStorage = object : FileStorage {
        override fun saveImage(fileName: String, bytes: ByteArray): String = "mock_path.jpg"
        override fun readImage(path: String): ByteArray = ByteArray(10)
        override fun clearCache() {}
    }

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        fhirRepository = FhirRepository(driver)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun `fetchPatientHistory successfully fetches and saves incoming bundle`() = runTest {
        val jsonPayload = """
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
        
        val mockEngine = MockEngine { request ->
            respond(
                content = jsonPayload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine)
        val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)
        
        val success = syncManager.fetchPatientHistory("pat-123")
        assertTrue(success)
        
        val savedPatient = fhirRepository.getPatient("pat-123")
        assertTrue(savedPatient != null)
    }

    @Test
    fun `fetchPatientHistory returns false on server error`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        
        val httpClient = HttpClient(mockEngine)
        val syncManager = SyncManager(fhirRepository, httpClient, mockFileStorage)
        
        val success = syncManager.fetchPatientHistory("pat-123")
        assertFalse(success)
    }
}
