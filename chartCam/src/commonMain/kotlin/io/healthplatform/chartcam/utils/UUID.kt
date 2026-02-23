package io.healthplatform.chartcam.utils

/**
 * Utility class to generate UUIDs across platforms.
 */
expect object UUID {
    /**
     * Generates a random UUID string.
     * @return A standard UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000").
     */
    fun randomUUID(): String
}