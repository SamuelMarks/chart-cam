/**
 * File defining the Android-specific implementation of the [SecureStorage] interface.
 */
package io.healthplatform.chartcam.storage

import android.content.Context
import android.util.Base64
import io.healthplatform.chartcam.AndroidAppInit

/**
 * Android implementation of [SecureStorage].
 * Uses Android Jetpack Security's equivalents to transparently encrypt both keys and values
 * using AES256 for secure storage of sensitive data.
 */
class AndroidSecureStorage : SecureStorage {
    private val sharedPreferences by lazy {
        AndroidAppInit.getContext().getSharedPreferences("secure_prefs_v2", Context.MODE_PRIVATE)
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
        val encrypted = CryptoHelper.encrypt(value.toByteArray(Charsets.UTF_8))
        val base64 = Base64.encodeToString(encrypted, Base64.DEFAULT)
        sharedPreferences.edit().putString(key, base64).apply()
    }

    /**
     * Retrieves a decrypted string value for a specific key.
     *
     * @param key The key of the value to retrieve.
     * @return The decrypted string value, or `null` if the key does not exist.
     */
    override fun getString(key: String): String? {
        val base64 = sharedPreferences.getString(key, null) ?: return null
        return try {
            val encrypted = Base64.decode(base64, Base64.DEFAULT)
            String(CryptoHelper.decrypt(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

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
