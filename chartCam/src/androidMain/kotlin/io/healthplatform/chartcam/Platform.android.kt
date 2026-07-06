/**
 * File defining the Android-specific platform identification.
 */
package io.healthplatform.chartcam

import android.os.Build

/**
 * Android implementation of the [Platform] interface.
 * Exposes the Android OS name and the SDK integer version.
 */
class AndroidPlatform : Platform {
    /**
     * The name of the platform, including the Android SDK version (e.g., "Android 33").
     */
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Creates and returns an instance of the Android [Platform].
 *
 * @return An instance of [Platform] (specifically [AndroidPlatform]).
 */
actual fun getPlatform(): Platform = AndroidPlatform()
