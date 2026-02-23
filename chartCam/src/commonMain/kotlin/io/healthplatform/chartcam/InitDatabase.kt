package io.healthplatform.chartcam

import app.cash.sqldelight.db.SqlDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate

suspend fun initDatabase(driver: SqlDriver) {
    try {
        // We handle exceptions if schema already exists, but some driver implementations
        // throw Throwable instead of Exception, causing it to crash the app.
        ChartCamDatabase.Schema.awaitCreate(driver)
    } catch(e: Throwable) {
        // usually fails if already created or synchronous driver handles it
    }
}
