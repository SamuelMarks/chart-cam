/**
 * File defining the Android-specific implementation of the [SecureStorage] interface.
 */
package io.healthplatform.chartcam.storage

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.healthplatform.chartcam.AndroidAppInit

/**
 * Android implementation of [SecureStorage].
 * Uses Android Jetpack Security's [EncryptedSharedPreferences] to transparently encrypt both keys and values
 * using AES256 for secure storage of sensitive data.
 */
class AndroidSecureStorage : SecureStorage {
    /**
     * Lazy initialization of [EncryptedSharedPreferences].
     * Configures AES256_GCM for values and AES256_SIV for keys.
     */
    private val sharedPreferences by lazy {
        val context = AndroidAppInit.getContext()
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Encrypts and saves a string value mapped to a specific key.
     *
     * @param key The key under which the string value will be saved.
     * @param value The string value to securely store.
     */
    override fun save(
        key: String,
        value: String,
    ) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Retrieves a decrypted string value for a specific key.
     *
     * @param key The key of the value to retrieve.
     * @return The decrypted string value, or `null` if the key does not exist.
     */
    override fun getString(key: String): String? = sharedPreferences.getString(key, null)

    /**
     * Deletes the stored value mapped to the given key.
     *
     * @param key The key of the entry to delete.
     */
    override fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}

/**
 * Creates and returns the Android-specific implementation of [SecureStorage].
 * Note: Requires [AndroidAppInit.init] to have been called beforehand.
 *
 * @return An instance of [SecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = AndroidSecureStorage()
