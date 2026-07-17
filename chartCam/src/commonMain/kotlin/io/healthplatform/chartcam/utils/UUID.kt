/**
 * @file UUID.kt
 * Contains declarations for UUID.kt.
 *
 * Provides multiplatform utilities for generating Universally Unique Identifiers (UUIDs).
 */
package io.healthplatform.chartcam.utils

/**
 * Utility object to generate UUIDs across different platforms.
 */
expect object UUID {
    /**
     * Generates a random UUID string (version 4).
     *
     * @return A standard UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000").
     */
    fun randomUUID(): String
}
