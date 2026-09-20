/**
 * @file FhirAuditLoggerTest.kt
 * Tests verifying HIPAA/GDPR audit logging and contained resource extraction.
 */

package io.healthplatform.chartcam.audit

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.AuditEvent
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.fhir.getContainedResource
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests verifying AuditEvent persistence and DomainResource contained children resolution.
 */
class FhirAuditLoggerTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: FhirRepository

    /**
     * Sets up in-memory database and repository.
     */
    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repository = FhirRepository(db)
    }

    /**
     * Closes driver.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests logging access and export audit events into local repository.
     */
    @Test
    fun testLogAuditEvent() =
        runTest {
            val logResult =
                FhirAuditLogger.logAuditEvent(
                    repository = repository,
                    action = SecurityAuditAction.READ,
                    resourceType = "Patient",
                    resourceId = "pat-audit-1",
                    practitionerId = "prac-audit-1",
                    outcome = SecurityAuditOutcome.SUCCESS,
                    outcomeDesc = "Viewing patient demographics",
                )

            assertTrue(logResult.isSuccess)
            val event = logResult.getOrThrow()
            assertEquals(AuditEvent.AuditEventAction.R, event.action?.value)
            assertEquals(AuditEvent.AuditEventOutcome._0, event.outcome?.value)
            assertEquals("Viewing patient demographics", event.outcomeDesc?.value)

            val fetched = repository.getResource("AuditEvent", event.id ?: "")
            assertNotNull(fetched)
            assertTrue(fetched is AuditEvent)

            // Test all other actions and outcomes
            for (act in SecurityAuditAction.values()) {
                val res =
                    FhirAuditLogger.logAuditEvent(
                        repository = repository,
                        action = act,
                        resourceType = "Patient",
                        resourceId = "p-1",
                        practitionerId = "prac-1",
                        outcome = SecurityAuditOutcome.MINOR_FAILURE,
                    )
                assertTrue(res.isSuccess)
            }

            for (out in listOf(SecurityAuditOutcome.SERIOUS_FAILURE, SecurityAuditOutcome.MAJOR_FAILURE)) {
                val res =
                    FhirAuditLogger.logAuditEvent(
                        repository = repository,
                        action = SecurityAuditAction.READ,
                        resourceType = "Patient",
                        resourceId = "p-1",
                        practitionerId = null,
                        outcome = out,
                    )
                assertTrue(res.isSuccess)
            }

            val allDefaultsRes =
                FhirAuditLogger.logAuditEvent(
                    repository = repository,
                    action = SecurityAuditAction.READ,
                    resourceType = "Patient",
                    resourceId = "p-defaults",
                )
            assertTrue(allDefaultsRes.isSuccess)

            val closedDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(closedDriver)
            val closedRepo = FhirRepository(ChartCamDatabase(closedDriver))
            closedDriver.close()
            val failRes =
                FhirAuditLogger.logAuditEvent(
                    repository = closedRepo,
                    action = SecurityAuditAction.READ,
                    resourceType = "Patient",
                    resourceId = "p-1",
                    outcome = SecurityAuditOutcome.SUCCESS,
                )
            assertTrue(failRes.isFailure)
        }

    /**
     * Tests contained resource extraction by hash reference and clean ID.
     */
    @Test
    fun testContainedResourceExtraction() {
        val childPractitioner = Practitioner(id = "inline-prac-1")
        val childObservation =
            Observation(
                id = "inline-obs-1",
                status = Enumeration(value = Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
            )
        val patient =
            Patient(
                id = "parent-pat-1",
                contained = listOf(childPractitioner, childObservation),
            )

        val resolvedHash = patient.getContainedResource<Practitioner>("#inline-prac-1")
        assertTrue(resolvedHash.isSuccess)
        val prac = resolvedHash.getOrThrow()
        assertNotNull(prac)
        assertEquals("inline-prac-1", prac.id)

        val resolvedClean = patient.getContainedResource<Observation>("inline-obs-1")
        assertTrue(resolvedClean.isSuccess)
        val obs = resolvedClean.getOrThrow()
        assertNotNull(obs)
        assertEquals("inline-obs-1", obs.id)

        val nonExistent = patient.getContainedResource<Practitioner>("#missing-child")
        assertTrue(nonExistent.isSuccess)
        assertNull(nonExistent.getOrThrow())
    }
}
