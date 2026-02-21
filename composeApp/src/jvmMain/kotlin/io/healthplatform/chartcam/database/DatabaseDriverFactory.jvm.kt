package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
import java.io.File

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:chartcam.db")
        if (!File("chartcam.db").exists()) {
            ChartCamDatabase.Schema.synchronous().create(driver)
        }
        return driver
    }
}
