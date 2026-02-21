package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.healthplatform.chartcam.AndroidAppInit
import app.cash.sqldelight.async.coroutines.synchronous

/**
 * Android implementation of the Database Driver Factory.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates an AndroidSqliteDriver using the app context.
     * Requires [AndroidAppInit] to be initialized.
     */
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            ChartCamDatabase.Schema.synchronous(),
            AndroidAppInit.getContext(),
            "chartcam.db"
        )
    }
}