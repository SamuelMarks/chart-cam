/**
 * @file UUID.ios.kt
 * Contains declarations for UUID.ios.kt.
 *
 * iOS implementation of UUID generation.
 * This file provides the iOS-specific actual implementation for UUIDs.
 */
package io.healthplatform.chartcam.utils

import platform.Foundation.NSUUID

/**
 * iOS implementation for generating unique identifiers.
 */
actual object UUID {
    /**
     * Generates a random UUID string using the native [NSUUID].
     *
     * @return A randomly generated UUID string.
     */
    actual fun randomUUID(): String = NSUUID().UUIDString()
}
