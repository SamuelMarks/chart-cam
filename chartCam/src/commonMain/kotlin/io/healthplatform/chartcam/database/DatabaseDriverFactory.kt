package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Expect class to handle platform-specific SQL Driver creation.
 */
expect class DatabaseDriverFactory() {
    /**
     * Creates a SQLDelight driver instance.
     *
     * @return A configured [SqlDriver] for the specific platform.
     */
    fun createDriver(): SqlDriver
}