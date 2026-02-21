package io.healthplatform.chartcam

/**
 * Represents the underlying platform environment.
 *
 * This interface allows the common code to access platform-specific details
 * like OS name, version, or device characteristics without depending on
 * platform-specific SDKs directly.
 */
interface Platform {
    /**
     * A human-readable string identifying the current platform.
     * E.g., "Android 34", "iOS 17.2", "Java 21".
     */
    val name: String
}

/**
 * platform-specific factory function to retrieve the current [Platform] implementation.
 *
 * @return The [Platform] instance for the target currently running.
 */
expect fun getPlatform(): Platform