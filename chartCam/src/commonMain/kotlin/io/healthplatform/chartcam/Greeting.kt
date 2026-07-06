package io.healthplatform.chartcam

/**
 * A shared utility class to generate greeting messages.
 *
 * This class demonstrates logic sharing between platforms by utilizing
 * the [getPlatform] expect/actual mechanism.
 */
class Greeting {
    /**
     * The platform details for the current environment.
     */
    private val platform: Platform = getPlatform()

    /**
     * Generates a greeting string including the platform name.
     *
     * @return A formatted string "Hello, {PlatformName}!".
     */
    fun greet(): String = "Hello, ${platform.name}!"
}
