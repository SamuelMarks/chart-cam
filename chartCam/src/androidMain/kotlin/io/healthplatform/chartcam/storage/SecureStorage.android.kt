package io.healthplatform.chartcam.storage

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.healthplatform.chartcam.AndroidAppInit

/**
 * Android implementation of [SecureStorage].
 * Uses Jetpack Security's [EncryptedSharedPreferences] to encrypt keys and values.
 */
class AndroidSecureStorage : SecureStorage {

    private val sharedPreferences by lazy {
        val context = AndroidAppInit.getContext()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun save(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}

/**
 * Creates the Android-specific SecureStorage.
 * Note: Requires [AndroidAppInit.init] to be called prior.
 */
actual fun createSecureStorage(): SecureStorage = AndroidSecureStorage()