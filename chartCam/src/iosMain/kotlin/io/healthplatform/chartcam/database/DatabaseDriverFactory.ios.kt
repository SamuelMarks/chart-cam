/**
 * @file DatabaseDriverFactory.ios.kt
 * Contains declarations for DatabaseDriverFactory.ios.kt.
 *
 * iOS implementation of the database driver factory.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS-specific factory for creating SQLDelight database drivers.
 *
 * This factory initializes and provides a [NativeSqliteDriver] for iOS,
 * allowing the cross-platform SQLDelight database to perform read and write
 * operations against an SQLite database file.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates and returns an iOS-specific [SqlDriver].
     *
     * The driver is configured to use the [NativeSqliteDriver] with the schema
     * from [ChartCamDatabase] and connects to a local database file named "chartcam.db".
     *
     * @return A [SqlDriver] configured for iOS to execute database queries.
     */
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = ChartCamDatabase.Schema.synchronous(),
            name = "chartcam.db",
        )
}
