/**
 * @file ShareService.kt
 * Contains declarations for ShareService.kt.
 *
 * Provides an interface and factory for sharing content to other applications.
 */
package io.healthplatform.chartcam.utils

/**
 * Service to share files and text with other applications using platform-specific mechanisms.
 */
interface ShareService {
    /**
     * Shares a file located at [filePath].
     *
     * @param filePath The absolute path to the file to be shared.
     */
    fun shareFile(filePath: String)

    /**
     * Shares the given [text].
     *
     * @param text The text content to be shared.
     */
    fun shareText(text: String)
}

/**
 * Factory function to create a platform-specific instance of [ShareService].
 *
 * @return A concrete implementation of [ShareService] for the current platform.
 */
expect fun createShareService(): ShareService
