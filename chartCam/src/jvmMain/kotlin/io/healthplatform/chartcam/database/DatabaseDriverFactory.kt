/**
 * @file DatabaseDriverFactory.jvm.kt
 * Database driver factory implementation for the JVM platform.
 */
package io.healthplatform.chartcam.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * A factory for creating [SqlDriver] instances on the JVM using SQLite JDBC.
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates and initializes a new [SqlDriver] connection to the local SQLite database.
     *
     * If the database file (`chartcam_desktop.db`) does not exist, it will create the schema automatically.
     *
     * @return An initialized [SqlDriver] instance ready for database operations.
     */
    actual fun createDriver(): SqlDriver {
        // NOTE: Standard sqlite-jdbc driver does not support transparent database encryption.
        // For a true HIPAA-compliant desktop JVM build, you must replace the standard
        // sqlite-jdbc dependency with a commercial SQLCipher JDBC driver (e.g., Zetetic)
        // or a compiled sqleet-based wrapper.
        //
        // Example with a hypothetical encrypted JDBC driver:
        // val props = Properties().apply { setProperty("password", "secure_password") }
        // val driver = JdbcSqliteDriver("jdbc:sqlcipher:chartcam_encrypted.db", props)

        /**
         * The JDBC URL connection string for the SQLite database.
         */
        val url = "jdbc:sqlite:chartcam_desktop.db"

        /**
         * The instantiated SQLite JDBC driver.
         */
        val driver = JdbcSqliteDriver(url)

        if (!File("chartcam_desktop.db").exists()) {
            ChartCamDatabase.Schema.synchronous().create(driver)
        }

        return driver
    }
}
