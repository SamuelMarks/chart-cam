/**
 * @file SecureStorage.kt
 * Contains declarations for SecureStorage.kt.
 *
 * Defines the contract for secure storage of sensitive data.
 */
package io.healthplatform.chartcam.storage

/**
 * Interface for securely persisting sensitive string data (e.g., Auth Tokens).
 * Implementations should use platform-specific encryption mechanisms to protect the data.
 */
interface SecureStorage {
    /**
     * Saves a value securely.
     *
     * @param key The unique key to identify the data.
     * @param value The sensitive string to store securely.
     */
    fun save(
        key: String,
        value: String,
    )

    /**
     * Retrieves a securely stored value.
     *
     * @param key The unique key to look up.
     * @return The string value if found, or null if it does not exist.
     */
    fun getString(key: String): String?

    /**
     * Deletes a securely stored value.
     *
     * @param key The unique key identifying the value to remove.
     */
    fun delete(key: String)
}

/**
 * Factory function to create a platform-specific instance of [SecureStorage].
 *
 * @return A concrete implementation of [SecureStorage] suitable for the current platform.
 */
expect fun createSecureStorage(): SecureStorage
