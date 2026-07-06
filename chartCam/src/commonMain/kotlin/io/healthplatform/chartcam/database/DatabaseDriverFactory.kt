/**
 * Defines the contract for providing a platform-specific SQL database driver.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Expect class to handle platform-specific SQL Driver creation.
 * Implementations in platform-specific source sets will provide the actual mechanism
 * for initializing the database (e.g., AndroidSqliteDriver on Android).
 */
expect class DatabaseDriverFactory() {
    /**
     * Creates a SQLDelight driver instance configured for the specific platform.
     *
     * @return A configured [SqlDriver] to interact with the local SQL database.
     */
    fun createDriver(): SqlDriver
}
