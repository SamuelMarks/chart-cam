/**
 * @file SecureStorage.jvm.kt
 * Secure storage implementation for the JVM platform.
 */
package io.healthplatform.chartcam.storage

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM-specific implementation of [SecureStorage].
 *
 * Uses standard Java [Preferences] backed by AES-GCM encryption
 * to securely store and retrieve preferences on Desktop targets.
 *
 * @property nodeName The node name used for categorizing the [Preferences]. Defaults to "io.healthplatform.chartcam.secure".
 */
class JvmSecureStorage(
    nodeName: String = "io.healthplatform.chartcam.secure",
) : SecureStorage {
    /**
     * The underlying Java preferences instance where encrypted data is stored.
     */
    private val prefs: Preferences = Preferences.userRoot().node(nodeName)

    /**
     * The symmetric secret key derived from a static string, used for AES encryption.
     * Note: In a production HIPAA environment, this key should be properly derived from a user password or OS keystore.
     */
    private val secretKey: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("ChartCamSecretKey123".toByteArray(Charsets.UTF_8))
        SecretKeySpec(bytes, "AES")
    }

    /**
     * Encrypts the given [value] using AES-GCM and returns a Base64-encoded string.
     *
     * @param value The plain-text string to encrypt.
     * @return A Base64-encoded string combining the initialization vector (IV) and the encrypted payload.
     */
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts the Base64-encoded, AES-GCM encrypted [value] back into a plain string.
     *
     * @param value The encrypted Base64 string to decrypt.
     * @return The original plain-text string.
     */
    private fun decrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val decoded = Base64.getDecoder().decode(value)

        val iv = decoded.copyOfRange(0, 12)
        val encrypted = decoded.copyOfRange(12, decoded.size)

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Encrypts and securely saves a string [value] under the specified [key].
     *
     * @param key The identifier under which to store the value.
     * @param value The raw string value to encrypt and store.
     */
    override fun save(
        key: String,
        value: String,
    ) {
        prefs.put(key, encrypt(value))
    }

    /**
     * Retrieves and decrypts a string value associated with the specified [key].
     *
     * @param key The identifier of the stored value.
     * @return The decrypted string value, or null if it does not exist or decryption fails.
     */
    override fun getString(key: String): String? {
        val encrypted = prefs.get(key, null) ?: return null
        return try {
            decrypt(encrypted)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes the stored value associated with the specified [key].
     *
     * @param key The identifier of the value to delete.
     */
    override fun delete(key: String) {
        prefs.remove(key)
    }

    /**
     * Clears all encrypted preferences stored in this node.
     */
    internal fun clearAll() {
        prefs.clear()
    }
}

/**
 * Creates the JVM [SecureStorage] implementation.
 *
 * @return A new instance of [JvmSecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = JvmSecureStorage()
