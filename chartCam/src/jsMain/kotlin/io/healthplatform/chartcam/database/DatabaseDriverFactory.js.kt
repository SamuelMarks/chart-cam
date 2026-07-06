/**
 * Database infrastructure for JS web applications.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * Factory class for creating database drivers on the JS platform.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates a new [SqlDriver] using a web worker.
     *
     * @return The configured [SqlDriver] for JS using SQL.js in a web worker.
     */
    actual fun createDriver(): SqlDriver =
        WebWorkerDriver(
            Worker("sqljs.worker.js"),
        )
}
