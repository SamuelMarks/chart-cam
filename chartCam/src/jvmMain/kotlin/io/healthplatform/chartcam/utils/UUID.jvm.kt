/**
 * UUID generation utility for the JVM platform.
 */
package io.healthplatform.chartcam.utils

import java.util.UUID as JavaUUID

/**
 * A platform-specific object to generate UUIDs.
 */
actual object UUID {
    /**
     * Generates a random UUID string using the underlying Java platform UUID generator.
     *
     * @return A randomly generated UUID as a [String].
     */
    actual fun randomUUID(): String = JavaUUID.randomUUID().toString()
}
