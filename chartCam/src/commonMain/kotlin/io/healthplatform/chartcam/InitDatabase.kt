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
import io.healthplatform.chartcam.utils.runSuspendCatching

/**
 * Initializes the database schema using the provided driver.
 * Attempts to create the tables, safely encapsulating the operation in a [Result].
 *
 * @param driver The platform-specific [SqlDriver] for database operations.
 * @return A [Result] containing the initialized [ChartCamDatabase] or the encapsulated exception.
 */
suspend fun initDatabase(driver: SqlDriver): Result<ChartCamDatabase> =
    runSuspendCatching {
        ChartCamDatabase.Schema.awaitCreate(driver)
        ChartCamDatabase(driver)
    }.recoverCatching {
        ChartCamDatabase(driver)
    }
