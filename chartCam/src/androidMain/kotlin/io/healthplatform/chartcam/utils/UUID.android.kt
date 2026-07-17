/**
 * @file UUID.android.kt
 * Contains declarations for UUID.android.kt.
 *
 * File defining the Android-specific implementation for generating UUIDs.
 */
package io.healthplatform.chartcam.utils

import java.util.UUID as JavaUUID

/**
 * Android implementation of the UUID utility using the standard Java `java.util.UUID`.
 */
actual object UUID {
    /**
     * Generates a random universally unique identifier (UUID).
     *
     * @return A randomly generated UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000").
     */
    actual fun randomUUID(): String = JavaUUID.randomUUID().toString()
}
