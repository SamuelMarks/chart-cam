package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitCreate
import kotlin.test.Test

class DummyAsyncTest {
    @Test
    fun testAsyncDriver() = kotlinx.coroutines.runBlocking {
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.awaitCreate(driver)
        val db = ChartCamDatabase(driver)
        val x = db.chartCamQueries.getPractitionerById("1").awaitAsOneOrNull()
        println("X IS " + x)
    }
}
