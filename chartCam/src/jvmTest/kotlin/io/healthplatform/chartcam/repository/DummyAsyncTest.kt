/**
 * Sandbox tests specifically examining database interactions through driver configurations.
 *
 * Specifically meant to test basic compilation or sanity metrics on generated database drivers
 * against custom mapping parameters or specific async extensions.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlin.test.Test

/**
 * Validates baseline setup for an async connected [ChartCamDatabase].
 *
 * Sets up basic query logic directly testing driver-level integrations without caching.
 */
class DummyAsyncTest {
    /**
     * Performs a direct database extraction using raw query driver interfaces.
     *
     * Prepares an in-memory SQL database environment, binds the [ChartCamDatabase] schema,
     * and performs a raw `getPractitionerById` asynchronous driver invocation.
     */
    @Test
    fun testAsyncDriver() =
        kotlinx.coroutines.runBlocking {
            val driver =
                app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
                    app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY,
                )
            ChartCamDatabase.Schema.awaitCreate(driver)
            val db = ChartCamDatabase(driver)
            val x = db.chartCamQueries.getPractitionerById("1").awaitAsOneOrNull()
            println("X IS " + x)
        }
}
