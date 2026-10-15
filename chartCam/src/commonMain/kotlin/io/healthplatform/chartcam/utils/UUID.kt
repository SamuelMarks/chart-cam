/**
 * @file UUID.kt
 * Contains declarations for UUID.kt.
 *
 * Provides multiplatform utilities for generating Universally Unique Identifiers (UUIDs)
 * backed by the Kotlin standard library [kotlin.uuid.Uuid].
 */
package io.healthplatform.chartcam.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Utility object to generate cryptographically random v4 UUIDs across all Kotlin Multiplatform targets.
 */
object UUID {
    /**
     * Generates a random UUID string (version 4).
     *
     * @return A standard 36-character UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000").
     */
    @OptIn(ExperimentalUuidApi::class)
    fun randomUUID(): String = Uuid.random().toString()
}
