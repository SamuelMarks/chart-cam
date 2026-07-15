/**
 * @file Platform.js.kt
 * Platform identification for the JS platform.
 */
package io.healthplatform.chartcam

/**
 * Represents the JS (Web) platform environment.
 */
class JsPlatform : Platform {
    /**
     * A human-readable name identifying the platform.
     */
    override val name: String = "Web with Kotlin/JS"
}

/**
 * Retrieves the current platform information.
 *
 * @return An instance of [Platform] representing the JS web environment.
 */
actual fun getPlatform(): Platform = JsPlatform()
