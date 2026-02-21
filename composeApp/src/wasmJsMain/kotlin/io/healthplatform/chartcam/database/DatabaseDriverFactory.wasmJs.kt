package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            Worker("sqljs.worker.js")
        )
    }
}
