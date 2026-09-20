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
     * Shares a file located at [filePath] using platform-native share sheets or local file openers.
     *
     * @param filePath The absolute or relative path to the file to be shared.
     * @return A [Result] indicating success or failure containing a domain [ExportException].
     */
    fun shareFile(filePath: String): Result<Unit>

    /**
     * Shares the given [text] to external applications or the system clipboard.
     *
     * @param text The text content to be shared.
     * @return A [Result] indicating success or failure containing a domain [ExportException].
     */
    fun shareText(text: String): Result<Unit>

    /**
     * Validates that the provided file path is not empty.
     *
     * @param filePath The path to validate.
     * @return True if valid non-empty path, false otherwise.
     */
    fun isValidSharePath(filePath: String): Boolean = filePath.isNotBlank()
}

/**
 * Factory function to create a platform-specific instance of [ShareService].
 *
 * @return A concrete implementation of [ShareService] for the current platform.
 */
expect fun createShareService(): ShareService
