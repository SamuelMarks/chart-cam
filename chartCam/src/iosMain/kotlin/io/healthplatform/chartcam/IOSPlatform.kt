/**
 * @file Platform.ios.kt
 * Contains declarations for Platform.ios.kt.
 *
 * iOS implementation of the Platform interface.
 * Provides details about the iOS platform using native UIKit APIs.
 */
package io.healthplatform.chartcam

import platform.UIKit.UIDevice

/**
 * iOS-specific implementation of the [Platform] interface.
 */
class IOSPlatform : Platform {
    /**
     * The name of the platform, incorporating the device system name and version.
     */
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Returns the iOS-specific implementation of the [Platform] interface.
 *
 * @return An instance of [IOSPlatform].
 */
actual fun getPlatform(): Platform = IOSPlatform()
