/**
 * Platform identification utility for the JVM target.
 */
package io.healthplatform.chartcam

/**
 * A class representing the JVM platform.
 */
class JVMPlatform : Platform {
    /**
     * The name of the platform, including the currently running Java version.
     */
    override val name: String = "Java ${System.getProperty("java.version")}"
}

/**
 * Retrieves the current [Platform] instance for the JVM.
 *
 * @return A new instance of [JVMPlatform].
 */
actual fun getPlatform(): Platform = JVMPlatform()
