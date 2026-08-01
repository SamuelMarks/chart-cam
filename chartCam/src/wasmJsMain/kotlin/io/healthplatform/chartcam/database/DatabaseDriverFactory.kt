/**
 * @file DatabaseDriverFactory.wasmJs.kt
 * @file DatabaseDriverFactory.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of [DatabaseDriverFactory],
 * responsible for creating an [SqlDriver] for SQLDelight using a web worker.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * A factory for creating database drivers on the WebAssembly (WasmJs) platform.
 * Relies on [WebWorkerDriver] and a web worker script (`sqljs.worker.js`) to handle
 * SQLite operations off the main UI thread.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates and configures an [SqlDriver] for WebAssembly environments.
     * Uses a [WebWorkerDriver] communicating with a `sqljs.worker.js` worker instance.
     *
     * @return A newly created [SqlDriver] ready for SQLDelight operations.
     */
    actual fun createDriver(): SqlDriver =
        WebWorkerDriver(
            Worker("sqljs.worker.js"),
        )
}
