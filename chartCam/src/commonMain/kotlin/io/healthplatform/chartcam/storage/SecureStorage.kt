package io.healthplatform.chartcam.storage

/**
 * Interface for securely persisting sensitive string data (e.g., Auth Tokens).
 * Implementations should use platform-specific encryption mechanisms.
 */
interface SecureStorage {
    /**
     * Saves a value securely.
     *
     * @param key The unique key to identify the data.
     * @param value The sensitive string to store.
     */
    fun save(key: String, value: String)

    /**
     * Retrieves a securely stored value.
     *
     * @param key The unique key to look up.
     * @return The string value if found, null otherwise.
     */
    fun getString(key: String): String?

    /**
     * Deletes a stored value.
     *
     * @param key The unique key to remove.
     */
    fun delete(key: String)
}

/**
 * Factory function to create a platform-specific instance of [SecureStorage].
 *
 * @return A concrete implementation of SecureStorage.
 */
expect fun createSecureStorage(): SecureStorage