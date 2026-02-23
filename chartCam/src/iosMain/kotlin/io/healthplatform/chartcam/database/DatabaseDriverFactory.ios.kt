package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous

/**
 * iOS implementation of the Database Driver Factory.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates a NativeSqliteDriver.
     */
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = ChartCamDatabase.Schema.synchronous(),
            name = "chartcam.db"
        )
    }
}