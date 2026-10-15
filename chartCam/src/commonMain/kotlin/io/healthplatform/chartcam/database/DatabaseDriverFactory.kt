/**
 * @file DatabaseDriverFactory.kt
 * Contains declarations for DatabaseDriverFactory.kt.
 *
 * Defines the contract for providing a platform-specific SQL database driver.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Interface contract for creating a platform-specific SQLDelight [SqlDriver] instance.
 * Enables clean dependency injection and fake driver substitution in multiplatform tests.
 */
fun interface SqlDriverFactory {
    /**
     * Creates a SQLDelight driver instance configured for the specific platform.
     *
     * @return A configured [SqlDriver] to interact with the local SQL database.
     */
    fun createDriver(): SqlDriver

    /**
     * Safely creates a SQLDelight driver instance wrapped in a [Result].
     *
     * @return A [Result] enclosing the configured [SqlDriver] or a failure.
     */
    fun createDriverCatching(): Result<SqlDriver> = runCatching { createDriver() }
}

/**
 * Expect class to handle platform-specific SQL Driver creation.
 * Implementations in platform-specific source sets provide the actual mechanism
 * for initializing the database (e.g., AndroidSqliteDriver on Android).
 */
expect class DatabaseDriverFactory() : SqlDriverFactory {
    /**
     * Creates a SQLDelight driver instance configured for the specific platform.
     *
     * @return A configured [SqlDriver] to interact with the local SQL database.
     */
    override fun createDriver(): SqlDriver
}

/**
 * Factory function to create a platform-specific [SqlDriverFactory] instance.
 *
 * @return A [SqlDriverFactory] for the current platform.
 */
fun createDatabaseDriverFactory(): SqlDriverFactory = DatabaseDriverFactory()
