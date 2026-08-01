/**
 * @file DatabaseDriverFactory.kt
 * Contains declarations for DatabaseDriverFactory.kt.
 *
 * File defining the Android-specific implementation of the [DatabaseDriverFactory].
 */
package io.healthplatform.chartcam.database

import android.content.Context
import android.util.Base64
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.storage.CryptoHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

/**
 * Android implementation of the Database Driver Factory.
 * Incorporates SQLCipher to ensure database encryption at rest (HIPAA/PHI compliance).
 * The passphrase is generated cryptographically securely and stored using Android Keystore.
 */
actual class DatabaseDriverFactory actual constructor() {
    companion object {
        private const val PASSPHRASE_LENGTH = 32
    }

    /**
     * Creates an encrypted AndroidSqliteDriver using the app context and SQLCipher SupportFactory.
     * Requires [AndroidAppInit] to be initialized.
     *
     * @return A configured [SqlDriver] suitable for operating on an encrypted SQLite database.
     */
    actual fun createDriver(): SqlDriver {
        val context = AndroidAppInit.getContext()

        System.loadLibrary("sqlcipher")

        val prefs = context.getSharedPreferences("db_secure_prefs_v2", Context.MODE_PRIVATE)

        // Retrieve or generate a 32-byte secure passphrase for SQLCipher
        var encodedPassphrase = prefs.getString("db_passphrase_v2", null)
        if (encodedPassphrase == null) {
            val bytes = ByteArray(PASSPHRASE_LENGTH)
            SecureRandom().nextBytes(bytes)
            val encryptedBytes = CryptoHelper.encrypt(bytes)
            encodedPassphrase = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            prefs.edit().putString("db_passphrase_v2", encodedPassphrase).apply()
        }

        val encryptedBytes = Base64.decode(encodedPassphrase, Base64.DEFAULT)
        val passphrase = CryptoHelper.decrypt(encryptedBytes)
        val factory = SupportOpenHelperFactory(passphrase)

        return AndroidSqliteDriver(
            schema = ChartCamDatabase.Schema.synchronous(),
            context = context,
            name = "chartcam_encrypted.db",
            factory = factory,
        )
    }
}
