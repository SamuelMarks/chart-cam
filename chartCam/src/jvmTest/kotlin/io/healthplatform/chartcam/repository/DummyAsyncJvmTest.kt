/**
 * @file DummyAsyncJvmTest.kt
 * Contains declarations for DummyAsyncJvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Dummy async test class for JVM.
 */
class DummyAsyncJvmTest {
    /**
     * Tests dummy async behavior.
     */
    @Test
    fun testDummyAsyncJvm() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val db = ChartCamDatabase(driver)
        }
}
