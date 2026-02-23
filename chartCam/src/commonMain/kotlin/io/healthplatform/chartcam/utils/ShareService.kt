package io.healthplatform.chartcam.utils

/**
 * Service to share files and text with other applications.
 */
interface ShareService {
    /**
     * Shares a file located at [filePath].
     * @param filePath The absolute path to the file.
     */
    fun shareFile(filePath: String)

    /**
     * Shares the given [text].
     * @param text The text to share.
     */
    fun shareText(text: String)
}

/**
 * Factory for the platform-specific ShareService.
 */
expect fun createShareService(): ShareService
