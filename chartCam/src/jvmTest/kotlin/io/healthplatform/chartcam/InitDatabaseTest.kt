package io.healthplatform.chartcam

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class InitDatabaseTest {
    @Test
    fun testInitDatabaseSuccess() =
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            // This should run without throwing any exceptions
            initDatabase(driver)

            // Running it a second time should catch Throwable (actually JdbcSqliteDriver throws an exception if tables exist)
            // and swallow it cleanly.
            initDatabase(driver)
        }
}
