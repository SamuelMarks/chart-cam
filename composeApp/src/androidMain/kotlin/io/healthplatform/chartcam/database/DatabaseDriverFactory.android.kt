package io.healthplatform.chartcam.database

import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.healthplatform.chartcam.AndroidAppInit
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

/**
 * Android implementation of the Database Driver Factory.
 * Incorporates SQLCipher to ensure database encryption at rest (HIPAA/PHI compliance).
 * The passphrase is generated cryptographically securely and stored using Android Keystore
 * via [EncryptedSharedPreferences].
 */
actual class DatabaseDriverFactory actual constructor() {
    /**
     * Creates an encrypted AndroidSqliteDriver using the app context and SQLCipher SupportFactory.
     * Requires [AndroidAppInit] to be initialized.
     */
    actual fun createDriver(): SqlDriver {
        val context = AndroidAppInit.getContext()
        
        // Use MasterKey to encrypt SharedPreferences containing our database passphrase
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        val prefs = EncryptedSharedPreferences.create(
            context,
            "db_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // Retrieve or generate a 32-byte secure passphrase for SQLCipher
        var encodedPassphrase = prefs.getString("db_passphrase", null)
        if (encodedPassphrase == null) {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            encodedPassphrase = Base64.encodeToString(bytes, Base64.DEFAULT)
            prefs.edit().putString("db_passphrase", encodedPassphrase).apply()
        }
        
        val passphrase = Base64.decode(encodedPassphrase, Base64.DEFAULT)
        val factory = SupportFactory(passphrase)
        
        return AndroidSqliteDriver(
            schema = ChartCamDatabase.Schema.synchronous(),
            context = context,
            name = "chartcam_encrypted.db",
            factory = factory
        )
    }
}
