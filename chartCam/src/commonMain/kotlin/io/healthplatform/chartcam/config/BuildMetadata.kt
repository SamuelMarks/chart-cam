/**
 * @file BuildMetadata.kt
 * Centralized application build and configuration metadata.
 */
package io.healthplatform.chartcam.config

/**
 * Holds centralized build properties and application metadata constants.
 */
object BuildMetadata {
    /** The application version name. */
    const val VERSION_NAME: String = "1.0.5"

    /** The official application website URL. */
    const val WEBSITE_URL: String = "https://healthplatform.io"

    /**
     * Formats the version string and website URL for display.
     *
     * @return The formatted version string combining version name and website URL.
     */
    fun formattedVersionInfo(): String = "$VERSION_NAME — $WEBSITE_URL"
}
