/**
 * @file DummyAsyncJvmTest.kt
 * Contains JVM SQLDelight schema initialization and asynchronous query verification tests.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for async database schema initialization and baseline queries on the JVM platform.
 */
class DummyAsyncJvmTest {
    /**
     * Verifies that [ChartCamDatabase.Schema] initializes all core tables on an in-memory JDBC SQLite driver
     * and executes an asynchronous query returning an empty list before inserting and querying a resource.
     */
    @Test
    fun testDummyAsyncJvm() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val db = ChartCamDatabase(driver)
            assertNotNull(db)

            val initialPatients =
                db.chartCamQueries
                    .getAllResourcesByType("Patient")
                    .awaitAsList()
            assertTrue(initialPatients.isEmpty())

            db.chartCamQueries.insertResource(
                resourceId = "p-init-1",
                resourceType = "Patient",
                serializedResource = """{"resourceType":"Patient","id":"p-init-1"}""",
                lastUpdated = "2026-10-15T00:00:00Z",
            )
            val afterInsert =
                db.chartCamQueries
                    .getAllResourcesByType("Patient")
                    .awaitAsList()
            assertEquals(1, afterInsert.size)
            assertEquals("p-init-1", afterInsert.first().resourceId)
            driver.close()
        }
}
