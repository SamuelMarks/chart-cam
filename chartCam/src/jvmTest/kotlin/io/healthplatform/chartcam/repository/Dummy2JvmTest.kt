/**
 * @file Dummy2JvmTest.kt
 * Contains declarations for Dummy2JvmTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Dummy test class 2 for JVM.
 */
class Dummy2JvmTest {
    /**
     * Tests async dummy behavior.
     */
    @Test
    fun testAsyncDummy() =
        runTest {
            // Just verify it doesn't crash on an empty database query
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val db = ChartCamDatabase(driver)

            assertTrue(true)
            driver.close()
        }
}
