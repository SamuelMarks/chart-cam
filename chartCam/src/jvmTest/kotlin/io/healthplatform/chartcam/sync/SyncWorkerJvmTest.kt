package io.healthplatform.chartcam.sync

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncWorkerJvmTest {
    private lateinit var repository: FhirRepository
    private val fhirJson =
        com.google.fhir.model.r4
            .FhirR4Json()

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        repository = FhirRepository(driver)
    }

    @Test
    fun testSuccessfulSyncWithDeltaUpdates() =
        runTest {
            // Setup mock HTTP Client
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/fhir/Patient/123" -> {
                            // Upload success
                            respond(
                                content = "",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ETag, "W/\"2\""),
                            )
                        }
                        "/fhir/Patient/\$everything" -> {
                            // Delta download
                            val bundleJson =
                                """
                                {
                                    "resourceType": "Bundle",
                                    "type": "searchset",
                                    "entry": [
                                        {
                                            "resource": {
                                                "resourceType": "Patient",
                                                "id": "456",
                                                "meta": { "versionId": "1" }
                                            }
                                        }
                                    ]
                                }
                                """.trimIndent()
                            respond(
                                content = bundleJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine)
            val syncWorker = SyncWorker(repository, client)

            // Queue local change
            val patient = createFhirPatient("123", "John", "Doe", kotlinx.datetime.LocalDate(1990, 1, 1), "mrn1", "male")
            repository.saveResource("Patient", "123", patient, isLocalChange = true)

            assertEquals(1, repository.getPendingLocalChangesCount())

            // Execute sync
            syncWorker.sync()

            // Verify successful sync
            assertEquals(SyncState.Idle, syncWorker.syncState.value) // Returns to Idle
            assertEquals(0, repository.getPendingLocalChangesCount(), "Pending changes should be cleared")

            // Verify remote patient 456 was saved locally
            val downloadedPatient = repository.getPatient("456")
            assertTrue(downloadedPatient != null)
        }

    @Test
    fun testOfflineQueuingAndErrorState() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respondError(HttpStatusCode.ServiceUnavailable)
                }
            val client = HttpClient(mockEngine)
            val syncWorker = SyncWorker(repository, client)

            // Queue local change
            val patient = createFhirPatient("123", "John", "Doe", kotlinx.datetime.LocalDate(1990, 1, 1), "mrn1", "male")
            repository.saveResource("Patient", "123", patient, isLocalChange = true)

            // Execute sync
            syncWorker.sync()

            // Should transition to Offline with 1 queued item
            val state = syncWorker.syncState.value
            assertTrue(state is SyncState.Offline)
            assertEquals(1, (state as SyncState.Offline).queuedChanges)

            // Queue change remains
            assertEquals(1, repository.getPendingLocalChangesCount())
        }

    @Test
    fun testConflictResolutionETagMismatch() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    if (request.url.encodedPath == "/fhir/Patient/123" && request.method.value == "PUT") {
                        // Conflict
                        respondError(HttpStatusCode.PreconditionFailed) // 412
                    } else if (request.url.encodedPath == "/fhir/Patient/\$everything") {
                        // Simulate downloading the newer version
                        val bundleJson =
                            """
                            {
                                "resourceType": "Bundle",
                                "type": "searchset",
                                "entry": []
                            }
                            """.trimIndent()
                        respond(bundleJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine)
            val syncWorker = SyncWorker(repository, client)

            // Queue local change
            val patient = createFhirPatient("123", "John", "Doe", kotlinx.datetime.LocalDate(1990, 1, 1), "mrn1", "male")
            repository.saveResource("Patient", "123", patient, isLocalChange = true)

            syncWorker.sync()

            // Change should still be in the local changes queue or handled
            assertEquals(1, repository.getPendingLocalChangesCount())
        }
}
