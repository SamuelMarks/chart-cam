/**
 * @file InitDatabase.kt
 * Contains declarations for InitDatabase.kt.
 *
 * Provides database initialization logic across platforms.
 */
package io.healthplatform.chartcam

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import io.healthplatform.chartcam.database.ChartCamDatabase

/**
 * Initializes the database schema using the provided driver.
 * Will attempt to create the tables, but catches errors silently if the schema already exists
 * or if the underlying driver throws a general Throwable during creation.
 *
 * @param driver The platform-specific [SqlDriver] for database operations.
 */
suspend fun initDatabase(driver: SqlDriver) {
    try {
        // We handle exceptions if schema already exists, but some driver implementations
        // throw Throwable instead of Exception, causing it to crash the app.
        ChartCamDatabase.Schema.awaitCreate(driver)
    } catch (e: Throwable) {
        // usually fails if already created or synchronous driver handles it
    }
}
